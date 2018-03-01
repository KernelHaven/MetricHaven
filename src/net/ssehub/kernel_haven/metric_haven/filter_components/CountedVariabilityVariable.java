package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Contains a {@link VariabilityVariable} and in how many #ifdefs and files it is used.
 * 
 * @author Adam
 */
public class CountedVariabilityVariable {

    private @NonNull VariabilityVariable variable;
    
    private int ifdefs;
    
    private int files;
    
    /**
     * Creates an instance for the given {@link VariabilityVariable}.
     * 
     * @param variable The variable.
     */
    public CountedVariabilityVariable(@NonNull VariabilityVariable variable) {
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
     * Returns the number of #ifdefs that this variable was used in.
     * 
     * @return The number of #ifdefs that this variable was used in.
     */
    public int getIfdefs() {
        return ifdefs;
    }
    
    /**
     * Returns the number of source files that this variable was used in.
     * 
     * @return The number of source files that this variable was used in.
     */
    public int getFiles() {
        return files;
    }
    
    /**
     * Increases the #ifdef count by one.
     */
    public void addIfdef() {
        ifdefs++;
    }
    
    /**
     * Increases the file count by one.
     */
    public void addFile() {
        files++;
    }
    
}
