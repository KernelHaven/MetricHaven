package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.VariationPointerCounter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter.CodeFunction;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Formula;

/**
 * Measures the Cyclomatic Complexity of Functions. Supports 3 different variants:
 * <ul>
 *   <li>MCCABE: Measures the cyclomatic complexity of classical code elements as defined by McCabe, uses a
 *   simplification that only the following keywords will be counted: <tt>if, for, while, case</tt>.</li>
 *   <li>VARIATION_POINTS: Measures the cyclomatic complexity of variation points only, uses a
 *   simplification that only the following keywords will be counted: <tt>if, elif</tt>.</li>
 *   <li>ALL: MCCABE + VARIATION_POINTS</li>
 * </ul>
 * @author El-Sharkawy
 * @author Adam
 */
public class CyclomaticComplexityMetric extends AnalysisComponent<MetricResult> {
    
    /**
     * Specification which kind of Cyclomatic Complexity shall be measured.
     * @author El-Sharkawy
     *
     */
    static enum CCType {
        MCCABE, VARIATION_POINTS, ALL;
    }
    
    static final Setting<CCType> VARIABLE_TYPE_SETTING
        = new EnumSetting<>("metric.cyclomatic_complexity.measured_type", CCType.class, true, 
            CCType.MCCABE, "Defines which variables should be counted for a function.");

    private AnalysisComponent<CodeFunction> codeFunctionFinder;
    private CCType measuredCode;
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException In case of problems with the configuration of {@link #VARIABLE_TYPE_SETTING}.
     */
    public CyclomaticComplexityMetric(Configuration config, AnalysisComponent<CodeFunction> codeFunctionFinder)
        throws SetUpException {
        
        super(config);
        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredCode = config.getValue(VARIABLE_TYPE_SETTING);
        this.codeFunctionFinder = codeFunctionFinder;
    }

    @Override
    protected void execute() {
        CodeFunction function;
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            Formula baseLine = function.getFunction().getPresenceCondition();
            VariationPointerCounter counter = new VariationPointerCounter(baseLine);
            int mcCabeValue = count(function.getFunction(), counter);
            int result;
            switch (measuredCode) {
            case MCCABE:
                result = 1 + mcCabeValue;
                break;
            case VARIATION_POINTS:
                result = 1 + counter.countVPs();
                break;
            case ALL:
                result = mcCabeValue + counter.countVPs() + 2;
                break;
            default:
                // Indicate that something went wrong.
                result = 0;
                Logger.get().logError("Unknown code type specified for " + getClass().getName());
                break;
            }
            
            addResult(new MetricResult(function.getName(), result));
        }
    }

    /**
     * Recursively walks through the AST and counts the while-, if-, for- and case-statements.
     * 
     * @param element The current AST node.
     * @param counter To count the different variation points based on changing presence conditions.
     * 
     * @return The number of while-, if-, for- and case-statements found.
     */
    private int count(SyntaxElement element, VariationPointerCounter counter) {
        int result = 0;
        
        switch (measuredCode) {
        case ALL: 
            counter.add(element.getPresenceCondition());            
            // falls through
        case MCCABE:
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
            break;
        case VARIATION_POINTS:
            counter.add(element.getPresenceCondition());
            break;
        default:
            // Do nothing an return 0
            break;
        }
        
        for (SyntaxElement child : element.iterateNestedSyntaxElements()) {
            result += count(child, counter);
        }
        
        return result;
    }

    @Override
    public String getResultName() {
        String result;
        switch (measuredCode) {
        case MCCABE:
            result = "McCabe's Cyclomatic Complexity";
            break;
        case VARIATION_POINTS:
            result = "Cyclomatic Complexity of Variation Points";
            break;
        case ALL:
            result = "Cyclomatic Complexity of Code + VPs";
            break;
        default:
            result = "Unknown Cyclomatic Complexity";
            break;
        }
        
        return result;
    }

}
