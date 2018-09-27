package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.TanglingVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Sums up all tangling values of all CPP blocks in a function.
 * @author El-Sharkawy
 *
 */
public class TanglingDegree extends AbstractFunctionMetric<TanglingVisitor> {

    /**
     * Creates a new TanglingDegree metric.
     * @param params The parameters for creating this metric.
     * 
     * @throws SetUpException If creating the metric fails.
     */
    TanglingDegree(@NonNull MetricCreationParameters params) throws SetUpException {
        super(params);
        
        // All weights are always supported
        
        init();
    }

    @Override
    // CHECKSTYLE:OFF
    protected @NonNull TanglingVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
    // CHECKSTYLE:ON
        
        return new TanglingVisitor(varModel, weight);
    }

    @Override
    protected Number computeResult(@NonNull TanglingVisitor functionVisitor, CodeFunction func) {
        return functionVisitor.getResult();
    }

    @Override
    public @NonNull String getMetricName() {
        return "TD";
    }

}
