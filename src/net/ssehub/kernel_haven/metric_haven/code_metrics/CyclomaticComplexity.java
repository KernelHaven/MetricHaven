package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.metric_haven.code_metrics.CyclomaticComplexity.CCType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.McCabeVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
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
     * @param varModel The component to get the variability model from. If not <tt>null</tt>
     *     {@link CCType#VARIATION_POINTS} and {@link CCType#ALL} will check if at least one variable of the variability
     *     model is involved in {@link net.ssehub.kernel_haven.code_model.ast.CppBlock#getCondition()} expressions to
     *     treat a {@link CppBlock} as variation point.
     * @param buildModel May be <tt>null</tt> as it is not used by this metric.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * @param measuredCode Specifies whether to measure McCabe for classical code, on variation points, or on both.
     * @throws UnsupportedMetricVariationException In case that classical code should be measured but a different
     *     {@link IVariableWeight} than {@link NoWeight#INSTANCE} was specified.
     */
    @PreferedConstructor
    CyclomaticComplexity(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight, @NonNull CCType measuredCode) throws UnsupportedMetricVariationException {
        super(varModel, buildModel, weight);
        this.measuredCode = measuredCode;
        
        if (measuredCode == CCType.MCCABE && weight != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), weight);
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
