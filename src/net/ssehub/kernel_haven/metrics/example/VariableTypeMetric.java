package net.ssehub.kernel_haven.metrics.example;

import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.filters.VariabilityVariableMetric;
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
     */
    public VariableTypeMetric(Configuration config) {
        super(config);
    }

    @Override
    protected double run(VariabilityVariable variable, VariabilityModel model) {
        
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
            System.out.println(variable.getType());
            break;
        }
        
        return result;
    }

}
