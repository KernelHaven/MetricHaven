package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.code_metrics.NestingDepth;
import net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.FeatureSizeWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.WeigthsCache;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Hard coded list of metrics to run.
 * @author Sascha El-Sharkawy
 */
public class IndividualCodeMetricsRunner extends AnalysisComponent<MultiMetricResult> {
    
    public static final @NonNull Setting<@NonNull Integer> MAX_THREADS = new Setting<>("metrics.max_parallel_threads",
        Type.INTEGER, true, "1", "Defines the number of threads to use for calculating metrics. Must be >= 1.");
    
    public static final @NonNull Setting<@NonNull Boolean> ROUND_RESULTS = new Setting<>("metrics.round_results",
        Type.BOOLEAN, true, "false", "If turned on, results will be limited to 2 digits after the comma (0.005 will be "
            + "rounded up). This is maybe neccessary to limit the disk usage.");
    
    public static final @NonNull Setting<@Nullable List<@NonNull String>> METRICS_SETTING = new Setting<>(
            "metrics.code_metrics", Type.STRING_LIST, false, null,
            "Defines a list of fully qualified class names of metrics that the "
            + IndividualCodeMetricsRunner.class.getName() + " component should execute.");
    
    
    private boolean round = false;
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    private @Nullable AnalysisComponent<FunctionMap> fmComponent;
    private @Nullable AnalysisComponent<FeatureSizeContainer> fsComponent;
    
    private int nThreads;
    
