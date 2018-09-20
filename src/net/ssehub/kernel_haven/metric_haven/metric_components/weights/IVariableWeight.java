package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.io.File;

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
    
    /**
     * Returns the weight for the specified variable and the currently measured code artifact.
     * @param variable An existing variable of the variability model, for which the weight shall be returned for.
     * @param codefile The currently measured code artifact (only required for some weights).
     * @return A positive weight (&ge; 1), 1 denotes a neutral value.
     */
    public default int getWeight(String variable, File codefile) {
        return getWeight(variable);
    }
    
    /**
     * Returns the name of the weight, may be used together with
     * {@link net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric#getResultName()}.
     * @return The name of activated weights.
     */
    public String getName();
}
