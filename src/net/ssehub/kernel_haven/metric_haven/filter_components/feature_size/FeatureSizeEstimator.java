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
package net.ssehub.kernel_haven.metric_haven.filter_components.feature_size;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
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
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Counts the lines of code controlled by a {@link VariabilityVariable}.
 * 
 * @author El-Sharkawy
 */
public class FeatureSizeEstimator extends AnalysisComponent<FeatureSizeContainer> implements ISyntaxElementVisitor,
    IVoidFormulaVisitor {

    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    
    private @NonNull AnalysisComponent<SourceFile<?>> cmProvider;
    private @Nullable AnalysisComponent<BuildModel> bmProvider;
    
    private @Nullable FeatureSizeContainer featureSizes;
    
    /**
     * The condition of the file. Use to compute presence conditions.
     */
    private @NonNull Formula filePC;
    private int loc;
    private boolean positive;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param vmProvider The component to get the variability model from.
     * @param cmProvider The component to get the code model from.
     * @param bmProvider The component to get the build model from.
     */
    public FeatureSizeEstimator(@NonNull Configuration config, @NonNull AnalysisComponent<VariabilityModel> vmProvider,
        @NonNull AnalysisComponent<SourceFile<?>> cmProvider, @Nullable AnalysisComponent<BuildModel> bmProvider) {
        
        super(config);
        
        this.vmProvider = vmProvider;
        this.cmProvider = cmProvider;
        this.bmProvider = bmProvider;
        filePC = True.INSTANCE;
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = vmProvider.getNextResult();
        if (varModel == null) {
            LOGGER.logError("Did not get a variability model", "Can't create any results");
            return;
        }
        featureSizes = new FeatureSizeContainer(varModel);
        
        BuildModel bm = (bmProvider != null) ? bmProvider.getNextResult() : null;
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        while ((file = cmProvider.getNextResult()) != null) {
            Formula filePC = (null != bm) ? bm.getPc(file.getPath()) : True.INSTANCE;
            if (null == filePC) {
                filePC = True.INSTANCE;
            }
            this.filePC = filePC;
            loc = 0;
            
            for (ISyntaxElement element : file.castTo(ISyntaxElement.class)) {
                element.accept(this);
            }
            
            positive = true;
            filePC.accept(this);
            
            progress.processedOne();
        }
        
        addResult(NullHelpers.notNull(featureSizes));
        
        progress.close();
    }

    /**
     * Computes the presence condition based on the (presence) condition within a file and the condition of the file.
     * @param condition The presence condition of an element within the file.
     * @return <tt>file condition &and; presence condition of AST element</tt>
     */
    private @NonNull Formula getPresenceCondition(@NonNull Formula condition) {
        Formula result = condition;
        if (condition != True.INSTANCE && filePC != True.INSTANCE) {
            result = new Conjunction(filePC, condition);
        } else if (condition == True.INSTANCE && filePC != True.INSTANCE) {
            result = filePC;
        }
        
        return result;
    }
    
    @Override
    public @NonNull String getResultName() {
        return "Feature Sizes";
    }
    
    /*
     * ISyntaxElementVisitor
     */
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        int oldLoc = loc;
        loc = 0;
        
        // continue into this block
        ISyntaxElementVisitor.super.visitCppBlock(block);
        
        Formula newCondition = block.getPresenceCondition();
        newCondition = getPresenceCondition(newCondition);
        positive = true;
        newCondition.accept(this);
        
        loc = oldLoc;
    }
    
    @Override
    public void visitSingleStatement(@NonNull SingleStatement statement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(@NonNull TypeDefinition typeDef) {
        // Count the typedef as one statement
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitTypeDefinition(typeDef);
    }

    // C Control structures
    
    @Override
    public void visitBranchStatement(@NonNull BranchStatement elseStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitBranchStatement(elseStatement);
    }
    
    @Override
    public void visitSwitchStatement(@NonNull SwitchStatement switchStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitCaseStatement(@NonNull CaseStatement caseStatement) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitCaseStatement(caseStatement);
    }
    
    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitLoopStatement(loop);
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        count();
        
        // Continue visiting
        ISyntaxElementVisitor.super.visitFunction(function);
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        // Do not visit comments!
    }
    
    /**
     * Counts one line of code .
     */
    private void count() {
        loc++;
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
        FeatureSize countedVar = NullHelpers.notNull(featureSizes).getFeatureSize(varName);
        
        // heuristically handle tristate (_MODULE) variables
        if (countedVar == null && varName.endsWith("_MODULE")) {
            varName = notNull(varName.substring(0, varName.length() - "_MODULE".length()));
            countedVar = NullHelpers.notNull(featureSizes).getFeatureSize(varName);
        }
        
        if (countedVar != null) {
            if (positive) {
                countedVar.incPositiveSize(loc);
            }
            countedVar.incTotalSize(loc);
        }
    }

    @Override
    public void visitNegation(@NonNull Negation formula) {
        positive = !positive;
        visit(formula.getFormula());
        positive = !positive;
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
