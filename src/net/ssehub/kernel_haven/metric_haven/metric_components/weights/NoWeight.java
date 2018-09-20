package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

/**
 * Programmatically solution to use a <i>neutral</i> weight, which does not change the result.
 * @author El-Sharkawy
 *
 */
public class NoWeight implements IVariableWeight {
    
    public static final NoWeight INSTANCE = new NoWeight();
    
    /**
     * Singleton constructor.
     */
    private NoWeight() {}

    @Override
    public int getWeight(String variable) {
        return 1;
    }

    @Override
    public String getName() {
        return "No weight";
    }
}
