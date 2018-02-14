package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to define fan-in/fan-out metrics on functions.
 * @author El-Sharkawy
 *
 */
public abstract class AbstractFanInOutVisitor extends AbstractFunctionVisitor {
    
    /**
     * Currently, we measure approx. 275,000 distinct function names (depending on capabilities of used extractor). 
     */
    private static final int INITIAL_MAP_SIZE = 3 * 275000;
    
    private static Set<String> allFunctionNames;
    private static Map<String, CodeFunction> functionMap;
    private static int instanceCounter = 0;
    
    private @Nullable Function currentFunction;

    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code, if their formula contains at least one known variable of the variability model.
     * @param functions The list of all known functions.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    protected AbstractFanInOutVisitor(@NonNull Collection<CodeFunction> functions,
        @Nullable VariabilityModel varModel) {
        
        super(varModel);
        instanceCounter++;
        if (null == allFunctionNames) {
            synchronized (AbstractFanInOutVisitor.class) {
                /*
                 * Not 100% thread safe, on the other hand we want to fill the set with the same data,
                 * so it should not be a big issue
                 * See for more details: https://www.javaworld.com/article/2073352/?page=2
                 */
                if (null == allFunctionNames) {
                    allFunctionNames = new HashSet<>(INITIAL_MAP_SIZE);
                    functionMap = new HashMap<>(INITIAL_MAP_SIZE);
                    for (CodeFunction function: functions) {
                        allFunctionNames.add(function.getName());
                        functionMap.put(function.getName(), function);
                    }
                }
            }
        }
    }
    
    /**
     * Checks whether the given identifier is a defined function name.
     * @param identifier A token to check.
     * @return <tt>true</tt> if it is known to be a function, <tt>false</tt> otherwise.
     */
    protected final boolean isFunction(String identifier) {
        return allFunctionNames.contains(identifier);
    }
    
    /**
     * Returns the {@link CodeFunction} for the specified function.
     * @param functionName The name of the function for which the {@link CodeFunction} shall be returned.
     * @return The (parsed) {@link CodeFunction} of the specified function(name).
     */
    protected final @Nullable CodeFunction getFunction(String functionName) {
        return functionMap.get(functionName);
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
        String[] unparsedCodeFragments = code.getText().split(" ");
        for (int i = unparsedCodeFragments.length - 1; i >= 0; i--) {
            String unparsedCode = unparsedCodeFragments[i];
            if (null != unparsedCode && isFunction(unparsedCode) && null != currentFunction
                && !unparsedCode.equals(currentFunction.getName())) {
                
                functionCall(currentFunction, unparsedCode);
            }            
        }
    }

    /**
     * Informs an inherited visitors about the detection of a function call.
     * @param caller The calling function.
     * @param callee The called function (name).
     */
    protected abstract void functionCall(@NonNull Function caller, @NonNull String callee);
    
    /**
     * Returns the result for the specified function, may only be called after the visitor was applied to <b>all</b>
     * functions.
     * @param functionName The function for which the result shall be returned.
     * @return The result for the specified function (&ge; 0).
     */
    public abstract int getResult(@NonNull String functionName);
    
    @Override
    protected void finalize() throws Throwable {
        try {
            instanceCounter--;
            
            if (instanceCounter <= 0 && null != allFunctionNames) {
                // Clear memory after last visitor was closed (if required, the maps will be re-build)
                synchronized (AbstractFanInOutVisitor.class) {
                    allFunctionNames = null;
                    functionMap = null;
                }
            }
        } finally {
            super.finalize();
        }
    }
}
