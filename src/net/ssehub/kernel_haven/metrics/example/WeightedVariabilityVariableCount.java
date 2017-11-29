package net.ssehub.kernel_haven.metrics.example;

import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
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
     * 
     * @throws SetUpException If creating this metric fails.
     */
    public WeightedVariabilityVariableCount(Configuration config) throws SetUpException {
        super(config);
    }
    
    @Override
    protected double calc(SourceFile file, Map<String, Double> variableWeight) {
        this.variableWeight = variableWeight; 
        
        double result = 0.0;
        
        for (CodeElement topElement : file) {
            result += countInElement(topElement);
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
            LOGGER.logWarning("Returning weight 0 for unknown variable " + variable);
            result = 0.0;
        }
        return result;
    }
    
    /**
     * Counts variables in the given source code element. Recursively walks through all child elements.
     * 
     * @param element The element to count in.
     * @return The summed weight of variables found in the element.
     */
    private double countInElement(CodeElement element) {
        double result = 0.0;
        
        if (element.getCondition() != null) {
            result += countInCondition(element.getCondition());
        }
        
        for (CodeElement child : element.iterateNestedElements()) {
            result += countInElement(child);
        }
        
        return result;
    }
    
    /**
     * Counts the variables in the given formula. Recursively walks through the formula.
     * 
     * @param formula The formula to count in.
     * @return The summed weight of variables found in the formula.
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
