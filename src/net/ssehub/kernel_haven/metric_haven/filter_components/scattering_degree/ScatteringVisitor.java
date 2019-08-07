/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Visitor part of the {@link VariabilityCounter} to count in how many &#35;ifdefs and files a
 * {@link VariabilityVariable} is used in. 
 *
 * @author El-Sharkawy
 * @author Adam
 */
class ScatteringVisitor implements ISyntaxElementVisitor, IVoidFormulaVisitor {
    
    private @NonNull Set<@NonNull String> variablesSeenInCurrentFile;   
    private @NonNull Set<@NonNull String> variablesSeenInCurrentIfdef;
    private @NonNull CountedVariables countedVariables;
    
    /**
     * Sole constructor.
     * @param countedVariables Shared instances to collect scattering degree values among multiple threads.
     */
    ScatteringVisitor(@NonNull CountedVariables countedVariables) {
        this.variablesSeenInCurrentFile = new HashSet<>();
        this.variablesSeenInCurrentIfdef = new HashSet<>();
        this.countedVariables = countedVariables;
    }
    
    /**
     * Resets the visitor, should be called after visiting a file if another file should be visited.
     */
    void reset() {
        variablesSeenInCurrentFile.clear();
    }

    /*
     * ISyntaxElementVisitor
     */
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        Formula condition = block.getCondition();
        if (condition != null) {
            visit(condition);
            variablesSeenInCurrentIfdef.clear();
        }
        
        // continue into this block
        ISyntaxElementVisitor.super.visitCppBlock(block);
    }
    
    /*
     * IVoidFormulaVisitor
     */

    @Override
    public void visitFalse(@NonNull False falseConstant) {
        // nothing to do
    }

    @Override
    public void visitTrue(@NonNull True trueConstant) {
        // nothing to do
    }

    @Override
    public void visitVariable(@NonNull Variable variable) {
        String varName = variable.getName();
        ScatteringDegree countedVar = countedVariables.getScatteringVariable(varName);
        
        if (countedVar != null) {
            varName = countedVar.getVariableName();
            
            if (!variablesSeenInCurrentIfdef.contains(varName)) {
                countedVar.addIfdef();
                variablesSeenInCurrentIfdef.add(varName);
            }
            
            if (!variablesSeenInCurrentFile.contains(varName)) {
                countedVar.addFile();
                variablesSeenInCurrentFile.add(varName);
            }
        }
    }

    @Override
    public void visitNegation(@NonNull Negation formula) {
        visit(formula.getFormula());
    }

    @Override
    public void visitDisjunction(@NonNull Disjunction formula) {
        visit(formula.getLeft());
        visit(formula.getRight());
    }

    @Override
    public void visitConjunction(@NonNull Conjunction formula) {
        visit(formula.getLeft());
        visit(formula.getRight());        
    }
}
