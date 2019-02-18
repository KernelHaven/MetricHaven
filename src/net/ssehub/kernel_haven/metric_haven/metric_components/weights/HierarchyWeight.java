package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.HierarchicalVariable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Weights variables based on the specified hierarchy.
 * @author El-Sharkawy
 *
 */
public class HierarchyWeight implements IVariableWeight {
    
    private @Nullable Map<String, Integer> hierarchyWeights;
    private Map<String, VariabilityVariable> varMap;
    private Map<String, Long> varWeights;
    
    /**
     * Sole constructor.
     * @param varModel The variability model, must not be <tt>null</tt>.
     * @param hierarchyWeights The weights which shall be used, one weight for each hierarchy type (if <tt>null</tt>,
     *     the hierarchy level is used directly as weight).
     */
    public HierarchyWeight(@NonNull VariabilityModel varModel, @Nullable Map<String, Integer> hierarchyWeights) {
        varMap = varModel.getVariableMap();
        this.hierarchyWeights = hierarchyWeights;
        varWeights = new HashMap<String, Long>(varMap.size());
    }

    @Override
    public long getWeight(String variable) {
        long result = 0;
        
        Long value = varWeights.get(variable);
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
     * Performs the weighting based either on a specified hierarchy type weighting
     * (cf. <tt>{@link #hierarchyWeights}</tt>) or directly on the hierarchy level.
     * @param var The feature variable to measure/weight.
     * @return The weighting value.
     */
    private long weight(HierarchicalVariable var) {
        long result;
        
        Map<String, Integer> hierarchyWeights = this.hierarchyWeights; // copy to fix null warnings
        if (null != hierarchyWeights) {
            // Weights specified for the type (top, intermediate, leaf)
            if (var.getParent() == null) {
                result = hierarchyWeights.get("top");
            } else if (var.getChildren().isEmpty()) {
                result = hierarchyWeights.get("leaf");
            } else {
                result = hierarchyWeights.get("intermediate");
            }
        } else {
            // Weights by level
            result = var.getNestingDepth() + 1;
        }
        
        varWeights.put(var.getName(), result);
        return result;
    }

    @Override
    public String getName() {
        StringBuffer name = new StringBuffer("Hierarchy ");
        Map<String, Integer> hierarchyWeights = this.hierarchyWeights;
        if (null == hierarchyWeights) {
            name.append("Levels");
        } else {
            name.append("Types");
            name.append('(');
            name.append(hierarchyWeights.get("top"));
            name.append('-');
            name.append(hierarchyWeights.get("intermediate"));
            name.append('-');
            name.append(hierarchyWeights.get("leaf"));
        }
        
        return name.toString();
    }
}
