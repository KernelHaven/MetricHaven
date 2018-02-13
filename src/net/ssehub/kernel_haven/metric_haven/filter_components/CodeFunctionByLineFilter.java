package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A filter component that filters {@link CodeFunction}s by line numbers. A configuration option specifies a line
 * number, and only function where this line number is inside (i.e.
 * <code>func.getLineStart() <= lineNumber && func.getLineEnd() >= lineNumber</code>) are passed along.
 * 
 * @author Adam
 */
public class CodeFunctionByLineFilter extends AnalysisComponent<CodeFunction> {

    private static final @NonNull Setting<@NonNull Integer> LINE_NUMBER_SETTING
            = new Setting<>("analysis.code_function.line", Type.INTEGER, true, null,
                    "Specifies, the line number that the CodeFunctionByLineFilter should filter the code functions for."
                    + " It will pass on the function that this line lies in.");
    
    private @NonNull AnalysisComponent<CodeFunction> functionSource;
    
    private int lineNumber;
    
    /**
     * Creates this filter.
     * 
     * @param config The pipeline configuration.
     * @param functionSource The component to get the {@link CodeFunction}s to filter from.
     * 
     * @throws SetUpException If reading the line number setting fails.
     */
    public CodeFunctionByLineFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> functionSource) throws SetUpException {
        
        super(config);
        
        config.registerSetting(LINE_NUMBER_SETTING);
        lineNumber = config.getValue(LINE_NUMBER_SETTING);
        
        this.functionSource = functionSource;
    }

    @Override
    protected void execute() {
        CodeFunction function;
        while ((function = functionSource.getNextResult()) != null) {
            
            Function func = function.getFunction();
            if (func.getLineStart() <= lineNumber && func.getLineEnd() >= lineNumber) {
                addResult(function);
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions (filtered by line number)";
    }

}
