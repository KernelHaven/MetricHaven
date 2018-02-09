package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.UsedVariabilityVarsVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Implements the <tt>Number of internal/external configuration options</tt> metric from
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class VariablesPerFunctionMetric extends AnalysisComponent<MetricResult> {

    /**
     * Specification which kind of variables shall be measured.
     * @author El-Sharkawy
     *
     */
    static enum VarType {
        INTERNAL, EXTERNAL, ALL;
    }
    
    public static final @NonNull Setting<@NonNull VarType> VARIABLE_TYPE_SETTING
        = new EnumSetting<>("metric.variables_per_function.measured_variables_type", VarType.class, true, 
                VarType.ALL, "Defines which variables should be counted for a function.");
    
    
    private @NonNull VarType measuredVars;
    
    private @NonNull AnalysisComponent<CodeFunction> functionFinder;
    
    /**
     * Sole constructor for this class.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param functionFinder The component to get the code functions from.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_PROPERTY} was defined with an invalid option.
     */
    public VariablesPerFunctionMetric(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> functionFinder) throws SetUpException {
        
        super(config);
        
        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredVars = config.getValue(VARIABLE_TYPE_SETTING);
        this.functionFinder = functionFinder;
    }

    @Override
    protected void execute() {
        UsedVariabilityVarsVisitor visitor = new UsedVariabilityVarsVisitor(null);
        CodeFunction function;
        while ((function = functionFinder.getNextResult()) != null) {
            Function functionAST = function.getFunction();
            visitor.reset();
            functionAST.accept(visitor);
            
            int result;
            switch (measuredVars) {
            case EXTERNAL:
                result = visitor.externalVarsSize();
                break;
            case INTERNAL:
                result = visitor.internalVarsSize();
                break;
            case ALL:
                result = visitor.allVarsSize();
                break;
            default:
                throw new IllegalArgumentException("Unsupported " + VarType.class.getSimpleName()
                    + " property = " + measuredVars.name());
            }
            
            File cFile = function.getSourceFile().getPath();
            File includedFile = cFile.equals(functionAST.getSourceFile()) ? null : functionAST.getSourceFile();
            addResult(new MetricResult(cFile, includedFile, functionAST.getLineStart(), function.getName(), result));
        }
    }
    
    @Override
    public @NonNull String getResultName() {
        return "Variables per Function (" + measuredVars.toString() + ")";
    }

}
