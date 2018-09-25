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
    
    private boolean round = false;
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    private @Nullable AnalysisComponent<FunctionMap> fmComponent;
    
    private int nThreads;
    
    private long time = 0;
    
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
            
            allMetrics = MetricFactory.createAllVariations(params);
            
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
        
        LOGGER.logStatusLines("Execution of metrics took " + time, " ms in total.");
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
        
        long start = System.currentTimeMillis();
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
        
        time += System.currentTimeMillis() - start;
        
        MeasuredItem funcDescription = new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
            function.getFunction().getLineStart(), function.getName());
        MultiMetricResult result = new MultiMetricResult(funcDescription, metricNames, values);
        
        addResult(result);
    }
    
//    /**
//     * Executes all metric variations for a single function.
//     * @param allMetrics All metric instances to run.
//     * @param metricNames The name of the metrics in the same order.
//     * @param values The result array, will be changed as side-effect. Must be as big as the array of metric instances.
//     * @param function The function to measure.
//     */
//    private void runForSingleFunction(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
//        @NonNull String @NonNull [] metricNames, @Nullable Double @NonNull [] values, @NonNull CodeFunction function) {
//        
//        AtomicInteger valuesIndex = new AtomicInteger(0);
//        
//        OrderPreservingParallelizer<AbstractFunctionMetric<?>, Double> prallelizer = new OrderPreservingParallelizer<>(
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
