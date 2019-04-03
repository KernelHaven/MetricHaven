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
        if (null != hierarchyWeights) {
            this.hierarchyWeights = new HashMap<>();
            this.hierarchyWeights.putAll(hierarchyWeights);
        }
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
            name.append(')');
        }
        
        return name.toString();
    }
}
