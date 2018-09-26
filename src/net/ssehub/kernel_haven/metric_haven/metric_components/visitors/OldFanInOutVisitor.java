package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut.FanType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
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
@Deprecated
public class OldFanInOutVisitor extends AbstractFunctionVisitor {

    private static final Logger LOGGER = Logger.get();
    
    /**
     * Data class to store a function call with it's presence condition.
     * @author El-Sharkawy
     *
     */
    private class FunctionCall {
        private String functionName;
        private Formula pc;
        private File codeFile;
        
        /**
         * Sole constructor.
         * @param functionName The calling/called function name.
         * @param pc The presence condition of the call (use {@link True#INSTANCE} if conditions should
         *     not be considered.
         * @param codefile The location of the function call.
         */
        private FunctionCall(String functionName, Formula pc, File codefile) {
            this.functionName = functionName;
            this.pc = pc;
            this.codeFile = codefile;
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
                isEqual &= codeFile.equals(otherCall.codeFile);
            }
            
            return isEqual;
        }
    }
    
    private FanInOut.FanType type;
    private Map<String, Set<FunctionCall>> functionCalls;
    private @NonNull VariableFinder varFinder;
    private IVariableWeight weight;
    
    private @NonNull FunctionMap allFunctions;
    
    private @Nullable Function currentFunction;
    
    /**
     * Preferred Constructor, allows sharing of function map.
     * @param functions A list of all functions (necessary to identify callers and callees.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param type Specifies which kind of fan-in / fan-out shall be measured by this class.
     * @param weight Specifies a weight for discovered features, only relevant for degree centrality variations of this
     *     metric.
     */
    public OldFanInOutVisitor(@NonNull FunctionMap functions,
        @Nullable VariabilityModel varModel, FanInOut.FanType type, IVariableWeight weight) {
        
        super(varModel);
        allFunctions = functions;
        this.type = type;
        functionCalls = new HashMap<>();
        varFinder = new VariableFinder();
        this.weight = weight;
        

        throw new UnsupportedOperationException();
    }
    
    /**
     * Dummy. Always throws.
     * @param functions .
     * @param varModel .
     * @param type2 .
     * @param weight2 .
     */
    public OldFanInOutVisitor(@NonNull List<CodeFunction> functions, @Nullable VariabilityModel varModel,
            @NonNull FanType type2, IVariableWeight weight2) {
        super(varModel);
        throw new UnsupportedOperationException();
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

    /**
     * Informs an inherited visitors about the detection of a function call.
     * @param caller The calling function.
     * @param callee The called function (name).
     * @param pc The presence condition of the function call.
     */
    // CHECKSTYLE:OFF
    protected void functionCall(@NonNull Function caller, @NonNull String callee, Formula pc) {
    // CHECKSTYLE:ON
        
        List<File> others = getFunction(callee);
        boolean isSameFile = false;
        
        File calleFile = null;
        File callerFile = caller.getSourceFile();
        if (null != others) {
            for (File otherFunction : others) {
                calleFile = otherFunction;
                isSameFile = (null != calleFile && calleFile.equals(callerFile));
                if (isSameFile) {
                    break;
                }
            }
        }
        
        switch (type) {
        // CALLED functions for a specified function
        case CLASSICAL_FAN_OUT_GLOBALLY:
            // Measures (globally) the number of CALLED functions for a specified function
            getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE, calleFile));
            break;
        case CLASSICAL_FAN_OUT_LOCALLY:
            // Measures (locally) the number of CALLED functions for a specified function
            if (isSameFile) {
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE, calleFile));
            }
            break;
        case VP_FAN_OUT_GLOBALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc)) {
                // Measures (globally) the number of CALLED functions for a specified function
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE, calleFile));
            }
            break;
        case VP_FAN_OUT_LOCALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc) && isSameFile) {
                // Measures (locally) the number of CALLED functions for a specified function
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, True.INSTANCE, calleFile));
            }
            break;
        case DEGREE_CENTRALITY_OUT_GLOBALLY:
            // Measures (globally) the number of CALLED functions for a specified function
            getFunctionCalls(caller.getName()).add(new FunctionCall(callee, pc, calleFile));
            break;
        case DEGREE_CENTRALITY_OUT_LOCALLY:
            // Measures (locally) the number of CALLED functions for a specified function
            if (isSameFile) {
                getFunctionCalls(caller.getName()).add(new FunctionCall(callee, pc, calleFile));
            }
            break;
            
        // Functions CALLING the specified function
        case CLASSICAL_FAN_IN_GLOBALLY:
            // Measures (globally) the number of functions CALLING the specified function
            getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE, callerFile));
            break;
        case CLASSICAL_FAN_IN_LOCALLY:
            // Measures (locally) the number of functions CALLING the specified function
            if (isSameFile) {
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE, callerFile));
            }
            break;
        case VP_FAN_IN_GLOBALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc)) {
                // Measures (globally) the number of functions CALLING the specified function
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE, callerFile));
            }
            break;
        case VP_FAN_IN_LOCALLY:
            if (!getCurrentfunction().getPresenceCondition().equals(pc) && isSameFile) {
                // Measures (locally) the number of functions CALLING the specified function
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), True.INSTANCE, callerFile));
            }
            break;
        case DEGREE_CENTRALITY_IN_GLOBALLY:
            // Measures (globally) the number of functions CALLING the specified function
            getFunctionCalls(callee).add(new FunctionCall(caller.getName(), pc, callerFile));
            break;
        case DEGREE_CENTRALITY_IN_LOCALLY:
            // Measures (locally) the number of functions CALLING the specified function
            if (isSameFile) {
                getFunctionCalls(callee).add(new FunctionCall(caller.getName(), pc, callerFile));
            }
            break;
        
        default:
            LOGGER.logError2("Unsupported operation ", type.name(), " for visitor ", getClass().getName());
            break;
        }
    }

    /**
     * Returns the result for the specified function, may only be called after the visitor was applied to <b>all</b>
     * functions.
     * @param functionName The function for which the result shall be returned.
     * @return The result for the specified function (&ge; 0).
     */
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
                        if (null != call.codeFile) {
                            result += weight.getWeight(variable.getName(), call.codeFile);
                        } else {
                            result += weight.getWeight(variable.getName());
                        }
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
    
    /**
     * Checks whether the given identifier is a defined function name.
     * @param identifier A token to check.
     * @return <tt>true</tt> if it is known to be a function, <tt>false</tt> otherwise.
     */
    protected final boolean isFunction(String identifier) {
//        return allFunctions.isFunction(identifier);
        return false;
    }
    
    /**
     * Returns the source locations for the specified function.
     * @param functionName The name of the function for which the {@link CodeFunction} shall be returned.
     * @return The (locations) of the specified function(name).
     */
    protected final @Nullable List<File> getFunction(String functionName) {
//        return allFunctions.getFunction(functionName);
        return null;
    }
    
    /**
     * Returns the current visited function.
     * @return the current visited function, may be <tt>null</tt>.
     */
    protected @Nullable Function getCurrentfunction() {
        return currentFunction;
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        Function previousFunction = this.currentFunction;
        this.currentFunction = function;
        
        super.visitFunction(function);
        
        this.currentFunction = previousFunction;
    }
    
    @Override
    public void visitCode(@NonNull Code code) {
        Function currentFunction = this.currentFunction;
        
        String[] unparsedCodeFragments = code.getText().split(" ");
        for (int i = unparsedCodeFragments.length - 1; i >= 0; i--) {
            String unparsedCode = unparsedCodeFragments[i];
            if (null != unparsedCode && isFunction(unparsedCode) && null != currentFunction
                && !unparsedCode.equals(currentFunction.getName())) {
                
                functionCall(currentFunction, unparsedCode, code.getPresenceCondition());
            }            
        }
    }

}
