package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Creates from filtered {@link CodeFunction}s a {@link FunctionMap}, required for
 * {@link net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut}-metrics.
 * {@link OrderedCodeFunctionFilter} and {@link CodeFunctionFilter}
 * @see {@link CodeFunctionFilter}, {@link CodeFunctionByLineFilter}, and {@link OrderedCodeFunctionFilter}
 * @author El-Sharkawy
 *
 */
public class FunctionMapCreator extends AnalysisComponent<FunctionMap> {

    private @NonNull AnalysisComponent<CodeFunction> cmProvider;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param cmProvider The component to get the code model, filtered for code functions, from. Probably one of
     *      {@link CodeFunctionFilter}, {@link CodeFunctionByLineFilter}, or {@link OrderedCodeFunctionFilter}
     */
    public FunctionMapCreator(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> cmProvider) {
        
        super(config);
        this.cmProvider = cmProvider;
    }
    
    @Override
    protected void execute() {
        List<CodeFunction> functions = new LinkedList<>();
        CodeFunction function;
        while ((function = cmProvider.getNextResult()) != null)  {
            functions.add(function);
        }
        
        addResult(new FunctionMap(functions));
    }

    @Override
    public @NonNull String getResultName() {
        return "FunctionMap";
    }

}
