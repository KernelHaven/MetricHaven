package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.LoCVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A metric, which measures the amount of delivered Lines of Code (dLoC) per function. More precisely, this metric
 * measures the number of statements within a function which should be a good approximation for dLoC.
 * @author El-Sharkawy
 *
 */
public class DLoC extends AbstractFunctionMetric<LoCVisitor> {

    /**
     * Specification which kind of LoC-metric shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum LoFType {
        DLOC, LOF, PLOF;
    }

    private @NonNull LoFType type;
    
    /**
     * Creates a new DLoC metric, which can also be created by the {@link MetricFactory}.
     * 
     * @param params The metric creation parameters.
     * 
     * @throws UnsupportedMetricVariationException In case that not {@link NoWeight} was used (this metric does not
     *     support any weights).
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    DLoC(@NonNull MetricCreationParameters params) throws UnsupportedMetricVariationException, SetUpException {
        
        super(params);
        this.type = params.getMetricSpecificSettingValue(LoFType.class);            
        
        if (params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        init();
    }

    @Override
    protected @NonNull LoCVisitor createVisitor(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight) {
        
        // DLoC does not consider any IVariableWeights
        return new LoCVisitor(varModel);
    }

    @Override
    protected Number computeResult(@NonNull LoCVisitor functionVisitor, CodeFunction func) {
        Number result;
        switch(type) {
        case DLOC:
            result = functionVisitor.getDLoC();
            break;
        case LOF:
            result = functionVisitor.getLoF();
            break;
        case PLOF:
            result = functionVisitor.getPLoF();
            break;
        default:
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-analysis: ",
                net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LOC_TYPE_SETTING.getKey(),
                "=", type.name());
            result = null;
            break;
        }
        
        return result;
    }
    
    @Override
    public @NonNull String getMetricName() {
        String resultName;
        switch(type) {
        case DLOC:
            resultName = "LoC";
            break;
        case LOF:
            resultName = "LoF";
            break;
        case PLOF:
            resultName = "PLoF";
            break;
        default:
            resultName = "Unsupported metric specified";
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-analysis: ",
                net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LOC_TYPE_SETTING.getKey(),
                "=", type.name());
            break;
        }
        
        return resultName;
    }

}
