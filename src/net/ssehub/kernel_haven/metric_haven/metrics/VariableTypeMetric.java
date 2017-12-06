package net.ssehub.kernel_haven.metric_haven.metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filters.VariabilityVariableMetric;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A metric that assigns values for variability variables based on their data types.
 * 
 * @author Adam
 */
public class VariableTypeMetric extends VariabilityVariableMetric {

    /**
     * Creates a new variable type metric.
     * 
     * @param config The global pipeline configuration.
     * 
     * @throws SetUpException If creating this metric fails. 
     */
    public VariableTypeMetric(Configuration config) throws SetUpException {
        super(config);
    }

    @Override
    protected double calc(VariabilityVariable variable, VariabilityModel model) {
        
        int result = 1;
        
        switch (variable.getType()) {
        case "bool":
            result = 1;
            break;
        case "tristate":
            result = 2;
            break;
        case "integer":
        case "hex":
            result = 10;
            break;
        case "string":
            result = 15;
            break;
        default:
            LOGGER.logWarning("Unknown type: " + variable.getType());
            break;
        }
        
        return result;
    }

}
