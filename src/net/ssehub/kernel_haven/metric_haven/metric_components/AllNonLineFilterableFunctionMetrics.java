package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.ObservableAnalysis;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByLineFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.metric_components.FanInOutMetric.FanType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics that can not be filtered by a {@link CodeFunctionByLineFilter} in parallel.
 * 
 * @author Adam
 */
public class AllNonLineFilterableFunctionMetrics extends PipelineAnalysis {

    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     */
    private static boolean addObservable = false;
    
    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllNonLineFilterableFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        
        @SuppressWarnings("unchecked")
        @NonNull AnalysisComponent<MetricResult>[] metrics = new @NonNull AnalysisComponent[4];
        
        // Fan-in / Fan-out
        config.registerSetting(FanInOutMetric.FAN_TYPE_SETTING);
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_IN_GLOBALLY);
        metrics[0] = new FanInOutMetric(config, functionSplitter.createOutputComponent());
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_IN_LOCALLY);
        metrics[1] = new FanInOutMetric(config, functionSplitter.createOutputComponent());
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_OUT_GLOBALLY);
        metrics[2] = new FanInOutMetric(config, functionSplitter.createOutputComponent());
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_OUT_LOCALLY);
        metrics[3] = new FanInOutMetric(config, functionSplitter.createOutputComponent());
        
        // join the parallel metrics together
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, "All Function Metrics", metrics);
        
        if (addObservable) {
            join = new ObservableAnalysis<>(config, join);
        }
        
        return join;
    }
    
    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     * Default is <code>false</code>.
     * 
     * @param addObservable <code>true</code> if an {@link ObservableAnalysis} should be added.
     */
    public static void setAddObservable(boolean addObservable) {
        AllNonLineFilterableFunctionMetrics.addObservable = addObservable;
    }
    
}
