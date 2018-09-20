package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Creates one or multiple metric instances with their corresponding variations.
 *  
 * @author Adam
 */
public class MetricFactory {

    /**
     * Creates all variations of the DLoC metric.
     * 
     * @return All variations of the DLoC metric.
     */
    public @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllDLoCVariations() {

        List<@NonNull AbstractFunctionMetric<?>> result = new LinkedList<>();
        
        for (LoFType type : LoFType.values()) {
            try {
                result.add(new DLoC(null, NoWeight.INSTANCE, type));
                
            } catch (UnsupportedMetricVariationException e) {
                // ignore
            }
        }
        
        return result;
    }
    
    /**
     * Creates all metric variations for the given metric class.
     * 
     * @param metricClass The metric class to instantiate.
     * 
     * @return A list of all instantiated metric variations.
     * 
     * @throws SetUpException If creating the metric variations fails.
     */
    public @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllVariations(
            @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> result = new LinkedList<>();
        
        // TODO: how to get the right constructor for the metricClass?
        // TODO: how to get the right parameters for the metricClass?
        // TODO: how to get all allowed metric specific settings?
        
        return result;
    }
    
}
