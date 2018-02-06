package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.ElseStatement;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.IfStructure;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the number of (delivered) Lines of Code. This visitor supports
 * the following metrics:
 * <ul>
 *     <li>Lines of Code: {@link #getDLoC()}</li>
 *     <li>Lines of Feature code: {@link #getLoF()}</li>
 *     <li>Percentage of Lines of Feature code: {@link #getPLoF()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class LoCVisitor extends AbstractFunctionVisitor {
    
    private int nDLoC = 0;
    private int nLoF = 0;
    
    /**
     * Constructor which won't check {@link CppBlock#getCondition()} whether they contain at least one known variable of
     * the variability model. Is the same as calling <tt>new LoCVisitor(null)</tt>.
     */
    public LoCVisitor() {
        this(null);
    }
    
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one known variable of the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public LoCVisitor(VariabilityModel varModel) {
        super(varModel);
    }
    
    @Override
    public void visitSingleStatement(SingleStatement statement) {
        count();
        
        // Continue visiting
        super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(TypeDefinition typeDef) {
        // Count the typedef as one statement
        count();
        
        // Continue visiting
        super.visitTypeDefinition(typeDef);
    }

    // C Control structures
    
    @Override
    public void visitIfStructure(IfStructure ifStatement) {
        count();
        
        // Continue visiting
        super.visitIfStructure(ifStatement);
    }
    
    @Override
    public void visitElseStatement(ElseStatement elseStatement) {
        count();
        
        // Continue visiting
        super.visitElseStatement(elseStatement);
    }
    
    @Override
    public void visitSwitchStatement(SwitchStatement switchStatement) {
        count();
        
        // Continue visiting
        super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitCaseStatement(CaseStatement caseStatement) {
        count();
        
        // Continue visiting
        super.visitCaseStatement(caseStatement);
    }
    
    @Override
    public void visitLoopStatement(LoopStatement loop) {
        count();
        
        // Continue visiting
        super.visitLoopStatement(loop);
    }
    
    @Override
    public void visitComment(Comment comment) {
        // Do not visit comments!
    }

    /**
     * Counts one line of code (only if currently a function is visited, does also consider c-preprocessor blocks).
     */
    private void count() {
        if (isInFunction()) {
            nDLoC++;
            
            if (isinConditionalCode()) {
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