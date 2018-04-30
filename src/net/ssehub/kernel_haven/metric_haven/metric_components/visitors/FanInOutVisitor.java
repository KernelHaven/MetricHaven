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
import net.ssehub.kernel_haven.metric_haven.metric_components.FanInOutMetric.FanType;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measures classical globally/locally fan-in/fan-out metrics (function calls). 
 * @author El-Sharkawy
 *
 */
public class FanInOutVisitor extends AbstractFanInOutVisitor {

    private static final Logger LOGGER = Logger.get();
    
    /**
     * Data class to store a function call with it's presence condition.
     * @author El-Sharkawy
     *
     */
    private class FunctionCall {
        private String functionName;
        private Formula pc;
        
        /**
         * Sole constructor.
         * @param functionName The calling/called function name.
         * @param pc The presence condition of the call (use {@link True#INSTANCE} if conditions should
         *     not be considered.
         */
        private FunctionCall(String functionName, Formula pc) {
            this.functionName = functionName;
            this.pc = pc;
        }
        
        @Override
        public int hashCode() {
            return functionName.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            boolean isEqual = (other instanceof FunctionCall);
            if (isEqual) {
                FunctionCall otherCall = (FunctionCall) other;
                isEqual &= functionName.equals(otherCall.functionName);
                isEqual &= pc.equals(otherCall.pc);
            }
            
            return isEqual;
        }
    }
    
    private FanType type;
    private Map<String, Set<FunctionCall>> functionCalls;
    private @NonNull VariableFinder varFinder;
    private IVariableWeight weight;
    
    /**
     * Sole constructor for this class.
     * @param functions A list of all functions (necessary to identify callers and callees.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param type Specifies which kind of fan-in / fan-out shall be measured by this class.
     * @param weight Specifies a weight for discovered features, only relevant for degree centrality variations of this
     *     metric.
     */
    public FanInOutVisitor(@NonNull Collection<CodeFunction> functions,
        @Nullable VariabilityModel varModel, FanType type, IVariableWeight weight) {
        
        super(functions, varModel);
        this.type = type;
        functionCalls = new HashMap<>();
        varFinder = new VariableFinder();
        this.weight = weight;
    }
    
    /**
     * Returns the measured elements (depending on {@link #type} for the given function.
     * @param functionName Specification for which function the collected elements shall be returned.
     * @return The distinct set of callers/callees for the specified function.
     */
    private @NonNull Set<FunctionCall> getFunctionCalls(@NonNull String functionName) {
        Set<FunctionCall> calls = functionCalls.get(functionName);
        if (null == calls) {
            calls = new HashSet<>();
            functionCalls.put(functionName, calls);
        }
        
        return calls;
    }

    // CHECKSTYLE:OFF
    @Override
    protected void functionCall(@NonNull Function caller, @NonNull String callee, Formula pc) {
    // CHECKSTYLE:ON
        
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
        // CALLED functions for a specified function
        case CLASSICAL_FAN_OUT_GLOBALLY:
            // Measures (globally) the number of CALLED functions for a specified function
            getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE));
            break;
        case CLASSICAL_FAN_OUT_LOCALLY:
            // Measures (locally) the number of CALLED functions for a specified function
            if (isSameFile) {
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE));
            }
            break;
        case VP_FAN_OUT_GLOBALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc)) {
                // Measures (globally) the number of CALLED functions for a specified function
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE));
            }
            break;
        case VP_FAN_OUT_LOCALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc) && isSameFile) {
                // Measures (locally) the number of CALLED functions for a specified function
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE));
            }
            break;
        case DEGREE_CENTRALITY_OUT_GLOBALLY:
            // Measures (globally) the number of CALLED functions for a specified function
            getFunctionCalls(caller.getName()).add(new FunctionCall(callee, pc));
            break;
        case DEGREE_CENTRALITY_OUT_LOCALLY:
            // Measures (locally) the number of CALLED functions for a specified function
            if (isSameFile) {
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, pc));
            }
            break;
            
        // Functions CALLING the specified function
        case CLASSICAL_FAN_IN_GLOBALLY:
            // Measures (globally) the number of functions CALLING the specified function
            getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE));
            break;
        case CLASSICAL_FAN_IN_LOCALLY:
            // Measures (locally) the number of functions CALLING the specified function
            if (isSameFile) {
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE));
            }
            break;
        case VP_FAN_IN_GLOBALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc)) {
                // Measures (globally) the number of functions CALLING the specified function
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE));
            }
            break;
        case VP_FAN_IN_LOCALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc) && isSameFile) {
                // Measures (locally) the number of functions CALLING the specified function
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE));
            }
            break;
        case DEGREE_CENTRALITY_IN_GLOBALLY:
            // Measures (globally) the number of functions CALLING the specified function
            getFunctionCalls(callee).add(new FunctionCall(caller.getName(), pc));
            break;
        case DEGREE_CENTRALITY_IN_LOCALLY:
            // Measures (locally) the number of functions CALLING the specified function
            if (isSameFile) {
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), pc));
            }
            break;
        
        default:
            LOGGER.logError2("Unsupported operation ", type.name(), " for visitor ", getClass().getName());
            break;
        }
    }

    @Override
    public int getResult(@NonNull String functionName) {
        int result;
        if (type.isDegreeCentrality()) {
            // Compute the DC value for each connection
            result = 0;
            for (FunctionCall call : getFunctionCalls(functionName)) {
                call.pc.accept(varFinder);
                boolean containsFeature = false;
                for (Variable variable : varFinder.getVariables()) {
                    String varName = variable.getName();
                    /* By default weight will count unknown elements with 1, but this is already counted by 
                     * non-DegreeCentrality-metrics. Therefore, ensure that we count only feature of the varModel.
                     */
                    if (isFeature(varName)) {
                        result += weight.getWeight(variable.getName());
                        containsFeature = true;
                    }
                }
                if (containsFeature) {
                    result += 1;
                }
                varFinder.clear();
            }
        } else {
            result = getFunctionCalls(functionName).size();
        }
            
        return result;
    }

}
