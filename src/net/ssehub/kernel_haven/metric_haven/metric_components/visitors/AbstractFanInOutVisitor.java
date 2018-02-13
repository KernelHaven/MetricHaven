package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    
    private static Set<String> allFunctionNames;
    
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
        
        if (null == allFunctionNames) {
            synchronized (AbstractFanInOutVisitor.class) {
                /*
                 * Not 100% thread safe, on the other hand we want to fill the set with the same data,
                 * so it should not be a big issue
                 */
                if (null == allFunctionNames) {
                    Set<String> names = new HashSet<>();
                    for (CodeFunction function: functions) {
                        names.add(function.getName());
                    }
                    
                    allFunctionNames = Collections.unmodifiableSet(names);
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

    /**
     * Returns the result for the specified function, may only be called after the visitor was applied to <b>all</b>
     * functions.
     * @param functionName The function for which the result shall be returned.
     * @return The result for the specified function (&ge; 0).
     */
    public abstract int getResult(String functionName);
}
