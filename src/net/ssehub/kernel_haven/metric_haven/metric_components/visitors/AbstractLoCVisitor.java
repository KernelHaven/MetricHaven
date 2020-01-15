/*
 * Copyright 2020 University of Hildesheim, Software Systems Engineering
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

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * 
 * Super class for Statement- and LoC-counting visitors.
 * @author El-Sharkawy
 */
public abstract class AbstractLoCVisitor extends AbstractFunctionVisitor {

    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one known variable of the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    protected AbstractLoCVisitor(VariabilityModel varModel) {
        super(varModel);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Returns the number of Lines of Code (LoC) / statements.
     * @return The number of statements / LoC (&ge; 0).
     */
    public abstract int getLoC();
    
    /**
     * Returns the number of Lines of Feature-Code (LoF) / statements, which are in a variation point.
     * @return The number of Feature-Code (LoF) / statements / LoC (&ge; 0).
     */
    public abstract int getLoF();
    
    /**
     * Returns the number of Lines of Code (LoC) / statements.
     * @return The number of statements / LoC (&ge; 0).
     */
    public abstract int getLoCComments();
   
    /**
     * Returns the number of Lines of Feature-Code (LoF) / statements, which are in a variation point.
     * @return The number of Feature-Code (LoF) / statements / LoC (&ge; 0).
     */
    public abstract int getLoFComments();

    /**
     * Returns the Percentage of <a href="https://link.springer.com/article/10.1007/s10664-015-9360-1">
     * Lines of Feature code (PLoF)</a>.
     * @return {@link #getLoF()} / {@link #getLoC()} or 0 (if {@link #getLoC()} is 0).
     */
    public double getPLoF() {
        double basis = getLoC();
        return (basis != 0) ? getLoF() / basis : 0.0d;
    }
    
    /**
     * Returns the percentage of comments with respect to all statements / lines in scope.
     * @return The relative amount of comments (&ge; 0).
     */
    public abstract double getLoCCommentRatio();
    
    /**
     * Returns the percentage of comments with respect to feature statements / lines in scope.
     * @return The relative amount of comments (&ge; 0).
     */
    public abstract double getLoFCommentRatio();
    
    /**
     * Ignore doubled code elements.
     * We count only the referenced element, which exists outside of the variation point.
     * Thus it is counted as LoC and not as LoF code.
     * This is done since the code is anyway part of the product and only some control structures around the code
     * are conditional.
     */
    @Override
    public void visitReference(@NonNull ReferenceElement referenceElement) { }
    
    @Override
    public void visitSingleStatement(@NonNull SingleStatement statement) {
        countStatement();
        
        // Continue visiting
        super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(@NonNull TypeDefinition typeDef) {
        // Count the typedef as one statement
        countStatement();
        
        // Continue visiting
        super.visitTypeDefinition(typeDef);
    }

    // C Control structures
    
    @Override
    public void visitBranchStatement(@NonNull BranchStatement elseStatement) {
        countStatement();
        
        // Continue visiting
        super.visitBranchStatement(elseStatement);
    }
    
    @Override
    public void visitSwitchStatement(@NonNull SwitchStatement switchStatement) {
        countStatement();
        
        // Continue visiting
        super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitCaseStatement(@NonNull CaseStatement caseStatement) {
        countStatement();
        
        // Continue visiting
        super.visitCaseStatement(caseStatement);
    }
    
    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        countStatement();
        
        // Continue visiting
        super.visitLoopStatement(loop);
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        countComment();
    }
    
    /**
     * Will be called by each countable statement.
     * Relevant for statement-count metrics.
     */
    protected abstract void countStatement();
    
    /**
     * Will be called by each countable comment.
     */
    protected abstract void countComment();
}
