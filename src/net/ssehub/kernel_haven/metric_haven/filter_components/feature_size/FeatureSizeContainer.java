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
package net.ssehub.kernel_haven.metric_haven.filter_components.feature_size;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Stores for all variables of the variability model its implementation size (Lines of Code per Feature).
 * 
 * @author El-Sharkawy
 *
 */
public class FeatureSizeContainer {
    private static final int MISSING_VALUE = 0;

    private Map<String, FeatureSize> variables;
    
    /**
     * Sole constructor for this class.
     * @param varModel The variability model, containing all configurable feature variables.
     */
    public FeatureSizeContainer(VariabilityModel varModel) {
        Set<VariabilityVariable> variables = varModel.getVariables();
        this.variables = new HashMap<String, FeatureSize>(variables.size());
        
        for (VariabilityVariable variable : variables) {
            this.variables.put(variable.getName(), new FeatureSize(variable));
        }
    }
    
    /**
     * Returns the Lines of Code configurable via the variable only in its positive form.
     * @param varName The name of the variable for which the feature size shall be returned for.
     * @return Returns positive feature size (LoC) for that feature (&gt; {@value #MISSING_VALUE})
     */
    public int getPositiveSize(String varName) {
        return variables.containsKey(varName) ? variables.get(varName).getPositiveSize() : MISSING_VALUE;
    }
    
    /**
     * Returns the Lines of Code configurable via the variable (in any form).
     * @param varName The name of the variable for which the feature size shall be returned for.
     * @return Returns total feature size (LoC) for that feature (&gt; {@value #MISSING_VALUE})
     */
    public int getTotalSize(String varName) {
        return variables.containsKey(varName) ? variables.get(varName).getTotalSize() : MISSING_VALUE;
    }
    
    /**
     * Returns the number of handled variables.
     * @return The number variables for which SD values are collected.
     */
    public int getSize() {
        return variables.size();
    }
    
    /**
     * Returns {@link FeatureSize}s for the specified.
     * @param varName The name of the variable for which the feature size shall be returned for.
     * @return The {@link FeatureSize} container for the specified variable, or <tt>null</tt> if the variable is not
     *     a variable of the {@link VariabilityModel}.
     */
    @Nullable FeatureSize getFeatureSize(String varName) {
        return variables.get(varName);
    }
    
    /**
     * Returns an iterator over all {@link FeatureSize}s in this collection.
     * 
     * @return An iterator over all {@link FeatureSize}s.
     */
    @NonNull Iterator<FeatureSize> getScatteringDegreeIterator() {
        return notNull(variables.values().iterator());
    }
    
}
