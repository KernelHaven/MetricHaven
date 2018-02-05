package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.ElseStatement;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.code_model.ast.IfStructure;
import net.ssehub.kernel_haven.code_model.ast.Loop;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the number of (delivered) Lines of Code. This visitor supports
 * the following metrics:
 * <ul>
 *     <li>Lines of Code: {@link #getDLoC()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class LoCVisitor implements ISyntaxElementVisitor {
    
    private VariabilityModel varModel;
    
    private boolean isInCPP = false;
    private boolean isInFunction = false;

    private int nDLoC = 0;
    private int nLoF = 0;
    
    /**
     * Constructor which won't check {@link CppBlock#getCondition()} if they contain at least one known variable from
     * the variability model. Is the same as calling <tt>new LoCVisitor(null)</tt>.
     */
    public LoCVisitor() {
        this(null);
    }
    
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one know variable from the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public LoCVisitor(VariabilityModel varModel) {
        this.varModel = varModel;
    }

    @Override
    public void visitCppBlock(CppBlock block) {
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
    private boolean isFeatureDependentBlock(CppBlock block) {
        boolean isFeatureDependent = true;
        
        if (null != varModel && null != block.getCondition()) {
            isFeatureDependent = false;
            VariableFinder varFinder = new VariableFinder();
            varFinder.visit(block.getCondition());
            
            // Check whether at least 1 variable name is known by the variability model
            for (int i = varFinder.getVariableNames().size() - 1; i >= 0 && !isFeatureDependent; i--) {
                isFeatureDependent = (varModel.getVariableMap().containsKey(varFinder.getVariableNames().get(i)));
            }
            
        }
        
        return isFeatureDependent;
    }
    
    @Override
    public void visitFunction(Function function) {
        boolean oldState = isInFunction;
        isInFunction = true;
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitFunction(function);
        
        isInFunction = oldState;
    }
    
    @Override
    public void visitSingleStatement(SingleStatement statement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitCode(Code code) {
        // No action needed
    }
    
    @Override
    public void visitTypeDefinition(TypeDefinition typeDef) {
        // Count the typedef as one statement
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitTypeDefinition(typeDef);
    }

    // C Control structures
    
    @Override
    public void visitIfStructure(IfStructure ifStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitIfStructure(ifStatement);
    }
    
    @Override
    public void visitElseStatement(ElseStatement elseStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitElseStatement(elseStatement);
    }
    
    @Override
    public void visitSwitchStatement(SwitchStatement switchStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitCaseStatement(CaseStatement caseStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitCaseStatement(caseStatement);
    }
    
    @Override
    public void visitLoop(Loop loop) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitLoop(loop);
    }

    /**
     * Counts one line of code (only if currently a function is visited, does also consider c-preprocessor blocks).
     */
    private void count() {
        if (isInFunction) {
            nDLoC++;
            
            if (isInCPP) {
                nLoF++;
            }
        }
    }
    
    /**
     * Returns the number of (delivered) Lines of Code (dLoC). Treats all statements as a dLoC, since we do not
     * parse comments.
     * @return The number of statements (&ge; 0).
     */
    public int getDLoC() {
        return nDLoC;
    }
    
    /**
     * Returns the number of (delivered) <a href="https://dl.acm.org/citation.cfm?id=1806819">Lines of Feature code
     * (LoF)</a>.
     * Will treat all statements as a LoF, which a surrounded by an (additional) CppBlock, since we do not
     * parse comments.
     * @return The number of conditional statements (&ge; 0).
     */
    public int getLoF() {
        return nLoF;
    }
    
    /**
     * Returns the Percentage of (delivered) <a href="https://link.springer.com/article/10.1007/s10664-015-9360-1">
     * Lines of Feature code (PLoF)</a>.
     * @return {@link #getLoF()} / {@link #getDLoC()} or 0 (if {@link #getDLoC()} is 0).
     */
    public double getPLoF() {
        double basis = (double) getDLoC();
        return (basis != 0) ? (double) getLoF() / basis : 0.0d;
    }
}
