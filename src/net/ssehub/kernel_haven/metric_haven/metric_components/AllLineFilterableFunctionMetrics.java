package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.ObservableAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByLineFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.metric_components.NestingDepthMetric.NDType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics that can be filtered by a {@link CodeFunctionByLineFilter} in parallel.
 * 
 * @author Adam
 */
public class AllLineFilterableFunctionMetrics extends AbstractMultiFunctionMetrics {

    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     */
    private static boolean addObservable = false;
    
    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllLineFilterableFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        functionFilter = new CodeFunctionByLineFilter(config, functionFilter);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        
        // All Cyclomatic complexity metrics
        config.registerSetting(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING);
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics = new LinkedList<>();
        
        // All dLoC per Function metrics
        addMetric(DLoC.class, DLoC.LOC_TYPE_SETTING, functionSplitter, null, metrics,
            net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.values());
        
        // All Nesting Depth metrics
        addMetric(NestingDepthMetric.class, NestingDepthMetric.ND_TYPE_SETTING, functionSplitter, null, metrics,
            NDType.values());
        
        // join the parallel metrics together
        @SuppressWarnings({ "null", "unchecked" })
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, "All Function Metrics",
            metrics.toArray(new AnalysisComponent[metrics.size()]));
        
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
        AllLineFilterableFunctionMetrics.addObservable = addObservable;
    }
    
}
