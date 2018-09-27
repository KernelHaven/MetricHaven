package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.List;

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
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A processing unit for executing metrics.
 *  
 * @author Adam
 * @author Sascha El-Sharkawy
 */
public class CodeMetricsRunner extends AnalysisComponent<MultiMetricResult> {
    
    public static final @NonNull Setting<@NonNull Integer> MAX_THREADS = new Setting<>("metrics.max_parallel_threads",
        Type.INTEGER, true, "1", "Defines the number of threads to use for calculating metrics. Must be >= 1.");
    
    public static final @NonNull Setting<@NonNull Boolean> ROUND_RESULTS = new Setting<>("metrics.round_results",
        Type.BOOLEAN, true, "false", "If turned on, results will be limited to 2 digits after the comma (0.005 will be "
            + "rounded up). This is maybe neccessary to limit the disk usage.");
    
    public static final @NonNull Setting<@Nullable List<@NonNull String>> METRICS_SETTING = new Setting<>(
            "metrics.code_metrics", Type.STRING_LIST, false, null,
            "Defines a list of fully qualified class names of metrics that the "
            + CodeMetricsRunner.class.getName() + " component should execute.");
    
    private boolean round = false;
    
    private @Nullable Class<? extends AbstractFunctionMetric<?>> metricClass;
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    private @Nullable AnalysisComponent<FunctionMap> fmComponent;
    
    private @Nullable SDType sdValue;
    private @Nullable CTCRType ctcrValue;
//    private @Nullable FeatureDistanceType distanceValue;
//    private @Nullable VariabilityTypeMeasureType varTypeValue;
//    private @Nullable HierarchyType hierarhcyValue;
//    private @Nullable StructuralType structureValue;
    private boolean singleVariationSpecified;
    private Object metricSpecificValue;
//    
    private int nThreads;
    
    private MultiMetricResult firstResult;
    
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
    @SuppressWarnings("unchecked")
    public CodeMetricsRunner(@NonNull Configuration config,
    //CHECKSTYLE:ON
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent,
        @NonNull AnalysisComponent<BuildModel> bmComponent,
        @NonNull AnalysisComponent<ScatteringDegreeContainer> sdComponent,
        @NonNull AnalysisComponent<FunctionMap> fmComponent) throws SetUpException {
        
        super(config);
        
        this.codeFunctionComponent = codeFunctionComponent;
        this.varModelComponent = varModelComponent;
        this.bmComponent = bmComponent;
        this.sdComponent = sdComponent;
        this.fmComponent = fmComponent;
        
        config.registerSetting(METRICS_SETTING);
        List<@NonNull String> metricClassNames = config.getValue(METRICS_SETTING);
        if (metricClassNames != null) {
            if (metricClassNames.size() > 1) {
                throw new SetUpException("Specifying more than one metric in " + METRICS_SETTING.getKey() + " is "
                        + "currently unsupported; either specify one or none (i.e. run all metrics)");
            }
            if (metricClassNames.isEmpty()) {
                throw new SetUpException(METRICS_SETTING.getKey() + " contains no metrics");
            }
            
            try {
                this.metricClass = (Class<? extends AbstractFunctionMetric<?>>)
                        ClassLoader.getSystemClassLoader().loadClass(metricClassNames.get(0));
            } catch (ReflectiveOperationException e) {
                throw new SetUpException("Can't load metric class", e);
            }
            
            loadVariabilityWeightSettings(config);
        }
        
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
    
    private void loadVariabilityWeightSettings(@NonNull Configuration config) throws SetUpException {
        config.registerSetting(MetricSettings.ALL_METRIC_VARIATIONS);
        singleVariationSpecified = !config.getValue(MetricSettings.ALL_METRIC_VARIATIONS);
        
        if (singleVariationSpecified) {
            // Scattering Degree
            config.registerSetting(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING);
            sdValue = config.getValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING);
            
            // Cross-Tree Constraint Ratio
            config.registerSetting(MetricSettings.CTCR_USAGE_SETTING);
            ctcrValue = config.getValue(MetricSettings.CTCR_USAGE_SETTING);
            
            // This method is called inside the constructor, after the class was specified
            metricSpecificValue = MetricFactory.configureAndReadMetricSpecificSetting(config, notNull(metricClass));
        }
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        BuildModel bm = (null != bmComponent) ? bmComponent.getNextResult() : null;
        ScatteringDegreeContainer sdContainer = (null != sdComponent) ? sdComponent.getNextResult() : null;
        FunctionMap functionMap = (null != fmComponent) ? fmComponent.getNextResult() : null;
        
        List<@NonNull AbstractFunctionMetric<?>> allMetrics;
        try {
            if (varModel == null) {
                throw new SetUpException("VariabilityModel is null");
            }
            if (bm == null) {
                throw new SetUpException("BuildModel is null");
            }
            if (sdContainer == null) {
                throw new SetUpException("ScatteringDegreeContainer is null");
            }
            if (functionMap == null) {
                throw new SetUpException("FunctionMap is null");
            }
            
            MetricCreationParameters params = new MetricCreationParameters(varModel, bm, sdContainer);
            params.setFunctionMap(functionMap);
            params.setSingleMetricExecution(singleVariationSpecified);
            if (singleVariationSpecified) {
                params.setScatteringDegree(sdValue);
                params.setMetricSpecificSettingValue(metricSpecificValue);
            }
            
            if (metricClass == null) {
                allMetrics = MetricFactory.createAllVariations(params);
            } else {
                allMetrics = MetricFactory.createAllVariations(notNull(metricClass), params);
            }
            
        } catch (SetUpException e) {
            LOGGER.logException("Could not create metric instances", e);
            return;
        }
        
        String[] metrics = new String[allMetrics.size()];
        int metricsIndex = 0;
        for (AbstractFunctionMetric<?> metric : allMetrics) {
            metrics[metricsIndex++] = metric.getResultName();
        }
        Double[] values = new Double[allMetrics.size()];
        
        CodeFunction function;
        while ((function = codeFunctionComponent.getNextResult()) != null) {
            LOGGER.logDebug2("Running for function ", function.getName(), " at ", function.getSourceFile(),
                   ":", function.getFunction().getLineStart());
            
            runForSingleFunction2(allMetrics, metrics, values, function);
        }
    }

