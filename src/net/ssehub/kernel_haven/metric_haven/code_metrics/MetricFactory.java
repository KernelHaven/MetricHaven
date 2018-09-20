package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Creates one or multiple metric instances with their corresponding variations.
 *  
 * @author Adam
 */
public class MetricFactory {

    /**
     * Creates all metric variations for the given metric class.
     * 
     * @param config The pipeline configuration.
     * @param metricClass The metric class to instantiate.
     * 
     * @return A list of all instantiated metric variations.
     */
    public @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllVariations(@NonNull Configuration config,
            @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) {
        return new LinkedList<>(); // TODO
    }
    
}
