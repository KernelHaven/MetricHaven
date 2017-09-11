package net.ssehub.kernel_haven.metrics.example;
import net.ssehub.kernel_haven.code_model.Block;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.filters.CodeFunctionMetric;
import net.ssehub.kernel_haven.typechef.ast.SyntaxElements;
import net.ssehub.kernel_haven.typechef.ast.TypeChefBlock;

/**
 * The McCabe cyclomatic complexity metric for code functions. This metric ignores variability; it thus calculates the
 * complexity "visible" in the 150% model.
 * 
 * @author Adam
 */
public class NoVariabilityMcCabe extends CodeFunctionMetric {

    /**
     * Creates a new McCabe metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public NoVariabilityMcCabe(Configuration config) {
        super(config);
    }

    @Override
    protected double calc(TypeChefBlock function) {
        TypeChefBlock body = function.getChild("Body");
        return 1.0 + count(body);
    }
    
    /**
     * Recursively walks through the AST and counts the while-, if-, for- and case-statements.
     * 
     * @param block The current AST node.
     * 
     * @return The number of while-, if-, for- and case-statements found.
     */
    private double count(TypeChefBlock block) {
        double result = 0.0;
        
        
        if (block.getType() instanceof SyntaxElements) {
            switch ((SyntaxElements) block.getType()) {
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
        
        for (Block b : block) {
            result += count((TypeChefBlock) b);
        }
        
        return result;
    }

}
