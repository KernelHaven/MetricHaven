package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * A component that counts variability variables in source files with a given rating.
 * 
 * @author Adam
 */
public class WeightedVariabilityVariableCount extends AnalysisComponent<MetricResult> {

    private AnalysisComponent<MetricResult> variableRatings;
    
    private AnalysisComponent<SourceFile> sourceFiles;
    
    private Map<String, Double> variableWeights;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param variableRatings The component to get the variable ratings from. The {@link MetricResult#getContext()} is
     *      expected to be variable names.
     * @param sourceFiles The component to get the source file blocks to count in from.
     */
    public WeightedVariabilityVariableCount(Configuration config, AnalysisComponent<MetricResult> variableRatings,
            AnalysisComponent<SourceFile> sourceFiles) {
        super(config);
    }

    @Override
    protected void execute() {
        variableWeights = new HashMap<>();
        
        MetricResult rating;
        while ((rating = variableRatings.getNextResult()) != null) {
            variableWeights.put(rating.getContext(), rating.getValue());
        }
        
        SourceFile file;
        while ((file = sourceFiles.getNextResult()) != null) {
            
            double value = countInSourceFile(file);
            
            addResult(new MetricResult(file.getPath(), null, -1, file.getPath().getName(), value));
        }
        
    }

    @Override
    public String getResultName() {
        return "Weighted Variability Variable Count";
    }
    
    /**
     * Counts the weighted variables in the given source file.
     * 
     * @param file The {@link SourceFile} to count in.
     * 
     * @return The summed weight of variables found in the file.
     */
    private double countInSourceFile(SourceFile file) {
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
        Double result = variableWeights.get(variable);
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
