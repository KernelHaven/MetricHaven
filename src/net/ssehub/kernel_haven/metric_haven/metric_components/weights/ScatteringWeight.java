package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.SDType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Weights based on the scattering degree of a variable in code (either variation points or files).
 * @author El-Sharkawy
 *
 */
public class ScatteringWeight implements IVariableWeight {
    
    private @Nullable ScatteringDegreeContainer sdContainer;
    private @NonNull SDType sdType;

    /**
     * Creates a new weight based on scattering degree.
     * @param sdContainer already measure scattering degrees of all variables, must not be <tt>null</tt> if
     *     <tt>sdType</tt> is not {@link SDType#NO_SCATTERING}.
     * @param sdType Specifies which kind of scattering shall be used.
     */
    public ScatteringWeight(@Nullable ScatteringDegreeContainer sdContainer, @NonNull SDType sdType) {
        this.sdContainer = sdContainer;
        this.sdType = sdType;
    }
    
    @Override
    public synchronized int getWeight(String variable) {
        int weight = 1;
        
        ScatteringDegreeContainer sdContainer = this.sdContainer;
        if (sdType != SDType.NO_SCATTERING && null != sdContainer) {
            int value = 0;
            if (sdType == SDType.SD_FILE) {
                value = sdContainer.getSDFile(variable);
            } else {
                value = sdContainer.getSDVariationPoint(variable);
            }
            
            if (0 != value) {
                weight = value;
            }
        }
        
        return weight;
    }

}
