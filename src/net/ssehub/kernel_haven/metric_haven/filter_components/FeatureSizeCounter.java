package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Determines the size of {@link VariabilityVariable}s in lines of code.
 * 
 * @author Sascha El-Sharkawy
 */
public class FeatureSizeCounter extends AnalysisComponent<FeatureSize> implements ISyntaxElementVisitor {

    private @NonNull AnalysisComponent<SourceFile<?>> cmProvider;
    private @Nullable AnalysisComponent<BuildModel> bmProvider;
    
    private @NonNull FeatureSize featureSizes;
    
    private @NonNull Set<String> buildVars;
    private @NonNull VariableFinder finder;
    private int statementCount;
    private Formula currentPC;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param cmProvider The component to get the code model from.
     */
    public FeatureSizeCounter(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> cmProvider) {
        
        this(config, cmProvider, null);
    }
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param cmProvider The component to get the code model from.
     * @param bmProvider Optional: The component to get the build model from.
     */
    public FeatureSizeCounter(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> cmProvider,
            @Nullable AnalysisComponent<BuildModel> bmProvider) {
        
        super(config);
        
        this.cmProvider = cmProvider;
        this.bmProvider = bmProvider;
        this.featureSizes = new FeatureSize();
        buildVars = new HashSet<>();
        finder = new VariableFinder();
    }

    @Override
    protected void execute() {
        BuildModel bm = null;
        if (bmProvider != null) {
            bm = bmProvider.getNextResult();
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        while ((file = cmProvider.getNextResult()) != null) {
            // Consider variables of build model
            if (null != bm) {
                buildVars.clear();
                Formula buildPC = bm.getPc(file.getPath());
                if (null != buildPC) {
                    buildPC.accept(finder);
                    buildVars.addAll(finder.getVariableNames());
                    finder.clear();
                }
            }
            
            // Count lines of code
            for (ISyntaxElement element : file.castTo(ISyntaxElement.class)) {
                statementCount = 0;
                element.accept(this);
                countAndReset(null);
            }
            
            progress.processedOne();
        }
        
        addResult(featureSizes);
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "LoC by Feaure";
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        count(function.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitFunction(function);
    }
    
    @Override
    public void visitSingleStatement(@NonNull SingleStatement statement) {
        count(statement.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitSingleStatement(statement);
    }
    
    @Override
    public void visitTypeDefinition(@NonNull TypeDefinition typeDef) {
        count(typeDef.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitTypeDefinition(typeDef);
    }

    @Override
    public void visitBranchStatement(@NonNull BranchStatement branchStatement) {
        count(branchStatement.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitBranchStatement(branchStatement);
    }
    
    @Override
    public void visitSwitchStatement(@NonNull SwitchStatement switchStatement) {
        count(switchStatement.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitSwitchStatement(switchStatement);
    }
    
    @Override
    public void visitLoopStatement(@NonNull LoopStatement loop) {
        count(loop.getPresenceCondition(), 1);
        ISyntaxElementVisitor.super.visitLoopStatement(loop);
    }
    
    /**
     * Counts a statement.
     * @param pc The presence condition of the statement
     * @param loc How many lines of code shall be counted for a statement (default is 1).
     */
    private void count(@NonNull Formula pc, int loc) {
        if (pc == currentPC || pc.equals(currentPC)) {
            statementCount += loc;
        } else {
            countAndReset(pc);
        }
    }

    /**
     * Resets {@link #currentPC} to pc and stores the currently counted {@link #statementCount} value for the last
     * presence condition.
     * @param pc The new presence condition, <tt>null</tt>to reset for a new function.
     */
    private void countAndReset(Formula pc) {
        // Store current results
        currentPC.accept(finder);
        Set<String> vars = new HashSet<>(finder.getVariableNames());
        vars.addAll(buildVars);
        for (String variable : vars) {
            featureSizes.increment(variable, statementCount);
        }
        
        // Reset to collect results for new PC
        finder.clear();
        currentPC = pc;
        statementCount = 0;
    }
}
