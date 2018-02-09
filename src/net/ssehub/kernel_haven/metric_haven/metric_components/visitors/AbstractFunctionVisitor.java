package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import net.ssehub.kernel_haven.code_model.ast.Code;
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
abstract class AbstractFunctionVisitor implements ISyntaxElementVisitor {
    
    private @Nullable VariabilityModel varModel;
    
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
        
        VariabilityModel varModel = this.varModel;
        Formula condition = block.getCondition();
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
    protected boolean isinConditionalCode() {
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
    
    @Override
    public void visitCode(@NonNull Code code) {
        // No action needed by default for visiting code elements
    }
    
    /**
     * Resets this visitor so that it an be reused without re-instantiation.
     */
    public void reset() {
        isInCPP = false;
        isInFunction = false;
    }

}
