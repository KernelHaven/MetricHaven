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

/**
 * Singleton to reuse {@link IVariableWeight}s.
 * @author El-Sharkawy
 *
 */
public class WeigthsCache {
    
    public static final WeigthsCache INSTANCE = new WeigthsCache();
    
    private Map<Object, IVariableWeight> cachedWeighters = new HashMap<>();
    
    /**
     * Singleton constructor.
     */
    private WeigthsCache() {}

    /**
     * Returns the specified weight if it was cached already.
     * @param key The key which is used as identifier, ideally a settings value, which is used to create the weight.
     * @return The cached weight or <tt>null</tt> if it was not created and cached so far.
     */
    public synchronized @Nullable IVariableWeight getWeight(Object key) {
        return cachedWeighters.get(key);
    }
    
    /**
     * Caches the specified {@link IVariableWeight} at its identifier <tt>key</tt>.
     * @param key he key which is used as identifier, ideally a settings value, which is used to create the weight.
     * @param weight The weight to be cached.
     */
    public synchronized void add(@NonNull Object key, @NonNull IVariableWeight weight) {
        cachedWeighters.put(key, weight);
    }
    
    /**
     * Clears the cached weights, should be done if a new product line (version) is analyzed in the same process.
     */
    public void clear() {
        cachedWeighters.clear();
    }
}
