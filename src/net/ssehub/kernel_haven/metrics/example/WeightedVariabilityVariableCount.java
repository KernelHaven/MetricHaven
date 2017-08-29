package net.ssehub.kernel_haven.metrics.example;

import java.util.Map;

import net.ssehub.kernel_haven.code_model.Block;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.filters.CombinedCodeFileBlockMetric;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * A metric that counts variability variables used in source files. The variables are weighted based on another metric.
 * 
 * @author Adam
 */
public class WeightedVariabilityVariableCount extends CombinedCodeFileBlockMetric {

    private Map<String, Double> variableWeight;
    
    /**
     * Creates a new weighted variability count metric.
     * 
     * @param config The global pipeline configuration.
     */
    public WeightedVariabilityVariableCount(Configuration config) {
        super(config);
    }
    
    @Override
    protected AbstractMetric createOtherMetric(Configuration config) {
        // TODO: make this configurable
        return new VariableTypeMetric(config);
    }

    @Override
    protected double run(SourceFile file, Map<String, Double> variableWeight) {
        this.variableWeight = variableWeight; 
        
        double result = 0.0;
        
        for (Block topBlock : file) {
            result += countInBlock(topBlock);
        }
        
        return result;
    }
    
    /**
     * Reads the weight from the variableWeight map.
     * 
     * @param variable The variable to read the weight for.
     * 
     * @return The weight of the other variable.
     */
    private double getWeight(String variable) {
        Double result = variableWeight.get(variable);
        if (result == null) {
            LOGGER.logWarning("Returning weight 0 for unkown variable " + variable);
            result = 0.0;
        }
        return result;
    }
    
    /**
     * Counts variables in the given source code block. Recursively walks through all child blocks.
     * 
     * @param block The block to count in.
     * @return The summed weight of variables found in the block.
     */
    private double countInBlock(Block block) {
        double result = 0.0;
        
        if (block.getCondition() != null) {
            result += countInCondition(block.getCondition());
        }
        
        for (Block child : block) {
            result += countInBlock(child);
        }
        
        return result;
    }
    
    /**
     * Counts the variables in the given formula. Recursively walks through the formula.
     * 
     * @param formula The formula to count in.
     * @return The summed weight of variables found in the block.
     */
    private double countInCondition(Formula formula) {
        
        double result = 0.0;
        
        if (formula instanceof Disjunction) {
            result += countInCondition(((Disjunction) formula).getLeft());
            result += countInCondition(((Disjunction) formula).getRight());
        } else if (formula instanceof Conjunction) {
            result += countInCondition(((Conjunction) formula).getLeft());
            result += countInCondition(((Conjunction) formula).getRight());
        } else if (formula instanceof Negation) {
            result += countInCondition(((Negation) formula).getFormula());
        } else if (formula instanceof Variable) {
            result += getWeight(((Variable) formula).getName());
        } // ignore true and false
        
            
        return result;
    }

}
