package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Scattering degree based on cross-tree constraint ratio.
 * @author El-Sharkawy
 *
 */
public class CtcrWeight implements IVariableWeight {
    
    private @Nullable VariabilityModel varModel = null;
    
    /**
     * Creates a new weight based on cross-tree constraint ratios.
     * @param varModel Must be a variability model, which has the ability to provide information about constraint usage.
     */
    public CtcrWeight(@Nullable VariabilityModel varModel) {
        if (null != varModel && varModel.getDescriptor().hasConstraintUsage()) {
            this.varModel = varModel;
        }
    }

    @Override
    public synchronized int getWeight(String variable) {
        int weight = 1;
        
        if (null != varModel) {
            VariabilityVariable var = varModel.getVariableMap().get(variable);
            if (null != var) {
                weight += var.getVariablesUsedInConstraints().size()
                    + var.getUsedInConstraintsOfOtherVariables().size();
            }
        }
        
        return weight;
    }

}