    private MultiMetricResult firstResult;
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent) throws SetUpException {
        
        this(config, codeFunctionComponent, null, null, null, null);
        
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        this(config, codeFunctionComponent, varModelComponent, null, null, null);
        
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent,
        @NonNull AnalysisComponent<BuildModel> bmComponent) throws SetUpException {
        
        this(config, codeFunctionComponent, varModelComponent, bmComponent, null, null);
        
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * @param sdComponent Scattering degree values, used to create the {@link ScatteringWeight}.
     * @param fmComponent {@link FunctionMap}, required to run {@link FanInOut}-metrics.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    //CHECKSTYLE:OFF // More than 5 parameters
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
    //CHECKSTYLE:ON
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent,
        @Nullable AnalysisComponent<FunctionMap> fmComponent) throws SetUpException {
        
        this(config, codeFunctionComponent, varModelComponent, bmComponent, sdComponent, fmComponent, null);
    }
    
    /**
     * Creates this processing unit (default constructor for {@link IndividualCodeMetricsRunner} to run all variations).
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * @param sdComponent Scattering degree values, used to create the {@link ScatteringWeight}.
     * @param fmComponent {@link FunctionMap}, required to run {@link FanInOut}-metrics.
     * @param fsComponent {@link FeatureSizeContainer}, required to create the {@link FeatureSizeWeight}.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    //CHECKSTYLE:OFF // More than 5 parameters
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
    //CHECKSTYLE:ON
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent,
        @Nullable AnalysisComponent<FunctionMap> fmComponent,
        @Nullable AnalysisComponent<FeatureSizeContainer> fsComponent) throws SetUpException {
        
        super(config);
        
        this.codeFunctionComponent = codeFunctionComponent;
        this.varModelComponent = varModelComponent;
        this.bmComponent = bmComponent;
        this.sdComponent = sdComponent;
        this.fmComponent = fmComponent;
        this.fsComponent = fsComponent;
        
        config.registerSetting(MAX_THREADS);
        nThreads = config.getValue(MAX_THREADS);
        if (nThreads <= 0) {
            throw new SetUpException("Need at least one thread specified in " + MAX_THREADS.getKey()
                + " (got " + nThreads + ")");
        }
        
        try {
            config.registerSetting(ROUND_RESULTS);
            round = config.getValue(ROUND_RESULTS);
        } catch (SetUpException exc) {
            throw new SetUpException("Could not load configuration setting " + ROUND_RESULTS.getKey());
        }
    }
    
    /**
     * Creates a vector space basis, where always exactly one weight is set to 1 and all others to 0.  
     * @return The vector basis for creating elementary weights.
     */
    private @NonNull List<@NonNull Map<String, Integer>> createHierarchyWeightVectorSpace() {
        @NonNull List<@NonNull Map<String, Integer>> result = new ArrayList<>();
        
        @NonNull Map<String, Integer> topHierarchyWeights = new HashMap<>();
        topHierarchyWeights.put("top", 1);
        topHierarchyWeights.put("intermediate", 0);
        topHierarchyWeights.put("leaf", 0);
        result.add(topHierarchyWeights);
        
        @NonNull Map<String, Integer> intermediateHierarchyWeights = new HashMap<>();
        intermediateHierarchyWeights.put("top", 0);
        intermediateHierarchyWeights.put("intermediate", 1);
        intermediateHierarchyWeights.put("leaf", 0);
        result.add(intermediateHierarchyWeights);
        
        @NonNull Map<String, Integer> leafHierarchyWeights = new HashMap<>();
        leafHierarchyWeights.put("top", 0);
        leafHierarchyWeights.put("intermediate", 0);
        leafHierarchyWeights.put("leaf", 1);
        result.add(leafHierarchyWeights);
        
        return result;
    }
    
    
    /**
     * Coded list of metrics to execute.
     * @param params The parameters for creating metrics.
     * 
     * @return The selected metric instances.
     * @throws SetUpException If creation of specified metrics is not possible (wrong set-up)
     */
    private List<@NonNull AbstractFunctionMetric<?>> createMetrics(MetricCreationParameters params)
        throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> metrics = new ArrayList<>();
        params.setSingleMetricExecution(true);
        params.setHierarchyType(HierarchyType.HIERARCHY_WEIGHTS_BY_FILE);
        
        @NonNull List<@NonNull Map<String, Integer>> hierarchyWeightVectorSpace = createHierarchyWeightVectorSpace();
        
        // Variables per function metrics
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // External Variables
            params.setMetricSpecificSettingValue(VariablesPerFunction.VarType.EXTERNAL);
            List<@NonNull AbstractFunctionMetric<?>> newMetrics
                = MetricFactory.createAllVariations(VariablesPerFunction.class, params);
            
          LOGGER.logInfo2("Created ", newMetrics.size(), " new Metrics: ", newMetrics.get(0).getResultName());
            
            metrics.addAll(newMetrics);
            
        }
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // All Variables
            params.setMetricSpecificSettingValue(VariablesPerFunction.VarType.ALL);
            metrics.addAll(MetricFactory.createAllVariations(VariablesPerFunction.class, params));
        }
        
        // Nesting Depth
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // VP NDmax
            params.setMetricSpecificSettingValue(NestingDepth.NDType.VP_ND_MAX);
            metrics.addAll(MetricFactory.createAllVariations(NestingDepth.class, params));
        }
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // Code + VP NDmax
            params.setMetricSpecificSettingValue(NestingDepth.NDType.COMBINED_ND_MAX);
            metrics.addAll(MetricFactory.createAllVariations(NestingDepth.class, params));
        }
        
        // DC Fan-In/Out
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // DC Fan-In (locally)
            params.setMetricSpecificSettingValue(FanInOut.FanType.DEGREE_CENTRALITY_IN_LOCALLY);
            metrics.addAll(MetricFactory.createAllVariations(FanInOut.class, params));
        }
        
    StringBuffer tmpMsg = new StringBuffer();
    tmpMsg.append(metrics.size());
    tmpMsg.append(" new Metrics created:");
    for (AbstractFunctionMetric<?> metric : metrics) {
        tmpMsg.append("\n - ");
        tmpMsg.append(metric.getResultName());
    }
    LOGGER.logInfo2(tmpMsg);
        
        return metrics;
    }
   
    
    @Override
    protected void execute() {
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        BuildModel bm = (null != bmComponent) ? bmComponent.getNextResult() : null;
        ScatteringDegreeContainer sdContainer = (null != sdComponent) ? sdComponent.getNextResult() : null;
        FeatureSizeContainer fsContainer = (null != fsComponent) ? fsComponent.getNextResult() : null;
        FunctionMap functionMap = (null != fmComponent) ? fmComponent.getNextResult() : null;
        
        List<@NonNull AbstractFunctionMetric<?>> allMetrics = null;
        try {
            MetricCreationParameters params = new MetricCreationParameters(varModel, bm, sdContainer, fsContainer);
            params.setFunctionMap(functionMap);
            allMetrics = createMetrics(params);
        } catch (SetUpException e) {
            LOGGER.logException("Could not create metric instances", e);
            return;
        }
        
        String[] metrics = new String[allMetrics.size()];
        int metricsIndex = 0;
        for (AbstractFunctionMetric<?> metric : allMetrics) {
            metrics[metricsIndex++] = metric.getResultName();
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        CodeFunction function;
        while ((function = codeFunctionComponent.getNextResult()) != null) {
            if (nThreads == 1) {
                runForSingleFunctionSingleThread(allMetrics, metrics, function);
            } else {
                runForSingleFunction(allMetrics, metrics, function);
            }
            
            progress.processedOne();
        }
        
        progress.close();
    }
    
    /**
     * Executes all metric variations for a single function.
     * @param allMetrics All metric instances to run.
     * @param metricNames The name of the metrics in the same order.
     * @param function The function to measure.
     */
    private void runForSingleFunctionSingleThread(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
        @NonNull String @NonNull [] metricNames, @NonNull CodeFunction function) {
        
        @Nullable Double @NonNull [] values = new @Nullable Double[allMetrics.size()];
        
        int i = 0;
        for (AbstractFunctionMetric<?> metric : allMetrics) {
            Number result = metric.compute(function);
            
            if (result instanceof Double && round) {
                values[i++] = Math.floor(result.doubleValue() * 100) / 100;
            } else {
                values[i++] = (null != result) ? result.doubleValue() : null;
            }
        }
        
        MeasuredItem funcDescription = new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
                function.getFunction().getLineStart(), function.getName());
        if (null == firstResult) {
            // Initializes header
            firstResult = new MultiMetricResult(funcDescription, metricNames, values);
            addResult(firstResult);
        } else {
            // Less memory/time consuming
            MultiMetricResult result = new MultiMetricResult(funcDescription, notNull(firstResult), values);
            addResult(result);
        }
    }

    /**
     * Executes all metric variations for a single function.
     * @param allMetrics All metric instances to run.
     * @param metricNames The name of the metrics in the same order.
     * @param function The function to measure.
     */
    @SuppressWarnings("null")
    private void runForSingleFunction(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
        @NonNull String @NonNull [] metricNames, @NonNull CodeFunction function) {
        
        final @Nullable Double @NonNull [] values = new @Nullable Double[allMetrics.size()];
        
        Thread[] threads = new Thread[nThreads];
        int partitionSize = (int) Math.ceil((double) allMetrics.size() / nThreads);
        for (int i = 0; i < nThreads; i++) {
            // Start of interval (inclusive)
            final int partionStart = i * partitionSize;
            // End of interval (exclusive)
            final int partitionEnd = Math.min((i + 1) * partitionSize, allMetrics.size());
            
            threads[i] = new Thread(() -> {
                
                for (int j = partionStart; j < partitionEnd; j++) {
                    Number result = allMetrics.get(j).compute(function);
                    if (result instanceof Double && round) {
                        values[j] = Math.floor(result.doubleValue() * 100) / 100;
                    } else {
                        values[j] = (null != result) ? result.doubleValue() : null;
                    }
                }
                
            });
            
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.logException("Could not join metric threads for joining the result", e);
            }
        }
        
        MeasuredItem funcDescription = new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
            function.getFunction().getLineStart(), function.getName());
        if (null == firstResult) {
            // Initializes header
            firstResult = new MultiMetricResult(funcDescription, metricNames, values);
            addResult(firstResult);
        } else {
            // Less memory/time consuming
            MultiMetricResult result = new MultiMetricResult(funcDescription, firstResult, values);
            addResult(result);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "SpecifiedCodeFunctions";
    }

}
