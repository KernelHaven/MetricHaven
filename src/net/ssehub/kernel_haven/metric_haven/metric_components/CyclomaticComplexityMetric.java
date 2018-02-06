package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.SyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.McCabeVisitor;
import net.ssehub.kernel_haven.util.Logger;

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
    
    public static final Setting<CCType> VARIABLE_TYPE_SETTING
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
            SyntaxElement functionAST = function.getFunction();
            McCabeVisitor visitor = new McCabeVisitor(null);
            functionAST.accept(visitor);

            int result;
            switch (measuredCode) {
            case MCCABE:
                result = visitor.getClassicCyclomaticComplexity();
                break;
            case VARIATION_POINTS:
                result = visitor.getVariabilityCyclomaticComplexity();
                break;
            case ALL:
                result = visitor.getCombinedCyclomaticComplexity();
                break;
            default:
                // Indicate that something went wrong.
                result = 0;
                Logger.get().logError("Unknown code type specified for " + getClass().getName());
                break;
            }
            
            File cFile = function.getSourceFile().getPath();
            File includedFile = cFile.equals(functionAST.getSourceFile()) ? null : functionAST.getSourceFile();
            addResult(new MetricResult(cFile, includedFile, functionAST.getLineStart(), function.getName(), result));
        }
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
