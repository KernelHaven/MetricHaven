package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.McCabeVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
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
public class CyclomaticComplexity extends AbstractFunctionMetric<McCabeVisitor> {

    /**
     * Specification which kind of Cyclomatic Complexity shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum CCType {
        MCCABE, VARIATION_POINTS, ALL;
    }
    
    private @NonNull CCType measuredCode;

    /**
     * Creates a new DLoC metric, which can also be created by the {@link MetricFactory}.
     * 
     * @param params The parameters for creating this metric.
     * 
     * @throws UnsupportedMetricVariationException In case that classical code should be measured but a different
     *     {@link IVariableWeight} than {@link NoWeight#INSTANCE} was specified.
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    @PreferedConstructor
    CyclomaticComplexity(@NonNull MetricCreationParameters params) throws UnsupportedMetricVariationException,
        SetUpException {
        
        super(params);
        this.measuredCode = params.getMetricSpecificSettingValue(CCType.class);
        
        if (measuredCode == CCType.MCCABE && params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        init();
    }

    @Override
    protected McCabeVisitor createVisitor(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight) {
        
        return (measuredCode == CyclomaticComplexity.CCType.MCCABE) ? new McCabeVisitor(varModel)
            : new McCabeVisitor(varModel, weight);
    }

    @Override
    protected Number computeResult(McCabeVisitor functionVisitor, CodeFunction func) {
        Integer result;
        switch (measuredCode) {
        case MCCABE:
            result = functionVisitor.getClassicCyclomaticComplexity();
            break;
        case VARIATION_POINTS:
            result = functionVisitor.getVariabilityCyclomaticComplexity();
            break;
        case ALL:
            result = functionVisitor.getCombinedCyclomaticComplexity();
            break;
        default:
            // Indicate that something went wrong.
            result = null;
            LOGGER.logError("Unknown code type specified for ", getClass().getName());
            break;
        }
        
        return result;
    }

    @Override
    public @NonNull String getMetricName() {
        String resultName;
        switch (measuredCode) {
        case MCCABE:
            resultName = "McCabe";
            break;
        case VARIATION_POINTS:
            resultName = "CC on VPs";
            break;
        case ALL:
            resultName = "McCabe + CC on VPs";
            break;
        default:
            resultName = "Unknown Cyclomatic Complexity";
            break;
        }
        
        return resultName;
    }

}
