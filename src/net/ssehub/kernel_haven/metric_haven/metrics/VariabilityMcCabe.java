package net.ssehub.kernel_haven.metric_haven.metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filters.CodeFileBlockMetric;

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
     * 
     * @throws SetUpException If creating this metric fails. 
     */
    public VariabilityMcCabe(Configuration config) throws SetUpException {
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
     * Counts the number of conditional compilation elements.
     * 
     * @param element The element to count in.
     * 
     * @return The number of conditional compilation elements. This is 1 (this element) + number of children elements.
     */
    private int count(CodeElement element) {
        int result = 1;
        if (element.getCondition() == null) {
            // this is an else block
            result = 0;
        }
        
        for (CodeElement child : element.iterateNestedElements()) {
            result += count(child);
        }
        
        return result;
    }

}
