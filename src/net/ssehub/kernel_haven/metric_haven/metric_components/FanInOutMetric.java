package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
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
public class FanInOutMetric extends AbstractFunctionVisitorBasedMetric<FanInOutVisitor> {

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
    private @Nullable Set<String> fileNameFilter;
    
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
                    fileNameFilter = new HashSet<>();
                    for (String filePattern : filterList) {
                        if (File.separatorChar == '\\') {
                            // Make pattern platform independent (file names are generated from java.io.File objects)
                            fileNameFilter.add(filePattern.replace('/', File.separatorChar));
                        } else {
                            fileNameFilter.add(filePattern);
                        }
                    }
                }
            }
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + MetricSettings.FILTER_BY_FILES.getKey(), exc);
        }
        
        checkVariabilityWeights(type.isDegreeCentrality, type);
    }
    
    /**
     * Determines whether the given {@link CodeFunction} shall be rejected (<tt>false</tt>)
     * or be accepted (<tt>true</tt>) by this filter.
     * @param func A code function to test if it shall be processed.
     * @return <tt>true</tt>: {@link CodeFunction} shall be kept; <tt>false</tt>: {@link CodeFunction} shall be skipped.
     */
    private boolean filter(CodeFunction func) {
        // Do not filter code functions by default
        boolean accept = true;
        if (null != fileNameFilter) {
            // Filter code functions by file/path if a filter is defined
            accept = fileNameFilter.contains(func.getSourceFile().getPath().getPath());
        }
        
        return accept;
    }

    @Override
    protected final @NonNull FanInOutVisitor createVisitor(@Nullable VariabilityModel varModel) {
        throw new IllegalArgumentException("Wrong factory for visitor creation called: createVisitor(VariabilityModel),"
            + " but should be createVisitor(List<CodeFunction>, VariabilityModel, IVariableWeight)");
    }
    
    @Override
    protected final double computeResult(@NonNull FanInOutVisitor visitor) {
        throw new IllegalArgumentException("Wrong computation method called: computeResult(VariabilityModel),"
            + " but should be computeResult(List<CodeFunction>, VariabilityModel)");
    }
    
    /**
     * This method computes the result based on the results of the visitor created by
     *     {@link #createVisitor(List, VariabilityModel)}.
     * @param visitor The visitor after visitation of a single function.
     * @param function The function for which we compute the metric result for.
     * @return The computed value or {@link Double#NaN} if no result could be computed an this metric should not
     *     mention the result.
     */
    private double computeResult(@NonNull FanInOutVisitor visitor, CodeFunction function) {
        return visitor.getResult(function.getName());
    }

    
    /**
     * Creates the visitor to be used by the metric.
     * @param functions All gathered functions.
     * @param varModel Optional, if not <tt>null</tt> this visitor should use the variability model to check if at least
     *     one variable of the variability model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight {@link #getWeighter()}
     * @return The visitor to compute the fan-in fan-out metric by the inherited metric analysis.
     */
    // CHECKSTYLE:OFF checkstyle can't parse the annotations properly...
    private @NonNull FanInOutVisitor createVisitor(@NonNull List<CodeFunction> functions,
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
    
    @Override
    protected final void execute() {
        long time = System.currentTimeMillis();
        
        AnalysisComponent<VariabilityModel> vmComponent = getVMComponent();
        VariabilityModel varModel = (null != vmComponent) ? vmComponent.getNextResult() : null;
        createWeight(varModel);
        
        CodeFunction function;
        List<CodeFunction> functions = new LinkedList<>();
        while ((function = getCodeFunctionFinder().getNextResult()) != null)  {
            functions.add(function);
        }
        // Looses threading for sub analyses, but should not be a big issue
        
        // Gather function calls for all functions
        FanInOutVisitor visitor = createVisitor(functions, varModel, getWeighter());
        for (CodeFunction func : functions) {
            if (filter(func)) {
                func.getFunction().accept(visitor);
            }
        }
        
        logDuration(System.currentTimeMillis() - time, "Collection of function calls finished in ");
        
        // Compute and report all results
        for (CodeFunction func : functions) {
            if (filter(func)) {
                double result = computeResult(visitor, func);
                if (Double.isNaN(result)) {
                    return;
                }
                
                Function functionAST = func.getFunction();
                File cFile = func.getSourceFile().getPath();
                File includedFile = cFile.equals(functionAST.getSourceFile()) ? null : functionAST.getSourceFile();
                addResult(new MetricResult(cFile, includedFile, functionAST.getLineStart(), func.getName(), result));
            }
        }
        
        logDuration(System.currentTimeMillis() - time, "Analysis of ", getResultName(), " finished in ");
    }
}
