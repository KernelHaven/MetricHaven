package net.ssehub.kernel_haven.metric_haven.metrics;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filters.CodeFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric;

/**
 * The McCabe cyclomatic complexity metric for code functions. This metric ignores variability; it thus calculates the
 * complexity "visible" in the 150% model.
 * @deprecated Use {@link CyclomaticComplexityMetric} instead.
 * @author Adam
 */
public class NoVariabilityMcCabe extends CodeFunctionMetric {

    /**
     * Creates a new McCabe metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * 
     * @throws SetUpException If creating this metric fails. 
     */
    public NoVariabilityMcCabe(Configuration config) throws SetUpException {
        super(config);
    }

    @Override
    protected double calc(SyntaxElement function) {
        SyntaxElement body = function.getNestedElement("Body");
        return 1.0 + count(body);
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
