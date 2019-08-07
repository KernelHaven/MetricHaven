/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Thread safe map to store intermediate results of the {@link ScatteringVisitor}.
 *
 * @author El-Sharkawy
 */
class CountedVariables {
    
    private @NonNull Map<@NonNull String, ScatteringDegree> countedVariables = new HashMap<>();
    
    /**
     * Initializes the map with all elements of the variability model.
     * @param varModel The variability model which contains all elements to be counted.
     */
    void init(@NonNull VariabilityModel varModel) {
        for (VariabilityVariable variable : varModel.getVariables()) {
            countedVariables.put(variable.getName(), new ScatteringDegree(variable));
        }
    }
    
    /**
     * Returns the {@link ScatteringDegree} for the specified variable.
     * <b>Attention:</b> The name may change in case of a tristate variable was passed to this store.
     * @param varName The variable for which the {@link ScatteringDegree} shall be returned.
     * @return The {@link ScatteringDegree} for the specified variable, which may has a different name.
     */
    @Nullable ScatteringDegree getScatteringVariable(String varName) {
        ScatteringDegree countedVar = countedVariables.get(varName);
        
        // heuristically handle tristate (_MODULE) variables
        if (countedVar == null && varName.endsWith("_MODULE")) {
            varName = notNull(varName.substring(0, varName.length() - "_MODULE".length()));
            countedVar = countedVariables.get(varName);
        }
        
        return countedVar;
    }
    
    /**
     * Returns the results and should only be called after the last element was processed.
     * @return The results.
     */
    @NonNull Collection<ScatteringDegree> getResults() {
        return NullHelpers.notNull(countedVariables.values());
    }

}