    /**
     * Executes all metric variations for a single function.
     * @param allMetrics All metric instances to run.
     * @param metricNames The name of the metrics in the same order.
     * @param values The result array, will be changed as side-effect. Must be as big as the array of metric instances.
     * @param function The function to measure.
     */
    @SuppressWarnings("null")
    private void runForSingleFunction2(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
        @NonNull String @NonNull [] metricNames, @Nullable Double @NonNull [] values, @NonNull CodeFunction function) {
        
        Thread[] threads = new Thread[nThreads];
        // Rounds down
        int partitionSize = allMetrics.size() / nThreads;
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
    
//    /**
//     * Executes all metric variations for a single function.
//     * @param allMetrics All metric instances to run.
//     * @param metricNames The name of the metrics in the same order.
//   * @param values The result array, will be changed as side-effect. Must be as big as the array of metric instances.
//     * @param function The function to measure.
//     */
//    private void runForSingleFunction(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
//      @NonNull String @NonNull [] metricNames, @Nullable Double @NonNull [] values, @NonNull CodeFunction function) {
//        
//        AtomicInteger valuesIndex = new AtomicInteger(0);
//        
//      OrderPreservingParallelizer<AbstractFunctionMetric<?>, Double> prallelizer = new OrderPreservingParallelizer<>(
//            (metric) -> {
//                Number n = metric.compute(function);
//                Double result = null;
//                if (n != null) {
//                    result = n.doubleValue();
//                }
//                return result;
//                
//            }, (result) -> values[valuesIndex.getAndIncrement()] = result, nThreads);
//        
//        for (AbstractFunctionMetric<?> metric : allMetrics) {
//            prallelizer.add(metric);
//        }
//        prallelizer.end();
//        
//        prallelizer.join();
//        
//        MultiMetricResult result = new MultiMetricResult(
//                new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
//                        function.getFunction().getLineStart(), function.getName()),
//                metricNames, values);
//        
//        addResult(result);
//    }

    @Override
    public @NonNull String getResultName() {
        return "AllCodeFunctions";
    }

}
