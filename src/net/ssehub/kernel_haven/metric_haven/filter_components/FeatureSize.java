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
