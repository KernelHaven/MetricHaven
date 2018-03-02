package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.McCabeVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

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
public class CyclomaticComplexityMetric extends AbstractFunctionVisitorBasedMetric<McCabeVisitor> {
    
    /**
     * Specification which kind of Cyclomatic Complexity shall be measured.
     * @author El-Sharkawy
     *
     */
    static enum CCType {
        MCCABE, VARIATION_POINTS, ALL;
    }
    
    public static final @NonNull Setting<@NonNull CCType> VARIABLE_TYPE_SETTING
        = new EnumSetting<>("metric.cyclomatic_complexity.measured_type", CCType.class, true, 
            CCType.MCCABE, "Defines which variables should be counted for a function.");

    private CCType measuredCode;
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException In case of problems with the configuration of {@link #VARIABLE_TYPE_SETTING}.
     */
    public CyclomaticComplexityMetric(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: The component to get the variability model from. If not <tt>null</tt>
     *     {@link CCType#VARIATION_POINTS} and {@link CCType#ALL} will check if at least one variable of the variability
     *     model is involved in {@link net.ssehub.kernel_haven.code_model.ast.CppBlock#getCondition()} expressions to
     *     treat a {@link CppBlock} as variation point.
     * @throws SetUpException In case of problems with the configuration of {@link #VARIABLE_TYPE_SETTING}.
     */
    public CyclomaticComplexityMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, null);
        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredCode = config.getValue(VARIABLE_TYPE_SETTING);
    }

    @Override
    public @NonNull String getResultName() {
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

    @Override
    protected @NonNull McCabeVisitor createVisitor(@Nullable VariabilityModel varModel) {
        return new McCabeVisitor(varModel);
    }

    @Override
    protected double computeResult(@NonNull McCabeVisitor visitor) {
        double result;
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
            result = Double.NaN;
            LOGGER.logError("Unknown code type specified for " + getClass().getName());
            break;
        }
        
        return result;
    }

}
