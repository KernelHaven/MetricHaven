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

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Weights based on the scattering degree of a variable in code (either variation points or files).
 * @author El-Sharkawy
 *
 */
public class ScatteringWeight implements IVariableWeight {
    
    private @Nullable ScatteringDegreeContainer sdContainer;
    private @NonNull SDType sdType;

    /**
     * Creates a new weight based on scattering degree.
     * @param sdContainer already measure scattering degrees of all variables, must not be <tt>null</tt> if
     *     <tt>sdType</tt> is not {@link SDType#NO_SCATTERING}.
     * @param sdType Specifies which kind of scattering shall be used.
     * 
     * @throws SetUpException If a scattering degree container is required, but null.
     */
    public ScatteringWeight(@Nullable ScatteringDegreeContainer sdContainer, @NonNull SDType sdType)
            throws SetUpException {
        
        if (sdType != SDType.NO_SCATTERING && sdContainer == null) {
            throw new SetUpException("ScatteringWeight requires a ScatteringDegreeContainer");
        }
        
        this.sdContainer = sdContainer;
        this.sdType = sdType;
    }
    
    @Override
    public long getWeight(String variable) {
        long weight = 1;
        
        ScatteringDegreeContainer sdContainer = this.sdContainer;
        if (sdType != SDType.NO_SCATTERING && null != sdContainer) {
            int value = 0;
            if (sdType == SDType.SD_FILE) {
                value = sdContainer.getSDFile(variable);
            } else {
                value = sdContainer.getSDVariationPoint(variable);
            }
            
            if (0 != value) {
                weight = value;
            }
        }
        
        return weight;
    }

    @Override
    public String getName() {
        return sdType.name();
    }
}
