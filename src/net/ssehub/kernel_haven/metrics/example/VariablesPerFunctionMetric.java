package net.ssehub.kernel_haven.metrics.example;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metrics.filters.CodeFunctionMetric;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;

/**
 * Implements the <tt>Number of internal/external configuration options</tt> metric from
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class VariablesPerFunctionMetric extends CodeFunctionMetric {

    /**
     * Specification which kind of variables shall be measured.
     * @author El-Sharkawy
     *
     */
    private static enum VarType {
        INTERNAL, EXTERNAL, ALL;
    }
    
    private static final Setting<VarType> VARIABLE_TYPE_SETTING
        = new EnumSetting<>("metric.variables_per_function.measured_variables_type", VarType.class, true, 
                VarType.ALL, "Defines which variables should be counted for a function.");
    
    
    private VarType measuredVars;
    
    /**
     * Sole constructor for this class.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @throws SetUpException If {@link #VARIABLE_TYPE_PROPERTY} was defined with an invalid option.
     */
    public VariablesPerFunctionMetric(Configuration config) throws SetUpException {
        super(config);
        
        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredVars = config.getValue(VARIABLE_TYPE_SETTING);
    }

    /**
     * Entry point for recursive collection of used variables. 
     * @param function The function to measure (top level element).
     * @param finder A visitor which is used to collect all used variables.
     */
    private void collectVars(SyntaxElement function, VariableFinder finder) {
        // This formula was already visited in calc method
        Set<Formula> formulaCache = new HashSet<>();
        formulaCache.add(function.getCondition());
        
        Iterator<SyntaxElement> itr = function.iterateNestedSyntaxElements().iterator();
        while (itr.hasNext()) {
            SyntaxElement nestedElement = itr.next();
            collectVars(nestedElement, finder, formulaCache);
        }
    }
    
    /**
     * Recursive part of variable retrieval.
     * @param astNode An arbitrary sub-element of the function to measure.
     * @param finder A visitor which is used to collect all used variables.
     * @param formulaCache For optimization: the list of already gathered formulas.
     */
    private void collectVars(SyntaxElement astNode, VariableFinder finder, Set<Formula> formulaCache) {
        Formula formula = astNode.getCondition();
        
        // Optimization: search only in formula if not visited before
        if (!formulaCache.contains(formula)) {
            formula.accept(finder);
            formulaCache.add(formula);
        }
        
        // Recursively search in nested elements
        Iterator<SyntaxElement> itr = astNode.iterateNestedSyntaxElements().iterator();
        while (itr.hasNext()) {
            SyntaxElement nestedElement = (SyntaxElement) itr.next();
            collectVars(nestedElement, finder, formulaCache);
        }
    }
    
    @Override
    protected double calc(SyntaxElement function) {
        VariableFinder finder = new VariableFinder();
        function.getPresenceCondition().accept(finder);
        Set<String> externalVariables = new HashSet<>(finder.getVariableNames());

        int result;
        switch (measuredVars) {
        case EXTERNAL:
            result = externalVariables.size();
            break;
        case INTERNAL:
            collectVars(function, finder);
            Set<String> allVars = new HashSet<>(finder.getVariableNames());
            allVars.removeAll(externalVariables);
            result = allVars.size();
            break;
        case ALL:
            collectVars(function, finder);
            result = finder.getVariableNames().size();
            break;
        default:
            throw new IllegalArgumentException("Unsupported " + VarType.class.getSimpleName()
                + " property = " + measuredVars.name());
        }
        
        return result;
    }

}
