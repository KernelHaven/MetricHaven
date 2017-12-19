package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;

/**
 * An example pipeline that executes 2 metrics in parallel with the same input component.
 * 
 * @author Adam
 */
public class ExampleParallelMetrics extends PipelineAnalysis {

    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public ExampleParallelMetrics(Configuration config) {
        super(config);
    }

    @Override
    protected AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        AnalysisComponent<MetricResult> metric1
                = new CyclomaticComplexityMetric(config, functionSplitter.createOutputComponent());
        AnalysisComponent<MetricResult> metric2
                = new VariablesPerFunctionMetric(config, functionSplitter.createOutputComponent());
        
        // join the parallel metrics together
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, metric1, metric2);
        
        return join;
    }

}
