package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.HierarchicalVariable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Weights variables based on the structure of the variability model.
 * @author El-Sharkawy
 *
 */
public class StructuralWeight implements IVariableWeight {
    
    private Map<String, VariabilityVariable> varMap;
    private Map<String, Integer> varWeights;
    private StructuralType structureType;
    
    /**
     * Sole constructor.
     * @param varModel The variability model, must not be <tt>null</tt>.
     * @param structureType The structural property to measure, must not be
     *     {@link StructuralType#NO_STRUCTURAL_MEASUREMENT}.
     */
    public StructuralWeight(@NonNull VariabilityModel varModel, @NonNull StructuralType structureType) {
        varMap = varModel.getVariableMap();
        varWeights = new HashMap<String, Integer>(varMap.size());
        this.structureType = structureType;
    }

    @Override
    public int getWeight(String variable) {
        int result = 0;
        
        Integer value = varWeights.get(variable);
        if (null != value) {
            result = value;
        } else {
            VariabilityVariable var = varMap.get(variable);
            if (null != var && var instanceof HierarchicalVariable) {
                result = weight((HierarchicalVariable) var);
            }
        }
        
        return result;
    }

    /**
     * Performs the weighting based either on the structure in the variability model.
     * @param var The feature variable to weight.
     * @return The weighting value.
     */
    private int weight(HierarchicalVariable var) {
        int result;
        
        switch (structureType) {
        case NO_STRUCTURAL_MEASUREMENT:
            result = 1;
            LOGGER.logWarning2(getClass().getName(), " used to measure nothing, because structureType was set to ",
                StructuralType.NO_STRUCTURAL_MEASUREMENT.name());
            break;
        case NUMBER_OF_CHILDREN:
            result = var.getChildren() != null ? var.getChildren().size() : 0;
            break;
        case COC:
            Set<VariabilityVariable> connectedVariables = new HashSet<>();
            if (null != var.getChildren()) {
                connectedVariables.addAll(var.getChildren());
            }
            if (null != var.getParent()) {
                connectedVariables.add(var.getParent());
            }
            if (null != var.getUsedInConstraintsOfOtherVariables()) {
                connectedVariables.addAll(var.getUsedInConstraintsOfOtherVariables());
            }
            if (null != var.getVariablesUsedInConstraints()) {
                connectedVariables.addAll(var.getVariablesUsedInConstraints());
            }
            result = connectedVariables.size();
            break;
        default:
            result = 0;
            LOGGER.logError2("unknown structural type measure specified :",
                StructuralType.NO_STRUCTURAL_MEASUREMENT.name());
            break;
        }
        
        varWeights.put(var.getName(), result);
        return result;
    }

}
