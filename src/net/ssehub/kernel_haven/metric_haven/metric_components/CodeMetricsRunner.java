package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A processing unit for executing metrics.
 *  
 * @author Adam
 */
public class CodeMetricsRunner extends AnalysisComponent<MultiMetricResult> {

    // TODO: read from setting
    private static final List<@NonNull Class<? extends AbstractFunctionMetric<?>>> METRICS_TO_CREATE;
    
    static {
        METRICS_TO_CREATE = new LinkedList<>();
        METRICS_TO_CREATE.add(DLoC.class);
    }
    
    private @NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics;
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent;
    private @NonNull AnalysisComponent<VariabilityModel> varModelComponent;
    private @NonNull AnalysisComponent<BuildModel> bmComponent;
    private @NonNull AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    
    private @NonNull String resultName;
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public CodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent,
        @NonNull AnalysisComponent<BuildModel> bmComponent,
        @NonNull AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config);
        
        this.codeFunctionComponent = codeFunctionComponent;
        this.varModelComponent = varModelComponent;
        this.bmComponent = bmComponent;
        this.sdComponent = sdComponent;
        
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = varModelComponent.getNextResult();
        BuildModel bm = bmComponent.getNextResult();
        ScatteringDegreeContainer sdContainer = sdComponent.getNextResult();
        
        if (null == varModel || bm == null || null == sdContainer) {
            LOGGER.logError("Something was null, could not create weights: ");
            return;
        }
        try {
            allMetrics = MetricFactory.createAllVariations(varModel, bm , sdContainer);
        } catch (SetUpException e) {
            LOGGER.logException("Could not create instances", e);
            return;
        }
        
        this.resultName = METRICS_TO_CREATE.size() + " Metrics in " + allMetrics.size() + " Variations";
        
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
            
            int valuesIndex = 0;
            for (AbstractFunctionMetric<?> metric : allMetrics) {
                values[valuesIndex++] = metric.compute(function).doubleValue();
            }
            
            MultiMetricResult result = new MultiMetricResult(
                    new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
                            function.getFunction().getLineStart(), function.getName()),
                    metrics, values);
            
            addResult(result);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return resultName;
    }

}
