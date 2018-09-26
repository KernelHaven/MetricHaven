package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Abstract visitor to create visitors to visiting {@link Function}s.
 * @author El-Sharkawy
 *
 */
public abstract class AbstractFunctionVisitor implements ISyntaxElementVisitor {
    
    private final @Nullable VariabilityModel varModel;
    
    private boolean isInCPP = false;
    private boolean isInFunction = false;
    
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code, if their formula contains at least one known variable of the variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    protected AbstractFunctionVisitor(@Nullable VariabilityModel varModel) {
        this.varModel = varModel;
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        boolean oldState = isInCPP;
        isInCPP = isInCPP || isFeatureDependentBlock(block);
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitCppBlock(block);
        
        isInCPP = oldState;
    }
    
    /**
     * Checks if a {@link CppBlock} should be treated as feature-dependent code. 
     * @param block The block to check.
     * @return <tt>true</tt> if it contains at least one variable from the variability model or if no variability model
     *     was passed to the constructor, <tt>false</tt> otherwise.
     */
    protected boolean isFeatureDependentBlock(@NonNull CppBlock block) {
        boolean isFeatureDependent = true;
        
        Formula condition = block.getCondition();
        VariabilityModel varModel = this.varModel;
        if (null != varModel && null != condition) {
            isFeatureDependent = false;
            VariableFinder varFinder = new VariableFinder();
            varFinder.visit(condition);
            
            // Check whether at least 1 variable name is known by the variability model
            for (int i = varFinder.getVariableNames().size() - 1; i >= 0 && !isFeatureDependent; i--) {
                isFeatureDependent = (varModel.getVariableMap().containsKey(varFinder.getVariableNames().get(i)));
            }
            
        }
        
        return isFeatureDependent;
    }
    
    /**
     * Checks if the given name is defined in the variability model.
     * @param variableName The CPP element to check.
     * @return <tt>true</tt> if no variability model was passed to this visitor or if the element is defined in the
     *     variability model.
     */
    protected boolean isFeature(String variableName) {
        return (null == varModel || notNull(varModel).getVariableMap().containsKey(variableName));
    }
    
    /**
     * Returns if the visitor is currently inside a function.
     * @return <tt>true</tt> if inside a function, <tt>false</tt> otherwise
     */
    protected boolean isInFunction() {
        return isInFunction;
    }
    
    /**
     * Returns if the visitor is currently inside a {@link CppBlock}, which is dependent on variables of
     * the variability model.
     * @return <tt>true</tt> if inside a {@link CppBlock} (dependent on a variability variables),
     *     <tt>false</tt> otherwise
     */
    protected boolean isInConditionalCode() {
        return isInCPP;
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        boolean oldState = isInFunction;
        isInFunction = true;
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitFunction(function);
        
        isInFunction = oldState;
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        // Do not visit comments!
    }
    
    /**
     * Resets this visitor so that it an be reused without re-instantiation.
     */
    public void reset() {
        isInCPP = false;
        isInFunction = false;
    }

}
