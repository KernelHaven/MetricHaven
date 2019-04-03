/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement.Type;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement.CaseType;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
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
    private long variabilityCC;
    private boolean visitedStatement;
    private IVariableWeight weight;
    private VariableFinder varFinder;
    
    /**
     * Sole constructor for this class.
     * @param varModel  Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public McCabeVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
        classicCC = 1;
        variabilityCC = 1;
        varFinder = null;
    }
    
    /**
     * Should only be used if variation points shall be considered.
     * @param varModel  Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight A {@link IVariableWeight}to weight/measure the complexity of variation points.
     */
    public McCabeVisitor(@Nullable VariabilityModel varModel, @NonNull IVariableWeight weight) {
        this(varModel);
        // Small  optimization, do not discover variables if they are not required
        if (weight != NoWeight.INSTANCE) {
            this.weight = weight;
            varFinder = new VariableFinder();
        }
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        if (isFeatureDependentBlock(block) && block.getType() != CppBlock.Type.ELSE) {
            VariableFinder varFinder = this.varFinder;
            if (null != varFinder) {
                /* 
                 * If we use a varFinder, we are interested only in feature dependent code, i.e., check if symbols are
                 * defined in variability model.
                 * 
                 * block.getCondition() is not null because we already excluded else blocks above
                 */
                Set<Variable> usedVars = notNull(block.getCondition()).accept(varFinder);
                // Won't count blocks containing no variables
                for (Variable variable : usedVars) {
                    String varName = variable.getName();
                    if (isFeature(varName)) {
                        variabilityCC += weight.getWeight(varName, block.getSourceFile());
                    }
                }
                /*
                 * VarFinder returns internal set, clear method will also clear the external set as side-effect.
                 * Thus, arFinder.clear(); must be called AFTER the loop!
                 */
                varFinder.clear();
            } else {
                variabilityCC++;
            }
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
    public long getVariabilityCyclomaticComplexity() {
        return variabilityCC;
    }
    
    /**
     * Returns the sum of the two other complexities.
     * @return {@link #getClassicCyclomaticComplexity()} + {@link #getVariabilityCyclomaticComplexity()}.
     */
    public long getCombinedCyclomaticComplexity() {
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
