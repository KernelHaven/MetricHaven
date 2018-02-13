package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement.Type;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Determines the maximum and average nesting depth within a function.
 * @author El-Sharkawy
 *
 */
public class NestingDepthVisitor extends AbstractFunctionVisitor {
    private int nStatements;
    
    // Classical code parameters
    private Set<BranchStatement> visitedIFs = new HashSet<>();
    private int currentNestingDepth;
    private int maxDepth;
    private int allDepth;
    
    // Variability dependent code
    private int currentVPDepth;
    private int maxVPDepth;
    private int allVPDepth;
    
    /**
     * Sole constructor for this class.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public NestingDepthVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
        reset();
    }

    @Override
    public void visitBranchStatement(@NonNull BranchStatement branchStatement) {
        // IF, ELSE IF, and ELSE are also nested inside top level IF (as siblings) -> avoid double counting
        boolean count = isInFunction() && branchStatement.getType() == Type.IF && !visitedIFs.contains(branchStatement);
        if (count) {
            currentNestingDepth++;
        }
        
        super.visitBranchStatement(branchStatement);
        
        if (count) {
            currentNestingDepth--;
        }
    }
    
    @Override
    public void visitSwitchStatement(@NonNull SwitchStatement switchStatement) {
        if (isInFunction()) {
            currentNestingDepth++;
        }
        
        super.visitSwitchStatement(switchStatement);
        
        if (isInFunction()) {
            currentNestingDepth--;
        }
    }
    
    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        if (isInFunction()) {
            currentNestingDepth++;
        }
        
        super.visitLoopStatement(loop);
        
        if (isInFunction()) {
            currentNestingDepth--;
        }
        
    }
    
    @Override
    public void visitSingleStatement(@NonNull SingleStatement statement) {
        count();
        
        // Continue visiting
        super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(@NonNull TypeDefinition typeDef) {
        // Count the typedef as one statement
        count();
        
        // Continue visiting
        super.visitTypeDefinition(typeDef);
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        // Do not visit comments!
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        // Compute only once (in this child class)
        boolean isVariationPoint = isFeatureDependentBlock(block); 
        if (isVariationPoint) {
            currentVPDepth++;
        }
        
        super.visitCppBlock(block);
        
        if (isVariationPoint) {
            currentVPDepth--;
        }
    }
    
    @Override
    public void reset() {
        super.reset();
        visitedIFs.clear();
        nStatements = 0;

        // Classical code
        currentNestingDepth = 1;
        maxDepth = currentNestingDepth;
        allDepth = 0;
        
        // Variation Points
        currentVPDepth = 0; // Consider code not be dependent of variability
        maxVPDepth = currentVPDepth;
        allVPDepth = 0;
    }
    
    /**
     * Counts statements.
     */
    private void count() {
        if (isInFunction()) {
            // Classical depth
            maxDepth = Math.max(maxDepth, currentNestingDepth);
            allDepth += currentNestingDepth;
            nStatements++;
            
            // Count statements of conditional code (only if current statement is conditional)
            if (isInConditionalCode()) {
                maxVPDepth = Math.max(maxVPDepth, currentVPDepth);
                allVPDepth += currentVPDepth;
            }
        }
    }
    
    /**
     * Returns the average/maximum nesting depth of statements with respect to classical branching elements
     * (if, loops, switch).
     * 
     * @param max <tt>true</tt> to compute maximum nesting depth, <tt>false</tt> to compute average nesting depth.
     * @return The maximum (&ge; 1) / average (&ge; 0) nesting depth of statements.
     */
    public double getClassicalNestingDepth(boolean max) {
        double result;
        if (max) {
            result = maxDepth;
        } else {
            result = (0 != nStatements) ? (double) allDepth / nStatements : 0;
        }
        
        return result;
    }
    
    /**
     * Returns the average/maximum nesting depth of statements with respect to variation points.
     * 
     * @param max <tt>true</tt> to compute maximum nesting depth, <tt>false</tt> to compute average nesting depth.
     * @return The maximum (&ge; 1) / average (&ge; 0) nesting depth of statements.
     */
    public double getVariationPointNestingDepth(boolean max) {
        double result;
        if (max) {
            result = maxVPDepth;
        } else {
            result = (0 != nStatements) ? (double) allVPDepth / nStatements : 0;
        }
        
        return result;
    }
}
