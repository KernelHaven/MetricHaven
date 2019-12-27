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
import net.ssehub.kernel_haven.cpp_utils.non_boolean.INonBooleanFormulaVisitor;
import net.ssehub.kernel_haven.cpp_utils.non_boolean.Literal;
import net.ssehub.kernel_haven.cpp_utils.non_boolean.Macro;
import net.ssehub.kernel_haven.cpp_utils.non_boolean.NonBooleanOperator;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
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
class ScatteringVisitor implements ISyntaxElementVisitor, INonBooleanFormulaVisitor<Void> {
    
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
    public Void visitFalse(@NonNull False falseConstant) {
        /* No action needed */
        return null;
    }

    @Override
    public Void visitTrue(@NonNull True trueConstant) {
        /* No action needed */
        return null;
    }

    @Override
    public Void visitVariable(@NonNull Variable variable) {
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
        
        return null;
    }

    @Override
    public Void visitNegation(@NonNull Negation formula) {
        visit(formula.getFormula());
        
        return null;
    }

    @Override
    public Void visitDisjunction(@NonNull Disjunction formula) {
        visit(formula.getLeft());
        visit(formula.getRight());
        
        return null;
    }

    @Override
    public Void visitConjunction(@NonNull Conjunction formula) {
        visit(formula.getLeft());
        visit(formula.getRight());
        
        return null;
    }

    @Override
    public Void visitNonBooleanOperator(NonBooleanOperator operator) {
        operator.getLeft().accept(this);
        operator.getRight().accept(this);
        return null;
    }

    @Override
    public Void visitLiteral(Literal literal) {
        /* No action needed */
        return null;
    }

    @Override
    public Void visitMacro(Macro macro) {
        Formula argument = macro.getArgument();
        if (null != argument) {
            argument.accept(this);
        }
        return null;
    }
}
