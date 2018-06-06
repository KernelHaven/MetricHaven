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
