package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measures classical globally/locally fan-in/fan-out metrics (function calls). 
 * @author El-Sharkawy
 *
 */
public class ClassicalFanInOutVisitor extends AbstractFanInOutVisitor {

    private static final Logger LOGGER = Logger.get();
    
    /**
     * Specification which kind of fan-in/fan-out shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum MeasurementType {
        FAN_OUT_LOCALLY, FAN_OUT_GLOBALLY,
        FAN_IN_LOCALLY, FAN_IN_GLOBALLY;
    }
    
    private MeasurementType type;
    private Map<String, Set<String>> functionCalls;
    
    /**
     * Sole constructor for this class.
     * @param functions A list of all functions (necessary to identify callers and callees.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param type Specifies which kind of fan-in / fan-out shall be measured by this class.
     */
    public ClassicalFanInOutVisitor(@NonNull Collection<CodeFunction> functions,
        @Nullable VariabilityModel varModel, MeasurementType type) {
        
        super(functions, varModel);
        this.type = type;
        functionCalls = new HashMap<>();
    }
    
    /**
     * Returns the measured elements (depending on {@link #type} for the given function.
     * @param functionName Specification for which function the collected elements shall be returned.
     * @return The distinct set of callers/callees for the specified function.
     */
    private @NonNull Set<String> getFunctionCalls(@NonNull String functionName) {
        Set<String> calls = functionCalls.get(functionName);
        if (null == calls) {
            calls = new HashSet<>();
            functionCalls.put(functionName, calls);
        }
        
        return calls;
    }

    @Override
    protected void functionCall(@NonNull Function caller, @NonNull String callee) {
        List<CodeFunction> others = getFunction(callee);
        boolean isSameFile = false;
        
        if (null != others) {
            for (CodeFunction otherFunction : others) {
                File calleFile = otherFunction != null ? otherFunction.getSourceFile().getPath() : null;
                isSameFile = (null != calleFile && calleFile.equals(caller.getSourceFile()));
                if (isSameFile) {
                    break;
                }
            }
        }
        
        switch (type) {
        case FAN_OUT_GLOBALLY:
            // Measures (globally) the number of CALLED functions for a specified function
            getFunctionCalls(caller.getName()).add(callee);
            break;
        case FAN_OUT_LOCALLY:
            // Measures (locally) the number of CALLED functions for a specified function
            if (isSameFile) {
                getFunctionCalls(caller.getName()).add(callee);
            }
            break;
        case FAN_IN_GLOBALLY:
            // Measures (globally) the number of functions CALLING the specified function
            getFunctionCalls(callee).add(caller.getName());
            break;
        case FAN_IN_LOCALLY:
            // Measures (locally) the number of functions CALLING the specified function
            if (isSameFile) {
                getFunctionCalls(callee).add(caller.getName());
            }
            break;
        default:
            LOGGER.logError2("Unsupported operation ", type.name(), " for visitor ", getClass().getName());
            break;
        }
    }

    @Override
    public int getResult(@NonNull String functionName) {
        return getFunctionCalls(functionName).size();
    }

}
