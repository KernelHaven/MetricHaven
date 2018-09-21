package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.UsedVariabilityVarsVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Implements the <tt>Number of internal/external configuration options</tt> metric from
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class VariablesPerFunctionMetric extends AbstractFunctionVisitorBasedMetric<UsedVariabilityVarsVisitor> {

    public static final @NonNull Setting<
        net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunctionMetric.VarType> VARIABLE_TYPE_SETTING
            = new EnumSetting<>("metric.variables_per_function.measured_variables_type",
            net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunctionMetric.VarType.class, true, 
            net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunctionMetric.VarType.ALL,
            "Defines which variables should be counted for a function.");
    
    
    private @NonNull VarType measuredVars;
    
    /**
     * Constructor if no variability model is available. this won't verify whether a constant of a  CPP expression
     * belongs to the variability model.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    public VariablesPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Constructor to check if constant expression of CPP blocks belong to the variability model.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    public VariablesPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null);
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
    public VariablesPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, sdComponent);
    }
    
    /**
     * Constructor to use scattering degree to weight the results.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param bmComponent Optional: Allows to incorporate variables of the file presence conditions into the
     *     external variables list.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    public VariablesPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);
        
        config.registerSetting(VARIABLE_TYPE_SETTING);
        measuredVars = config.getValue(VARIABLE_TYPE_SETTING);
        
        if ((measuredVars == VarType.EXTERNAL_WITH_BUILD_VARS || measuredVars == VarType.ALL_WITH_BUILD_VARS)
            && null == bmComponent) {
            
            throw new SetUpException("Variables of file presence conditions shall be considered, but no build model "
                + "specified in class" + this.getClass().getName() + " with setting: "
                + VARIABLE_TYPE_SETTING.getKey() + " = " + measuredVars.name());
        }
    }

    
    @Override
    public @NonNull String getResultName() {
        StringBuffer resultName = new StringBuffer(measuredVars.toString());
        resultName.append(" Vars per Func.");
        resultName.append(getWeightsName());
        
        return NullHelpers.notNull(resultName.toString());
    }

    @Override
    protected @NonNull UsedVariabilityVarsVisitor createVisitor(@Nullable VariabilityModel varModel) {
        UsedVariabilityVarsVisitor visitor;
        if (measuredVars == VarType.INTERNAL || measuredVars == VarType.EXTERNAL || measuredVars == VarType.ALL) {
            visitor = new UsedVariabilityVarsVisitor(varModel);
        } else {
            visitor = new UsedVariabilityVarsVisitor(varModel, getBuildModel());
        }
        return visitor;
    }

    @Override
    protected double computeResult(@NonNull UsedVariabilityVarsVisitor visitor) {
        Set<String> variables;
        switch (measuredVars) {
        case INTERNAL:
            variables = visitor.internalVars();
            break;
        case EXTERNAL_WITH_BUILD_VARS:
            // Falls through
        case EXTERNAL:
            variables = visitor.externalVars();
            break;
        case ALL_WITH_BUILD_VARS:
            // Falls through
        case ALL:
            variables = visitor.allVars();
            break;
        default:
            variables = visitor.allVars();
            throw new IllegalArgumentException("Unsupported " + VarType.class.getSimpleName()
                + " property = " + measuredVars.name());
        }
        
        int result = 0;
        for (String variable : variables) {
            result += getWeighter().getWeight(variable, getCodeFile());
        }
        
        return result;
    }

}
