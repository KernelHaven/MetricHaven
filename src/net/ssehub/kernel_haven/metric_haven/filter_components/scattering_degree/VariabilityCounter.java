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
package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Counts in how many &#35;ifdefs and files a {@link VariabilityVariable} is used in.
 * 
 * @author Adam
 */
public class VariabilityCounter extends AnalysisComponent<ScatteringDegreeContainer> implements ISyntaxElementVisitor,
        IVoidFormulaVisitor {

    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    
    private @NonNull AnalysisComponent<SourceFile<?>> cmProvider;
    
    private @NonNull Map<@NonNull String, ScatteringDegree> countedVariables;
    
    private @NonNull Set<@NonNull String> variablesSeenInCurrentFile;
    
    private @NonNull Set<@NonNull String> variablesSeenInCurrentIfdef;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param vmProvider The component to get the variability model from.
     * @param cmProvider The component to get the code model from.
     */
    public VariabilityCounter(@NonNull Configuration config, @NonNull AnalysisComponent<VariabilityModel> vmProvider,
            @NonNull AnalysisComponent<SourceFile<?>> cmProvider) {
        super(config);
        
        this.vmProvider = vmProvider;
        this.cmProvider = cmProvider;
        this.countedVariables = new HashMap<>();
        this.variablesSeenInCurrentFile = new HashSet<>();
        this.variablesSeenInCurrentIfdef = new HashSet<>();
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = vmProvider.getNextResult();
        if (varModel == null) {
            LOGGER.logError("Did not get a variability model", "Can't create any results");
            return;
        }
        
        for (VariabilityVariable variable : varModel.getVariables()) {
            countedVariables.put(variable.getName(), new ScatteringDegree(variable));
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        while ((file = cmProvider.getNextResult()) != null) {
            
            for (ISyntaxElement element : file.castTo(ISyntaxElement.class)) {
                element.accept(this);
            }
            
            variablesSeenInCurrentFile.clear();
            
            progress.processedOne();
        }
        
        addResult(new ScatteringDegreeContainer(countedVariables.values()));
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Counted Variability Variables";
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
        ScatteringDegree countedVar = countedVariables.get(varName);
        
        // heuristically handle tristate (_MODULE) variables
        if (countedVar == null && varName.endsWith("_MODULE")) {
            varName = notNull(varName.substring(0, varName.length() - "_MODULE".length()));
            countedVar = countedVariables.get(varName);
        }
        
        if (countedVar != null) {
            
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
