package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;


/**
 * Weights variables based on the specified datatype ({@link VariabilityVariable#getType()}).
 * @author El-Sharkawy
 *
 */
public class TypeWeight implements IVariableWeight {
    
    private Map<String, Integer> typeWeights;
    private Map<String, VariabilityVariable> varMap;
    private Map<String, Long> varWeights;
    
    /**
     * Sole constructor.
     * @param varModel The variability model, must not be <tt>null</tt>.
     * @param typeWeights The weights which shall be used, one weight for each type.
     */
    public TypeWeight(@NonNull VariabilityModel varModel, @NonNull Map<String, Integer> typeWeights) {
        varMap = varModel.getVariableMap();
        this.typeWeights = typeWeights;
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
            if (null != var) {
                if (typeWeights.containsKey(var.getType())) {
                    result = typeWeights.get(var.getType());
                    // Avoid multiple hashing next time when the variable is measured.
                    varWeights.put(variable, result);
                    
                } else {
                    Logger.get().logWarning2("No weight specified for type ", var.getType(), " of variable ", variable);
                }
            }
            // else: leave result as 0 (non-features have no weight)
        }
        
        return result;
    }

    @Override
    public String getName() {
        return "Feature Type";
    }

}
