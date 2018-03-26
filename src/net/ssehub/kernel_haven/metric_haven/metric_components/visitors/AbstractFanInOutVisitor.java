package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.Collection;
import java.util.List;

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
    

    private FunctionMap allFunctions;

//    private static int instanceCounter = 0;
//    private static boolean mapCreationComplete = false;
    
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
        allFunctions = new FunctionMap(functions);
//        instanceCounter++;
//        if (null == allFunctions) {
//            synchronized (AbstractFanInOutVisitor.class) {
//                /*
//                 * Not 100% thread safe, on the other hand we want to fill the set with the same data,
//                 * so it should not be a big issue
//                 * See for more details: https://www.javaworld.com/article/2073352/?page=2
//                 */
//                if (null == allFunctions) {
//                    mapCreationComplete = true;
//                }
//            }
//        }
    }
    
    /**
     * Checks whether the given identifier is a defined function name.
     * @param identifier A token to check.
     * @return <tt>true</tt> if it is known to be a function, <tt>false</tt> otherwise.
     */
    protected final boolean isFunction(String identifier) {
        return allFunctions.isFunction(identifier);
    }
    
    /**
     * Returns the {@link CodeFunction} for the specified function.
     * @param functionName The name of the function for which the {@link CodeFunction} shall be returned.
     * @return The (parsed) {@link CodeFunction}s of the specified function(name).
     */
    protected final @Nullable List<CodeFunction> getFunction(String functionName) {
        return allFunctions.getFunction(functionName);
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
//        while (!mapCreationComplete) {
//            Logger.get().logWarning2("Map creation not complete wait for 400ms...");
//            try {
//                Thread.sleep(400);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                Logger.get().logException("InterruptedException in " + getClass().getName(), e);
//            }
//        }
        
        
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
    
//    @Override
//    protected void finalize() throws Throwable {
//        try {
//            instanceCounter--;
//            
//            if (instanceCounter <= 0 && null != allFunctions) {
//                // Clear memory after last visitor was closed (if required, the maps will be re-build)
//                synchronized (AbstractFanInOutVisitor.class) {
//                    allFunctions = null;
//                }
//            }
//        } finally {
//            super.finalize();
//        }
//    }
}
