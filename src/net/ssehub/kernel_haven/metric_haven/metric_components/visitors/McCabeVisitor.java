package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement.Type;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement.CaseType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the cyclomatic complexity. This visitor supports the following
 * metrics:
 * <ul>
 *     <li><a href="https://ieeexplore.ieee.org/document/1702388">McCabe</a>'s complexity:
 *         {@link #getClassicCyclomaticComplexity()}</li>
 *     <li><a href="https://pdfs.semanticscholar.org/d5ce/44ac8717a0f51bb869529398c9a20874ad0b.pdf">Lopez-Herrejon</a>'s
 *         complexity: {@link #getVariabilityCyclomaticComplexity()}</li>
 *     <li>The sum of both cyclomatic complexities: {@link #getCombinedCyclomaticComplexity()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class McCabeVisitor extends AbstractFunctionVisitor {

    private int classicCC;
    private int variabilityCC;
    private boolean visitedStatement;
    
    /**
     * Sole constructor for this class.
     * @param varModel  Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public McCabeVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
        classicCC = 1;
        variabilityCC = 1;
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        if (isFeatureDependentBlock(block)) {
            variabilityCC++;
        }
        
        super.visitCppBlock(block);
    }

    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        if (isInFunction()) {
            classicCC++;
        }
        
        super.visitLoopStatement(loop);
    }
    
    @Override
    public void visitBranchStatement(@NonNull BranchStatement branchStatement) {
        if (isInFunction() && branchStatement.getType() != Type.ELSE) {
            classicCC++;
        }
        
        super.visitBranchStatement(branchStatement);
    }
    
    @Override
    public void visitCaseStatement(@NonNull CaseStatement caseStatement) {
        // Do not count default blocks or empty case statements as they do not add a new edge to the graph
        boolean caseStementFound = (isInFunction() && caseStatement.getType() != CaseType.DEFAULT) ? true : false;

        boolean tmpState = visitedStatement;
        visitedStatement = false;
        super.visitCaseStatement(caseStatement);
        boolean nonEmptyCase = visitedStatement;
        visitedStatement = tmpState;
        
        
        if (caseStementFound && nonEmptyCase) {
            classicCC++;
        }
    }
    
    /**
     * Returns the classical cyclomatic complexity as defined by
     * <a href="https://ieeexplore.ieee.org/document/1702388">McCabe</a>.
     * @return McCabe's cyclomatic complexity (&ge; 0).
     */
    public int getClassicCyclomaticComplexity() {
        return classicCC;
    }
    
    /**
     * Returns the cyclomatic complexity defined one variability points as defined by
     * <a href="https://pdfs.semanticscholar.org/d5ce/44ac8717a0f51bb869529398c9a20874ad0b.pdf">Lopez-Herrejon</a>.
     * @return Cyclomatic complexity of preprocessor directives (&ge; 0).
     */
    public int getVariabilityCyclomaticComplexity() {
        return variabilityCC;
    }
    
    /**
     * Returns the sum of the two other complexities.
     * @return {@link #getClassicCyclomaticComplexity()} + {@link #getVariabilityCyclomaticComplexity()}.
     */
    public int getCombinedCyclomaticComplexity() {
        return getClassicCyclomaticComplexity() + getVariabilityCyclomaticComplexity();
    }
    
    @Override
    public void visitSingleStatement(@NonNull SingleStatement statement) {
        visitedStatement = isInFunction();
        super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(@NonNull TypeDefinition typeDef) {
        visitedStatement = isInFunction();
        super.visitTypeDefinition(typeDef);
    }

    @Override
    public void reset() {
        super.reset();
        classicCC = 1;
        variabilityCC = 1;
        visitedStatement = false;
    }
}
