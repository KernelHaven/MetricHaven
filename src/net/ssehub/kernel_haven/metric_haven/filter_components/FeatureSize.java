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
package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the size in terms of <b>Lines of Code</b> for the realization of a feature
 * ({@link net.ssehub.kernel_haven.variability_model.VariabilityVariable}).
 * @author El-Sharkawy
 *
 */
public class FeatureSize {
    
    private Map<String, Integer> featureSizes = new HashMap<>(300000);
    
    /**
     * Increments the size of a feature by the measured lines of code.
     * @param variable The variable to increment.
     * @param loc the value to increment with.
     */
    protected void increment(String variable, int loc) {
        int value = getFeautreSize(variable);
        value += loc;
        featureSizes.put(variable, value);
    }
    
    /**
     * Returns the measured size of a feature.
     * @param variable The feature / variability variable for which the size shall be returned.
     * @return The measured lines of code or 0 if nothing was measured for the specified variable.
     */
    public int getFeautreSize(String variable) {
        Integer oldValue =  featureSizes.get(variable);
        return (oldValue == null) ? 0 : oldValue;
    }

}
