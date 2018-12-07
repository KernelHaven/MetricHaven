package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByLineFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.FunctionMapCreator;
import net.ssehub.kernel_haven.metric_haven.filter_components.OrderedCodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeEstimator;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.VariabilityCounter;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs metrics.
 * 
 * @author El-Sharkawy
 * @author Adam
 */
public class MetricsRunner extends PipelineAnalysis {
    
    /**
     * Whether a line filter has been configured. If this is <code>true</code>, a {@link CodeFunctionByLineFilter}
     * should be added in the pipeline.
     */
    private boolean lineFilter;
    
    /**
     * Single constructor to instantiate and execute all variations of a single metric-analysis class.
     * 
     * @param config The global configuration.
     * 
     * @throws SetUpException In case of problems with the configuration.
     */
    public MetricsRunner(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        lineFilter = false;
        config.registerSetting(MetricSettings.LINE_NUMBER_SETTING);
        Object value = config.getValue(MetricSettings.LINE_NUMBER_SETTING);
        if (null != value) {
            lineFilter = true;
        }
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        /*
         * Unfiltered:
         * 
         * Code Model --+---+-> OrderedCodeFunctionFilter -> Split -+-> FunctionMapCreator -> |
         *              |   |                                       |                         |
         *              |   +-> |                                   +-----------------------> |
         *              |       | VariabilityCounter -+                                       |
         *              |   +-> |                     +-------------------------------------> | CodeMetricsRunner
         *              |   |                                                                 |
         * Var Model ----+--+---------------------------------------------------------------> |
         *              ||                                                                    |
         * Build Model ---+-----------------------------------------------------------------> |
         *              |||                                                                   |
         *              ||+-> |                                                               |
         *              |+--> | FeatureSizeEstimator ---------------------------------------> |
         *              +---> |
         */
        
        /*
         * Filtered:
         * 
         * Code Model --+---+-> OrderedCodeFunctionFilter -> Split -+-> FunctionMapCreator -------> |
         *              |   |                                       |                               |
         *              |   +-> |                                   +-> CodeFunctionByLineFilter -> |
         *              |       | VariabilityCounter -+                                             |
         *              |   +-> |                     +-------------------------------------------> | CodeMetricsRunner
         *              |   |                                                                       |
         * Var Model ----+--+---------------------------------------------------------------------> |
         *              ||                                                                          |
         * Build Model ---+-----------------------------------------------------------------------> |
         *              |||                                                                         |
         *              ||+-> |                                                                     |
         *              |+--> | FeatureSizeEstimator ---------------------------------------------> |
         *              +---> |
         */
        
        AnalysisComponent<CodeFunction> orderedFunctionFilter = new OrderedCodeFunctionFilter(config, getCmComponent());
        SplitComponent<CodeFunction> split = new SplitComponent<>(config, orderedFunctionFilter);
        
        AnalysisComponent<ScatteringDegreeContainer> variabilityCounter
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        AnalysisComponent<FunctionMap> functionMapCreator = new FunctionMapCreator(config,
            split.createOutputComponent());
        
        AnalysisComponent<FeatureSizeContainer> featureSizeCreator = new FeatureSizeEstimator(config,
                getVmComponent(), getCmComponent(), getBmComponent());
        
        AnalysisComponent<CodeFunction> functionInput = split.createOutputComponent();
        if (lineFilter) {
            functionInput = new CodeFunctionByLineFilter(config, functionInput);
        }
        
        CodeMetricsRunner metricAnalysis
            = new CodeMetricsRunner(config, functionInput, getVmComponent(),
                getBmComponent(), variabilityCounter, functionMapCreator, featureSizeCreator);
        
        return metricAnalysis;
    }
    
}
