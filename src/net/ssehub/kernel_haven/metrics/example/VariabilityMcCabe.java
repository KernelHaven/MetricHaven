package net.ssehub.kernel_haven.metrics.example;

import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.filters.CodeFileBlockMetric;

/**
 * McCabe cyclomatic complexity for the conditional compilation code blocks.
 * 
 * @author Adam
 */
public class VariabilityMcCabe extends CodeFileBlockMetric {

    /**
     * Creates a new McCabe conditional compilation block metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public VariabilityMcCabe(Configuration config) {
        super(config);
    }

    @Override
    protected double calc(SourceFile file) {
        
        int result = 1;
        
        for (CodeElement child : file) {
            result += count(child);
        }
        
        return result;
    }
    
    /**
     * Counts the number of conditional compilation blocks.
     * 
     * @param block The block to count in.
     * 
     * @return The number of conditional compilation blocks. This is 1 (this block) + number of children blocks.
     */
    private int count(CodeElement block) {
        int result = 1;
        if (block.getCondition() == null) {
            // this is an else block
            result = 0;
        }
        
        for (CodeElement child : block.iterateNestedElements()) {
            result += count(child);
        }
        
        return result;
    }

}
