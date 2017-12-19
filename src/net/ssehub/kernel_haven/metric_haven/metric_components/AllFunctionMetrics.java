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
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric.CCType;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;

/**
 * Runs all function metrics in parallel.
 * 
 * @author Adam
 */
public class AllFunctionMetrics extends PipelineAnalysis {

    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllFunctionMetrics(Configuration config) {
        super(config);
    }

    @Override
    protected AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        
        // All Cyclomatic complexity metrics
        config.registerSetting(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING);
        @SuppressWarnings("unchecked")
        AnalysisComponent<MetricResult>[] metrics = new AnalysisComponent[6];
        config.setValue(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING, CCType.MCCABE);
        metrics[0] = new CyclomaticComplexityMetric(config, functionSplitter.createOutputComponent());
        config.setValue(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING, CCType.VARIATION_POINTS);
        metrics[1] = new CyclomaticComplexityMetric(config, functionSplitter.createOutputComponent());
        config.setValue(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING, CCType.ALL);
        metrics[2] = new CyclomaticComplexityMetric(config, functionSplitter.createOutputComponent());

        // All Variables per Function metrics
        config.registerSetting(VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING);
        config.setValue(VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING, VarType.EXTERNAL);
        metrics[3] = new VariablesPerFunctionMetric(config, functionSplitter.createOutputComponent());
        config.setValue(VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING, VarType.INTERNAL);
        metrics[4] = new VariablesPerFunctionMetric(config, functionSplitter.createOutputComponent());
        config.setValue(VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING, VarType.ALL);
        metrics[5] = new VariablesPerFunctionMetric(config, functionSplitter.createOutputComponent());
        
        // join the parallel metrics together
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, metrics);
        
        return join;
    }

}
