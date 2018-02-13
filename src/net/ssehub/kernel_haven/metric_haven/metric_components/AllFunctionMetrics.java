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
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric.CCType;
import net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.metric_components.NestingDepthMetric.NDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics in parallel.
 * 
 * @author Adam
 */
public class AllFunctionMetrics extends PipelineAnalysis {

    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     */
    public static boolean ADD_OBSERVEABLE = false;
    
    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        
        // All Cyclomatic complexity metrics
        config.registerSetting(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING);
        @SuppressWarnings("unchecked")
        @NonNull AnalysisComponent<MetricResult>[] metrics = new @NonNull AnalysisComponent[15];
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
        
        // All dLoC per Function metrics
        config.registerSetting(DLoC.LOC_TYPE_SETTING);
        config.setValue(DLoC.LOC_TYPE_SETTING, LoFType.DLOC);
        metrics[6] = new DLoC(config, functionSplitter.createOutputComponent());
        config.setValue(DLoC.LOC_TYPE_SETTING, LoFType.LOF);
        metrics[7] = new DLoC(config, functionSplitter.createOutputComponent());
        config.setValue(DLoC.LOC_TYPE_SETTING, LoFType.PLOF);
        metrics[8] = new DLoC(config, functionSplitter.createOutputComponent());
        
        // All Nesting Depth metrics
        config.registerSetting(NestingDepthMetric.ND_TYPE_SETTING);
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.CLASSIC_ND_MAX);
        metrics[9] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.CLASSIC_ND_AVG);
        metrics[10] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.VP_ND_MAX);
        metrics[11] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.VP_ND_AVG);
        metrics[12] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.COMBINED_ND_MAX);
        metrics[13] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        config.setValue(NestingDepthMetric.ND_TYPE_SETTING, NDType.COMBINED_ND_AVG);
        metrics[14] = new NestingDepthMetric(config, functionSplitter.createOutputComponent());
        
        // join the parallel metrics together
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, "All Function Metrics", metrics);
        
        if (ADD_OBSERVEABLE) {
            join = new ObservableAnalysis<>(config, join);
        }
        
        return join;
    }

}
