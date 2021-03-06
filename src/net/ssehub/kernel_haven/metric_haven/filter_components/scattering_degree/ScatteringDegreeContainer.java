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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Stores for all variables of the variability model scattering degree metrics. More precisely:
 * <ul>
 *   <li><a href="https://dl.acm.org/citation.cfm?id=2556641">SD for variation points</a></li>
 *   <li><a href="https://dl.acm.org/citation.cfm?id=2491645">SD for files</a></li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class ScatteringDegreeContainer {
    private static final int MISSING_VALUE = 0;

    private Map<String, ScatteringDegree> variables;
    
    /**
     * Sole constructor for this class.
     * @param sdValues The gathered scattering degree values for all variables.
     */
    public ScatteringDegreeContainer(Collection<ScatteringDegree> sdValues) {
        variables = new HashMap<String, ScatteringDegree>(sdValues.size());
        
        for (ScatteringDegree scatteringDegree : sdValues) {
            variables.put(scatteringDegree.getVariable().getName(), scatteringDegree);
        }
    }
    
    /**
     * Returns SD<sub>VP</sub> (scattering degree of variation points) for the given variable.
     * @param varName The name of the variable for which the scattering degree shall be returned.
     * @return Returns the SD value or {@value #MISSING_VALUE} if no SD values exist for the specified variable.
     */
    public int getSDVariationPoint(String varName) {
        return variables.containsKey(varName) ? variables.get(varName).getIfdefs() : MISSING_VALUE;
    }
    
    /**
     * Returns SD<sub>File</sub> (scattering degree of files) for the given variable.
     * @param varName The name of the variable for which the scattering degree shall be returned.
     * @return Returns the SD value or {@value #MISSING_VALUE} if no SD values exist for the specified variable.
     */
    public int getSDFile(String varName) {
        return variables.containsKey(varName) ? variables.get(varName).getFiles() : MISSING_VALUE;
    }
    
    /**
     * Returns the number of handled variables.
     * @return The number variables for which SD values are collected.
     */
    public int getSize() {
        return variables.size();
    }
    
    /**
     * Returns an iterator over all {@link ScatteringDegree}s in this collection.
     * 
     * @return An iterator over all {@link ScatteringDegree}s.
     */
    @NonNull Iterator<ScatteringDegree> getScatteringDegreeIterator() {
        return notNull(variables.values().iterator());
    }
    
}
