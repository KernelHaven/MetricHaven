package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measurement of various fan-in/fan-out metrics based on function calls.
 * @author El-Sharkawy
 *
 */
public class FanInOutMetric extends AbstractFanInOutMetric {

    /**
     * Specification which kind of variables shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum FanType {
        // Classical parameters
        CLASSICAL_FAN_IN_GLOBALLY(false, false), CLASSICAL_FAN_IN_LOCALLY(true, false),
        CLASSICAL_FAN_OUT_GLOBALLY(false, false), CLASSICAL_FAN_OUT_LOCALLY(true, false),
        
        // Only feature code
        VP_FAN_IN_GLOBALLY(false, false), VP_FAN_IN_LOCALLY(true, false),
        VP_FAN_OUT_GLOBALLY(false, false), VP_FAN_OUT_LOCALLY(true, false),
        
        // Classical + feature code: DegreeCentrality Metric
        DEGREE_CENTRALITY_IN_GLOBALLY(false, true), DEGREE_CENTRALITY_IN_LOCALLY(true, true),
        DEGREE_CENTRALITY_OUT_GLOBALLY(false, true), DEGREE_CENTRALITY_OUT_LOCALLY(true, true);
        
        private boolean isLocal;
        private boolean isDegreeCentrality;
        
        /**
         * Sole constructor.
         * @param isLocal <tt>true</tt> if the metric measures fan-in/out only on the same file.
         * @param isDegreeCentrality <tt>true</tt> if this metric measures degree centrality.
         */
        private FanType(boolean isLocal, boolean isDegreeCentrality) {
            this.isLocal = isLocal;
            this.isDegreeCentrality = isDegreeCentrality;
        }
        
        /**
         * Returns whether degree centrality shall be measured.
         * @return <tt>true</tt> if degree centrality shall be measured.
         */
        public boolean isDegreeCentrality() {
            return isDegreeCentrality;
        }
    }
    
    public static final @NonNull Setting<@NonNull FanType> FAN_TYPE_SETTING
        = new EnumSetting<>("metric.fan_in_out.type", FanType.class, true, 
                FanType.CLASSICAL_FAN_IN_GLOBALLY, "Defines which type of fan in/out should be counted for a"
                    + " function.");
    
    private @NonNull FanType type;
    
    /**
     * Creates this metric, won't provide scattering degree, no check if a feature is defined in the variability model.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException if {@link #ND_TYPE_SETTING} is misconfigured.
     */
    public FanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Creates this metric, won't provide scattering degree, but checks if a feature is defined
     *     in the variability model.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @throws SetUpException if {@link #FAN_TYPE_SETTING} is misconfigured.
     */
    public FanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, null);
    }
    
    /**
     * Constructor to consider scattering degree of variables when degree centrality is measured.
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
    public FanInOutMetric(@NonNull Configuration config,
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
    public FanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);
        
        config.registerSetting(FAN_TYPE_SETTING);
        type = config.getValue(FAN_TYPE_SETTING);
        
        try {
            if (type.isLocal || type.name().contains("_OUT_")) {
                config.registerSetting(MetricSettings.FILTER_BY_FILES);
                List<String> filterList = config.getValue(MetricSettings.FILTER_BY_FILES);
                if (null != filterList && !filterList.isEmpty()) {
                    @Nullable Set<String> fileNameFilter = new HashSet<>();
                    for (String filePattern : filterList) {
                        if (File.separatorChar == '\\') {
                            // Make pattern platform independent (file names are generated from java.io.File objects)
                            fileNameFilter.add(filePattern.replace('/', File.separatorChar));
                        } else {
                            fileNameFilter.add(filePattern);
                        }
                    }
                    setFilter(fileNameFilter);
                }
            }
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + MetricSettings.FILTER_BY_FILES.getKey(), exc);
        }
        
        checkVariabilityWeights(type.isDegreeCentrality, type);
    }

    @Override
    protected double computeResult(@NonNull AbstractFanInOutVisitor visitor, CodeFunction function) {
        return visitor.getResult(function.getName());
    }

    @Override
    // CHECKSTYLE:OFF checkstyle can't parse the annotations properly...
    protected @NonNull AbstractFanInOutVisitor createVisitor(@NonNull List<CodeFunction> functions,
        @Nullable VariabilityModel varModel, IVariableWeight weight) {
    // CHECKSTYLE:ON
        
        return new FanInOutVisitor(functions, varModel, type, weight);
    }

    @Override
    public @NonNull String getResultName() {
        StringBuffer resultName = new StringBuffer();
        switch (type) {
        // Classical
        case CLASSICAL_FAN_IN_GLOBALLY:
            // falls through
        case CLASSICAL_FAN_IN_LOCALLY:
            resultName.append("Classical Fan-In");
            break;
        case CLASSICAL_FAN_OUT_GLOBALLY:
            // falls through
        case CLASSICAL_FAN_OUT_LOCALLY:
            resultName.append("Classical Fan-Out");
            break;
       
        // Variation point
        case VP_FAN_IN_GLOBALLY:
            // falls through
        case VP_FAN_IN_LOCALLY:
            resultName.append("VP Fan-In");
            break;
        case VP_FAN_OUT_GLOBALLY:
            // falls through
        case VP_FAN_OUT_LOCALLY:
            resultName.append("VP Fan-Out");
            break;
        
        // Variation point
        case DEGREE_CENTRALITY_IN_GLOBALLY:
            // falls through
        case DEGREE_CENTRALITY_IN_LOCALLY:
            resultName.append("DC Fan-In");
            break;
        case DEGREE_CENTRALITY_OUT_GLOBALLY:
            // falls through
        case DEGREE_CENTRALITY_OUT_LOCALLY:
            resultName.append("DC Fan-Out");
            break;
        default:
            resultName.append("Unspecified Fan-In/Out metric");
            break;
        }
        
        resultName.append("(");
        resultName.append((type.isLocal) ? "local" : "global");
        resultName.append(")");
        resultName.append(getWeightsName());
        
        return NullHelpers.notNull(resultName.toString());
    }
}
