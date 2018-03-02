package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
}
