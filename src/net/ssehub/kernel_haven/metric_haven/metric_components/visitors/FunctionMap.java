package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Container that stores all functions to easily detect whether an identifier is a function and to retrieve all
 * {@link CodeFunction}s for a given function name.
 * @author El-Sharkawy
 *
 */
public class FunctionMap {

    /**
     * Tuple of
     * <ul>
     *   <li>Unique name of function</li>
     *   <li>Locations of function (maybe there exist multiple definitions of the same function, e.g., in different
     *   CPP blocks.</li>
     * </ul>
     */
    private Map<String, List<CodeFunction>> functionMap;
    
    /**
     * Creates a new Function map that sores all existing functions.
     * @param functions The functions to store.
     */
    public FunctionMap(@NonNull Collection<CodeFunction> functions) {
        functionMap = new HashMap<>(functions.size());
        
        for (CodeFunction function: functions) {
            List<CodeFunction> sameFunctions = functionMap.get(function.getName());
            if (null == sameFunctions) {
                sameFunctions = new LinkedList<>();
                functionMap.put(function.getName(), sameFunctions);
            }
            sameFunctions.add(function);
        }
    }
    
    /**
     * Checks whether the given identifier is a defined function name.
     * @param identifier A token to check.
     * @return <tt>true</tt> if it is known to be a function, <tt>false</tt> otherwise.
     */
    public final boolean isFunction(String identifier) {
        return functionMap.containsKey(identifier);
    }
    
    /**
     * Returns the {@link CodeFunction} for the specified function.
     * @param functionName The name of the function for which the {@link CodeFunction} shall be returned.
     * @return The (parsed) {@link CodeFunction}s of the specified function(name).
     */
    public final @Nullable List<CodeFunction> getFunction(String functionName) {
        return functionMap.get(functionName);
    }
}
