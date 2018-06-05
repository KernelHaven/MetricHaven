package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;

/**
 * Scattering degree based on cross-tree constraint ratio.
 * @author El-Sharkawy
 *
 */
public class CtcrWeight implements IVariableWeight {
    
    private @Nullable Map<String, VariabilityVariable> varMapping = null;
    private @NonNull CTCRType ctcrType;
    
    /**
     * Creates a new weight based on cross-tree constraint ratios.
     * @param varModel Must be a variability model, which has the ability to provide information about constraint usage.
     *     May only be <tt>null</tt> if <tt>ctcrType</tt> is {@link CTCRType#NO_CTCR}, but than {@link NoWeight} is
     *     recommended as it produces the same result with less overhead.
     * @param ctcrType Specifies which kind of cross-tree constraint ratio shall be used.
     */
    public CtcrWeight(@Nullable VariabilityModel varModel, @NonNull CTCRType ctcrType) {
        this.ctcrType = ctcrType;
        if (null != varModel && varModel.getDescriptor().hasAttribute(Attribute.CONSTRAINT_USAGE)) {
            varMapping = varModel.getVariableMap();
        }
    }

    @Override
    public synchronized int getWeight(String variable) {
        int weight = 1;
        
        if (null != varMapping) {
            VariabilityVariable var = varMapping.get(variable);
            switch (ctcrType) {
            case NO_CTCR:
                // Handled already outside of switch block
                break;
            case INCOMIG_CONNECTIONS:
                if (null != var) {
                    Set<VariabilityVariable> otherVars = var.getUsedInConstraintsOfOtherVariables();
                    if (null != otherVars) {
                        weight = otherVars.size() + 1;
                    }
                } else {
                    LOGGER.logWarning2("Could not compute incoming constraint relations in ", getClass().getName(),
                        ", because " , variable , " was not found in variability model.");
                }
                break;
            case OUTGOING_CONNECTIONS:
                if (null != var) {
                    Set<VariabilityVariable> otherVars = var.getVariablesUsedInConstraints();
                    if (null != otherVars) {
                        weight = otherVars.size() + 1;
                    }
                } else {
                    LOGGER.logWarning2("Could not compute outgoing constraint relations in ", getClass().getName(),
                        ", because " , variable , " was not found in variability model.");
                }
                break;
            case ALL_CTCR:
                if (null != var) {
                    Set<VariabilityVariable> inVars = var.getVariablesUsedInConstraints();
                    Set<VariabilityVariable> outVars = var.getVariablesUsedInConstraints();
                    if (null != inVars && null != outVars) {
                        weight = inVars.size() + outVars.size() + 1;
                    }
                } else {
                    LOGGER.logWarning2("Could not compute full ctcr in ", getClass().getName(),
                        ", because " , variable , " was not found in variability model.");
                }
                break;
            default:
                LOGGER.logWarning2("Unknown CTCR type specified, cannot compute cross-tree constraint ration in ",
                    getClass().getName(), ", for CTCR type " , ctcrType.name());
                break;
            }
        }
        
        return weight;
    }

}
