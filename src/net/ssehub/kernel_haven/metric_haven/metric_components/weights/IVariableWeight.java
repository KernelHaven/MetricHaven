package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

/**
 * Specifies a weight for a variable of the variabiltiy model.
 * @author El-Sharkawy
 *
 */
public interface IVariableWeight {

    /**
     * Returns the weight for the specified variable.
     * @param variable An existing variable of the variability model, for which the weight shall be returned for.
     * @return A positive weight (&ge; 1), 1 denotes a neutral value.
     */
    public int getWeight(String variable);
}
