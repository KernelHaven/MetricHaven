package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measurement of various fan-in/fan-out metrics based on function calls.
 * @author El-Sharkawy
 *
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
    @PreferedConstructor
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
        
        init();
    }

    @Override
    protected @NonNull FanInOutVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
        
        return new FanInOutVisitor(functions, varModel, type, weight);
    }

    @Override
    protected Number computeResult(@NonNull FanInOutVisitor functionVisitor, CodeFunction func) {
        return functionVisitor.getResult(func.getName());
    }

    @Override
    public @NonNull String getMetricName() {
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
        
        return NullHelpers.notNull(resultName.toString());
    }

}
