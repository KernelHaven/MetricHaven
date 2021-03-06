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
        this.typeWeights = new HashMap<>();
        this.typeWeights.putAll(typeWeights);
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
        StringBuffer name = new StringBuffer("Feature Type");
        name.append('(');
        boolean elementAdded = false;
        for (Map.Entry<String, Integer> setting : typeWeights.entrySet()) {
            if (setting.getValue() != 0) {
                if (elementAdded) {
                    name.append(", ");
                }
                name.append(setting.getKey());
                name.append("=");
                name.append(setting.getValue());
                
                elementAdded = true;
            }
        }
        name.append(')');
        
        return name.toString();
    }

}
