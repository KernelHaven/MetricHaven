package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter.CodeFunction;

/**
 * The McCabe cyclomatic complexity metric for code functions. This metric ignores variability; it thus calculates the
 * complexity "visible" in the 150% model.
 * 
 * @author Adam
 */
public class NoVariabilityMcCabe extends AnalysisComponent<MetricResult> {

    private AnalysisComponent<CodeFunction> codeFunctionFinder;
    
    /**
     * Creates this component.
     * 
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     */
    public NoVariabilityMcCabe(Configuration config, AnalysisComponent<CodeFunction> codeFunctionFinder) {
        super(config);
        
        this.codeFunctionFinder = codeFunctionFinder;
    }

    @Override
    protected void execute() {
        CodeFunction function;
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            double value = 1.0 + count(function.getFunction());
            
            addResult(new MetricResult(function.getName(), value));
        }
    }

    @Override
    public String getResultName() {
        return "McCabe Metric (Ignoring Variability)";
    }
    
    /**
     * Recursively walks through the AST and counts the while-, if-, for- and case-statements.
     * 
     * @param element The current AST node.
     * 
     * @return The number of while-, if-, for- and case-statements found.
     */
    private double count(SyntaxElement element) {
        double result = 0.0;
        
        if (element.getType() instanceof SyntaxElementTypes) {
            switch ((SyntaxElementTypes) element.getType()) {
            case IF_STATEMENT:
            case ELIF_STATEMENT: // TypeChef produces separate nodes for "else if ()"
            case WHILE_STATEMENT:
            case FOR_STATEMENT:
            case DO_STATEMENT:
            case CASE_STATEMENT:
                result++;
                break;
            default:
                // ignore
            }
        }
        
        for (SyntaxElement child : element.iterateNestedSyntaxElements()) {
            result += count(child);
        }
        
        return result;
    }

}
