package net.ssehub.kernel_haven.metrics.example;
import net.ssehub.kernel_haven.code_model.Block;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.filters.CodeFunctionMetric;
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
    protected double run(TypeChefBlock function) {
        TypeChefBlock body = function.getChild("Body");
        return 1.0 + calc(body);
    }
    
    /**
     * Recursively walks through the AST and counts the while-, if-, for- and case-statements.
     * 
     * @param block The current AST node.
     * 
     * @return The number of while-, if-, for- and case-statements found.
     */
    private double calc(TypeChefBlock block) {
        double result = 0.0;
        
        switch (block.getText()) {
        case "WhileStatement":
            result++;
            break;
        case "IfStatement":
            result++;
            break;
        case "ForStatement":
            result++;
            break;
        case "CaseStatement":
            result++;
            break;
        default:
            // ignore
        }
        
        for (Block b : block) {
            result += calc((TypeChefBlock) b);
        }
        
        return result;
    }

}
