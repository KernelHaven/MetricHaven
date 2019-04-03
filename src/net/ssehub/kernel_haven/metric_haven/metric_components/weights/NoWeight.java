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

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Programmatically solution to use a <i>neutral</i> weight, which does not change the result.
 * @author El-Sharkawy
 *
 */
public class NoWeight implements IVariableWeight {
    
    public static final @NonNull NoWeight INSTANCE = new NoWeight();
    
    /**
     * Singleton constructor.
     */
    private NoWeight() {}

    @Override
    public long getWeight(String variable) {
        return 1;
    }

    @Override
    public String getName() {
        return "No weight";
    }
}
