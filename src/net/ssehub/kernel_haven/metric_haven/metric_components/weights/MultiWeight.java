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

import java.io.File;
import java.util.List;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A composite weighting function.
 * @author El-Sharkawy
 *
 */
public class MultiWeight implements IVariableWeight {

    private @NonNull IVariableWeight[] weights;
    
    /**
     * Creates a composite weighting function.
     * @param weights The weights to be used, will be multiplied with each other.
     */
    public MultiWeight(@NonNull IVariableWeight... weights) {
        this.weights = weights;
    }
    
    /**
     * Creates a composite weighting function.
     * @param weights The weights to be used, will be multiplied with each other.
     */
    @SuppressWarnings("null")
    public MultiWeight(@NonNull List<IVariableWeight> weights) {
        this(weights.toArray(new IVariableWeight[weights.size()]));
    }
    
    @Override
    public long getWeight(String variable) {
        long weight = 1;
        for (int i = 0; i < weights.length; i++) {
            weight *= weights[i].getWeight(variable);
        }
        
        return weight;
    }
    
    @Override
    public long getWeight(String variable, File codefile) {
        long weight = 1;
        for (int i = 0; i < weights.length; i++) {
            weight *= weights[i].getWeight(variable, codefile);
        }
        
        return weight;
    }

    @Override
    public String getName() {
        StringBuilder name = new StringBuilder();
        if (weights.length > 0) {
            name.append(weights[0].getName());
        }
        for (int i = 1; i < weights.length; i++) {
            name.append(" x ");
            name.append(weights[i].getName());
        }
        
        return name.toString();
    }
}
