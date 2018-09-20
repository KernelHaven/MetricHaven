package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

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
    
    private @NonNull String resultName;
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     */
    public CodeMetricsRunner(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent) {
        super(config);
        
        this.codeFunctionComponent = codeFunctionComponent;
        
        allMetrics = new ArrayList<>();
        MetricFactory factory = new MetricFactory();
        for (Class<? extends AbstractFunctionMetric<?>> metricType : METRICS_TO_CREATE) {
            allMetrics.addAll(factory.createAllVariations(config, metricType));
        }
        
        this.resultName = METRICS_TO_CREATE.size() + " Metrics in " + allMetrics.size() + " Variations";
    }

    @Override
    protected void execute() {
        String[] metrics = new String[allMetrics.size()];
        int metricsIndex = 0;
        for (AbstractFunctionMetric<?> metric : allMetrics) {
            metrics[metricsIndex++] = metric.getResultName();
        }
        Double[] values = new Double[allMetrics.size()];
        
        CodeFunction function;
        while ((function = codeFunctionComponent.getNextResult()) != null) {
            LOGGER.logDebug("Running for function " + function.getName() + " at " + function.getSourceFile()
                   + ":" + function.getFunction().getLineStart());
            
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
