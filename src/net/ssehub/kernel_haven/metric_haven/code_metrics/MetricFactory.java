package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Creates one or multiple metric instances with their corresponding variations.
 *  
 * @author Adam
 */
public class MetricFactory {

    private @NonNull Configuration config;
    
    /**
     * Creates this metrics factory.
     * 
     * @param config The pipeline configuration.
     */
    public MetricFactory(@NonNull Configuration config) {
        this.config = config;
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
        
        try {
            // TODO: for now, just create a single instance with the NoWeight
            Constructor<? extends AbstractFunctionMetric<?>> ctor =
                    metricClass.getConstructor(VariabilityModel.class, IVariableWeight.class, LoFType.class);
            // TODO: this is the constructor for the DLoC metric!
            AbstractFunctionMetric<?> metric = notNull(ctor.newInstance(null, NoWeight.INSTANCE, LoFType.DLOC));
            result.add(metric);
            
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new SetUpException("Can't instantiate metric", e);
        }
        
        return result;
    }
    
}
