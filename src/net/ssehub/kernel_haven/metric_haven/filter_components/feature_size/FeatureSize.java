package net.ssehub.kernel_haven.metric_haven.filter_components.feature_size;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Stores for a {@link VariabilityVariable} the accumulated size (Lines of Code) of the variation points (&#35;ifdefs)
 * where it is used in.
 * 
 * @author El-Sharkawy
 */
@TableRow
public class FeatureSize {

    private @NonNull VariabilityVariable variable;
    
    private int positiveSize;
    
    private int totalSize;
    
    /**
     * Creates an instance for the given {@link VariabilityVariable}.
     * 
     * @param variable The variable.
     */
    public FeatureSize(@NonNull VariabilityVariable variable) {
        this.variable = variable;
    }
    
    /**
     * Returns the variable that was counted by this object.
     * @return The variable that this object has the counts for.
     */
    public @NonNull VariabilityVariable getVariable() {
        return variable;
    }
    
    /**
     * Returns the name of the variable that was counted by this object.
     * 
     * @return The name of the variable that this object has the counts for.
     */
    @TableElement(index = 0, name = "Variable")
    public @NonNull String getVariableName() {
        return variable.getName();
    }
    
    /**
     * Returns the Lines of Code where the variable is used only in its positive form.
     * 
     * @return The Lines of Code where the variable is used only in its positive form.
     */
    @TableElement(index = 1, name = "Pos. Size")
    public int getPositiveSize() {
        return positiveSize;
    }
    
    /**
     * Returns the Lines of Code where the variable is used in (positively and negatively).
     * 
     * @return The Lines of Code where the variable is used in.
     */
    @TableElement(index = 2, name = "Total Size")
    public int getTotalSize() {
        return totalSize;
    }
    
    /**
     * Increases the positive LoC size of the feature.
     * @param loc The number of measured lines of code, which shall be added to that feature.
     */
    public void incPositiveSize(int loc) {
        positiveSize += loc;
    }
    
    /**
     * Increases the total LoC size of the feature.
     * @param loc The number of measured lines of code, which shall be added to that feature.
     */
    public void incTotalSize(int loc) {
        totalSize += loc;
    }
    
}
