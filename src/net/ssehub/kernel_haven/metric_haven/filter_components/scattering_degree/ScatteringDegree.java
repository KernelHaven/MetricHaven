/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Stores for a {@link VariabilityVariable} in how many  variation points (&#35;ifdefs) and files it is used.
 * 
 * @author Adam
 */
@TableRow
public class ScatteringDegree {

    private @NonNull VariabilityVariable variable;
    
    private int ifdefs;
    
    private int files;
    
    /**
     * Creates an instance for the given {@link VariabilityVariable}.
     * 
     * @param variable The variable.
     */
    public ScatteringDegree(@NonNull VariabilityVariable variable) {
        this.variable = variable;
    }
    
    /**
     * Creates an instance with the given values.
     * 
     * @param variable The variable that this object stores the counts for.
     * @param ifdefs The number of ifdefs the variable occurs in.
     * @param files The number of files the variable occurs in.
     */
    ScatteringDegree(@NonNull VariabilityVariable variable, int ifdefs, int files) {
        this.variable = variable;
        this.ifdefs = ifdefs;
        this.files = files;
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
     * Returns the number of #ifdefs that this variable was used in.
     * 
     * @return The number of #ifdefs that this variable was used in.
     */
    @TableElement(index = 1, name = "#ifdef Count")
    public int getIfdefs() {
        return ifdefs;
    }
    
    /**
     * Returns the number of source files that this variable was used in.
     * 
     * @return The number of source files that this variable was used in.
     */
    @TableElement(index = 2, name = "File Count")
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
