package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A component that assigns a rating for variability variables based on their data type.
 * 
 * @author Adam
 */
public class VariableTypeMetric extends AbstractVariabilityVariableRatingComponent {

    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param variableSource The component to get the {@link VariabilityVariable}s from.
     */
    public VariableTypeMetric(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariabilityVariable> variableSource) {
        super(config, variableSource);
    }

    @Override
    protected double getRating(@NonNull VariabilityVariable variable) {
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
            LOGGER.logWarning2("Unknown type: ", variable.getType());
            break;
        }
        
        return result;
    }


}
