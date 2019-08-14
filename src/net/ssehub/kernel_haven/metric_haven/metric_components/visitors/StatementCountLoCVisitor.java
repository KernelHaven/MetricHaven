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

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the number of Statements Count as derivation of LoC.
 * This visitor supports the following metrics:
 * <ul>
 *     <li>Statement Count of Code: {@link #getDLoC()}</li>
 *     <li>Statement Count of Feature code: {@link #getLoF()}</li>
 *     <li>Percentage of Statement Count of Feature code: {@link #getPLoF()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class StatementCountLoCVisitor extends AbstractFunctionVisitor {
    
    private int nDLoC = 0;
    private int nLoF = 0;
    
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one known variable of the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public StatementCountLoCVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
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

    // C Control structures
    
    @Override
    public void visitBranchStatement(@NonNull BranchStatement elseStatement) {
        count();
        
        // Continue visiting
        super.visitBranchStatement(elseStatement);
    }
    
    @Override
    public void visitSwitchStatement(@NonNull SwitchStatement switchStatement) {
        count();
        
        // Continue visiting
        super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitCaseStatement(@NonNull CaseStatement caseStatement) {
        count();
        
        // Continue visiting
        super.visitCaseStatement(caseStatement);
    }
    
    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        count();
        
        // Continue visiting
        super.visitLoopStatement(loop);
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        // Do not visit comments!
    }

    /**
     * Counts one line of code (only if currently a function is visited, does also consider c-preprocessor blocks).
     */
    protected void count() {
        if (isInFunction()) {
            nDLoC++;
            
            if (isInConditionalCode()) {
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
        double basis = getDLoC();
        return (basis != 0) ? getLoF() / basis : 0.0d;
    }

    @Override
    public void reset() {
        super.reset();
        nDLoC = 0;
        nLoF = 0;
    }
    
    /**
     * Ignore doubled code elements.
     * We count only the referenced element, which exists outside of the variation point.
     * Thus it is counted as LoC and not as LoF code.
     * This is done since the code is anyway part of the product and only some control structures around the code
     * are conditional.
     */
    @Override
    public void visitReference(@NonNull ReferenceElement referenceElement) { }
}
