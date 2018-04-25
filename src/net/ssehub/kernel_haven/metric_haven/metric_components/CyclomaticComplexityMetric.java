package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.McCabeVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
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
    public static enum CCType {
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
        
        this(config, codeFunctionFinder, varModelComponent, null, null);
    }
    
    /**
     * Constructor to use scattering degree to weight the results.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    public CyclomaticComplexityMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, sdComponent);
    }
    
    /**
     * Constructor for the automatic instantiation inside the {@link AllFunctionMetrics} component.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param bmComponent Will be ignored.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    public CyclomaticComplexityMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);

        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredCode = config.getValue(VARIABLE_TYPE_SETTING);
        
        if (measuredCode == CCType.MCCABE
            && (getSDType() != SDType.NO_SCATTERING || getCTCRType() != CTCRType.NO_CTCR)) {
            
            StringBuffer errMsg = new StringBuffer("Cannot apply variability weights on non variability metric. "
                + "Setting was:\n - Metric: ");
            errMsg.append(this.getClass().getName());
            // Variability type
            errMsg.append("\n - ");
            errMsg.append(CCType.class.getSimpleName());
            errMsg.append(": ");
            errMsg.append(measuredCode.name());
            // Scattering Degree
            errMsg.append("\n - ");
            errMsg.append(SDType.class.getSimpleName());
            errMsg.append(": ");
            errMsg.append(getSDType().name());
            // Cross-Tree Constraint Ratio
            errMsg.append("\n - ");
            errMsg.append(CTCRType.class.getSimpleName());
            errMsg.append(": ");
            errMsg.append(getCTCRType().name());

            throw new SetUpException(errMsg.toString());
        }
    }

    @Override
    public @NonNull String getResultName() {
        StringBuffer resultName;
        switch (measuredCode) {
        case MCCABE:
            resultName = new StringBuffer("McCabe");
            break;
        case VARIATION_POINTS:
            resultName = new StringBuffer("CC on VPs");
            break;
        case ALL:
            resultName = new StringBuffer("McCabe + CC on VPs");
            break;
        default:
            resultName = new StringBuffer("Unknown Cyclomatic Complexity");
            break;
        }
        
        if (getSDType() != SDType.NO_SCATTERING || getCTCRType() != CTCRType.NO_CTCR) {
            resultName.append(" x ");            
            resultName.append(getSDType().name());            
            resultName.append(" x ");            
            resultName.append(getCTCRType().name());            
        }
        
        return NullHelpers.notNull(resultName.toString());
    }

    @Override
    protected @NonNull McCabeVisitor createVisitor(@Nullable VariabilityModel varModel) {
        return (measuredCode == CCType.MCCABE) ? new McCabeVisitor(varModel)
            // getWeighter won't be null when the visitor is created
            : new McCabeVisitor(varModel, NullHelpers.notNull(getWeighter()));
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
            LOGGER.logError("Unknown code type specified for ", getClass().getName());
            break;
        }
        
        return result;
    }

}
