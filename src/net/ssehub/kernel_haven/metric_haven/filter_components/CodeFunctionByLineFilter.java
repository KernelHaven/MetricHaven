package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A filter component that filters {@link CodeFunction}s by line numbers. A configuration option specifies a line
 * number, and only function where this line number is inside (i.e.
 * <code>func.getLineStart() <= lineNumber && func.getLineEnd() >= lineNumber</code>) are passed along.
 * 
 * @author Adam
 */
public class CodeFunctionByLineFilter extends AnalysisComponent<CodeFunction> {

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
        
        config.registerSetting(MetricSettings.LINE_NUMBER_SETTING);
        Integer value = config.getValue(MetricSettings.LINE_NUMBER_SETTING);
        if (null != value) {
            lineNumber = value;
        } else {
            throw new SetUpException(MetricSettings.LINE_NUMBER_SETTING.getKey() + "missing, but required for "
                + this.getClass().getCanonicalName());
        }
        
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
