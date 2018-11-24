package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionCall;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionLocation;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measurement of various fan-in/fan-out metrics based on function calls.
 * 
 * @author El-Sharkawy
 * @author Adam
 */
public class FanInOut extends AbstractFunctionMetric<FanInOutVisitor> {

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
         * @return <tt>true</tt> if degree centrality shall be measured and {@link IVariableWeight}s may be used.
         */
        public boolean isDegreeCentrality() {
            return isDegreeCentrality;
        }
        
        /**
         * Returns whether the metric measures only local fan-in/-out calls within a file.
         * @return <tt>true</tt> if only local calls within a files are measured.
         */
        public boolean isLocal() {
            return isDegreeCentrality;
        }
    }
    
    private @NonNull FanType type;
    private @NonNull FunctionMap functions;
    private IVariableWeight weight;
    private @Nullable VariabilityModel varModel;
    private @NonNull VariableFinder varFinder;
    
    /**
     * Creates a new {@link FanInOut}-metric instance.
     * 
     * @param params The parameters for creating this metric.
     * 
     * @throws UnsupportedMetricVariationException In case that classical code should be measured but a different
     *     {@link IVariableWeight} than {@link NoWeight#INSTANCE} was specified.
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    FanInOut(@NonNull MetricCreationParameters params) throws UnsupportedMetricVariationException, SetUpException {
        
        super(params);
        this.type = params.getMetricSpecificSettingValue(FanType.class);
        FunctionMap fMap = params.getFunctionMap();
        if (fMap == null) {
            throw new SetUpException("FanInOutMetric needs function FunctionMap, but was null");
        }
        this.functions = fMap;
        
        if (!type.isDegreeCentrality && params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        varFinder = new VariableFinder();
        
        init();
    }

    @Override
    // CHECKSTYLE:OFF
    protected @NonNull FanInOutVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
    // CHECKSTYLE:ON
        
        this.weight = weight;
        this.varModel = varModel;
        return new FanInOutVisitor(varModel);
    }

    // CHECKSTYLE:OFF
    @Override
    protected Number computeResult(@NonNull FanInOutVisitor functionVisitor, CodeFunction func) {
    // CHECKSTYLE:ON
        List</*@NonNull*/ FunctionCall> functionCalls = functions.getFunctionCalls(func.getName());
        // TODO: @NonNull is commented out, because jacoco fails (probably https://github.com/jacoco/jacoco/issues/585)
        
        // Compute desired values
        boolean validResult = true;
        long result = 0;
        if (null != functionCalls) {
            for (FunctionCall call : functionCalls) {
                switch (type) {
                
                // CALLED functions for a specified function (source)
                case CLASSICAL_FAN_OUT_GLOBALLY:
                    // falls through
                case CLASSICAL_FAN_OUT_LOCALLY:
                    // Measures (locally/globally) the number of CALLED functions for a specified function
                    if (isDesiredFunction(call.getSource(), func)) {
                        result += 1;
                    }
                    break;
                case VP_FAN_OUT_GLOBALLY:
                    // falls through
                case VP_FAN_OUT_LOCALLY:
                    // Measures the number of CALLED functions for a specified function, which have a different PC
                    if (isDesiredFunction(call.getSource(), func) && !haveSamePC(call.getSource(), func)) {
                        result += 1;
                    }
                    break;
                case DEGREE_CENTRALITY_OUT_GLOBALLY:
                    // falls through
                case DEGREE_CENTRALITY_OUT_LOCALLY:
                    // Measures (locally/globally) the number of CALLED functions for a specified function
                    if (isDesiredFunction(call.getSource(), func)) {
                        result += complexityOfCall(call.getTarget());
                    }
                    break;
                    
                // Functions CALLING the specified function (target)
                case CLASSICAL_FAN_IN_GLOBALLY:
                    // falls through
                case CLASSICAL_FAN_IN_LOCALLY:
                    // Measures (locally/globally) the number of CALLING functions for a specified function
                    if (isDesiredFunction(call.getTarget(), func)) {
                        result += 1;
                    }
                    break;
                case VP_FAN_IN_GLOBALLY:
                    // falls through
                case VP_FAN_IN_LOCALLY:
                    // Measures the number of CALLING functions for a specified function, which have a different PC
                    if (isDesiredFunction(call.getTarget(), func) && !haveSamePC(call.getTarget(), func)) {
                        result += 1;
                    }
                    break;
                case DEGREE_CENTRALITY_IN_GLOBALLY:
                    // falls through
                case DEGREE_CENTRALITY_IN_LOCALLY:
                    // Measures (locally/globally) the number of CALLED functions for a specified function
                    if (isDesiredFunction(call.getTarget(), func)) {
                        result += complexityOfCall(call.getSource());
                    }
                    break;
                
                default:
                    LOGGER.logError2("Unsupported operation ", type.name(), " for visitor ", getClass().getName());
                    validResult = false;
                    break;
                }
            }
        }
        
        return validResult ? result : null;
    }
    
    /**
     * Checks if a participant of a function call (caller or callee) should be measured depending on the settings and
     * the currently measured function. Also performs a global/local check automatically.
     * @param callParticipant The role of the measured <tt>function</tt> within the function call:
     *     {@link FunctionCall#getSource()} if we want to measure callees (FanOut-metrics)
     *     {@link FunctionCall#getTarget()} if we want to measure callers (FanIn-metrics)
     * @param func The measured function.
     * @return <tt>true</tt> if the {@link FunctionCall} should be measured (somehow), <tt>false</tt> otherwise.
     */
    private boolean isDesiredFunction(FunctionLocation callParticipant, CodeFunction func) {
        return callParticipant.getName().equals(func.getName())
            && (!type.isLocal || callParticipant.getFile().equals(func.getSourceFile().getPath()));
    }
    
    /**
     * Checks if a participant of a function call (caller or callee) has the same presence condition as the measured
     * function.
     * @param callParticipant The opposite role of the measured <tt>function</tt> within the function call:
     *     {@link FunctionCall#getTarget()} if we want to measure callees (FanOut-metrics)
     *     {@link FunctionCall#getSource()} if we want to measure callers (FanIn-metrics)
     * @param func The measured function.
     * @return <tt>true</tt> if both elements have the same (equal) presence condition, <tt>false</tt> otherwise.
     */
    private boolean haveSamePC(FunctionLocation callParticipant, CodeFunction func) {
        return callParticipant.getPresenceCondition().equals(func.getFunction().getPresenceCondition());
    }

    /**
     * Checks if the given name is defined in the variability model.
     * @param variableName The CPP element to check.
     * @return <tt>true</tt> if no variability model was passed to this visitor or if the element is defined in the
     *     variability model.
     */
    private boolean isFeature(String variableName) {
        return (null == varModel || notNull(varModel).getVariableMap().containsKey(variableName));
    }
    
    /**
     * Measures the degree complexity for a participant of a function call (caller or callee).
     * Measures the complexity of features involved of the presence condition plus 1 for calls depending on at least one
     * feature.
     * @param callParticipant The measured item.
     * @return The configuration complexity result for the specified function (&ge; 0).
     */
    private long complexityOfCall(FunctionLocation callParticipant) {
        long result = 0;
        
        varFinder.clear();
        callParticipant.getPresenceCondition().accept(varFinder);
        boolean containsFeature = false;
        for (Variable variable : varFinder.getVariables()) {
            String varName = variable.getName();
            /* By default weight will count unknown elements with 1, but this is already counted by 
             * non-DegreeCentrality-metrics. Therefore, ensure that we count only feature of the varModel.
             */
            if (isFeature(varName)) {
                if (null != callParticipant.getFile()) {
                    result += weight.getWeight(variable.getName(), callParticipant.getFile());
                } else {
                    result += weight.getWeight(variable.getName());
                }
                containsFeature = true;
            }
        }
        if (containsFeature) {
            // Count also each connection embedded in a variation point
            result += 1;
        }
        
        return result;
    }
    
    @Override
    public @NonNull String getMetricName() {
        StringBuilder resultName = new StringBuilder();
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
        
        return NullHelpers.notNull(resultName.toString());
    }
    
    @Override
    public boolean isFilterable() {
        /*
         * Filterable variations are:
         * - Local metrics require only the complete file for its computation
         * - FanOut metrics can be computed by means of the Function map and won't need to run over all ASTs
         * 
         * Non filterable variations are:
         * - Global FanIn metric, they need to gather all function calls in other functions
         */
        return type.isLocal() || type.name().contains("_OUT_");
    }

}
