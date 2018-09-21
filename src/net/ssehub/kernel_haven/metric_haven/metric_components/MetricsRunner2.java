package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.OrderedCodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.VariabilityCounter;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all variations of a single metric-analysis class.
 * @author El-Sharkawy
 *
 */
public class MetricsRunner2 extends AbstractMultiFunctionMetrics {
    
   
    private Class<? extends AbstractFunctionVisitorBasedMetric<?>> metricClass;

    /**
     * Single constructor to instantiate and execute all variations of a single metric-analysis class.
     * @param config The global configuration.
     * @throws SetUpException In case of problems with the configuration of {@link #METRICS_CLASS}.
     */
    @SuppressWarnings("unchecked")
    public MetricsRunner2(@NonNull Configuration config) throws SetUpException {
        super(config);
    }

    @Override
    @SuppressWarnings("null")
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new OrderedCodeFunctionFilter(config, codeModel);
        
        AnalysisComponent<ScatteringDegreeContainer> sdAnalysis
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        CodeMetricsRunner metricAnalysis
            = new CodeMetricsRunner(config, functionFilter, getVmComponent(), getBmComponent(), sdAnalysis);
        
        return metricAnalysis;
    }
    
}
