package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import net.ssehub.kernel_haven.util.Logger;

/**
 * Specifies a weight for a variable of the variability model.
 * @author El-Sharkawy
 *
 */
public interface IVariableWeight {
    
    public static final Logger LOGGER = Logger.get();

    /**
     * Returns the weight for the specified variable.
     * @param variable An existing variable of the variability model, for which the weight shall be returned for.
     * @return A positive weight (&ge; 1), 1 denotes a neutral value.
     */
    public int getWeight(String variable);
}
