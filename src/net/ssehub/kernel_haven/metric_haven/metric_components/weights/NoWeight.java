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
