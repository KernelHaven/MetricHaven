/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Scattering degree based on cross-tree constraint ratio.
 * @author El-Sharkawy
 *
 */
public class CtcrWeight implements IVariableWeight {
    
    private @Nullable Map<String, VariabilityVariable> varMapping = null;
    private @NonNull CTCRType ctcrType;
    
    // Cached values
    private Map<String, Long> varWeights;
    
    /**
     * Creates a new weight based on cross-tree constraint ratios.
     * @param varModel Must be a variability model, which has the ability to provide information about constraint usage.
     *     May only be <tt>null</tt> if <tt>ctcrType</tt> is {@link CTCRType#NO_CTCR}, but than {@link NoWeight} is
     *     recommended as it produces the same result with less overhead.
     * @param ctcrType Specifies which kind of cross-tree constraint ratio shall be used.
     * 
     * @throws SetUpException If the var model does not match the configuration.
     */
    public CtcrWeight(@Nullable VariabilityModel varModel, @NonNull CTCRType ctcrType) throws SetUpException {
        this.ctcrType = ctcrType;
        if (null != varModel && varModel.getDescriptor().hasAttribute(Attribute.CONSTRAINT_USAGE)) {
            varMapping = varModel.getVariableMap();
            varWeights = new HashMap<>(varMapping.size());
        } else {
            throw new SetUpException("CtcrWeight without an approriate variability model created.");
        }
    }

    @Override
    public synchronized long getWeight(String variable) {
        long weight = 0;
        
        Long value = varWeights.get(variable);
        if (null != value) {
            weight = value;
        } else if (null != varMapping) {
            VariabilityVariable var = varMapping.get(variable);
            switch (ctcrType) {
            case NO_CTCR:
                // Handled already outside of switch block
                break;
            case INCOMIG_CONNECTIONS:
                if (null != var) {
                    Set<VariabilityVariable> otherVars = var.getUsedInConstraintsOfOtherVariables();
                    if (null != otherVars) {
                        weight = otherVars.size();
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
                        weight = otherVars.size();
                    }
                } else {
                    LOGGER.logWarning2("Could not compute outgoing constraint relations in ", getClass().getName(),
                        ", because " , variable , " was not found in variability model.");
                }
                break;
            case ALL_CTCR:
                if (null != var) {
                    Set<VariabilityVariable> inVars = var.getUsedInConstraintsOfOtherVariables();
                    Set<VariabilityVariable> outVars = var.getVariablesUsedInConstraints();
                    
                    Set<VariabilityVariable> union = new HashSet<>();
                    if (null != inVars) {
                        union.addAll(inVars);
                    }
                    if (null != outVars) {
                        union.addAll(outVars);
                    }
                    weight = union.size();
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
            varWeights.put(variable, weight);
        }
        
        return weight;
    }

    @Override
    public String getName() {
        return ctcrType.name();
    }
}
