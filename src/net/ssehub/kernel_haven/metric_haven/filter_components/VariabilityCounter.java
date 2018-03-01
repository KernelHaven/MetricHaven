package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
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
 * Counts in how many #ifdefs and files a {@link VariabilityVariable} is used.
 * 
 * @author Adam
 */
public class VariabilityCounter extends AnalysisComponent<CountedVariabilityVariable> implements ISyntaxElementVisitor,
        IVoidFormulaVisitor {

    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    
    private @NonNull AnalysisComponent<SourceFile> cmProvider;
    
    private @NonNull Map<@NonNull String, CountedVariabilityVariable> countedVariables;
    
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
            @NonNull AnalysisComponent<SourceFile> cmProvider) {
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
            countedVariables.put(variable.getName(), new CountedVariabilityVariable(variable));
        }
        
        SourceFile file;
        while ((file = cmProvider.getNextResult()) != null) {
            
            for (CodeElement element : file) {
                if (element instanceof ISyntaxElement) {
                    ((ISyntaxElement) element).accept(this);
                } else {
                    LOGGER.logError("This component can only handle ISyntaxElements");
                }
            }
            
            variablesSeenInCurrentFile.clear();
        }
        
        for (CountedVariabilityVariable variable : countedVariables.values()) {
            addResult(notNull(variable));
        }
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
        CountedVariabilityVariable countedVar = countedVariables.get(varName);
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
