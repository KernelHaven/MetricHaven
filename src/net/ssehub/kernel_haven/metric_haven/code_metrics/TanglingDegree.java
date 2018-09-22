package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
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
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param buildModel May be <tt>null</tt> as it is not used by this metric.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     */
    @PreferedConstructor
    TanglingDegree(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight) {
        super(varModel, buildModel, weight);
        // Always all weights are supported

        init();
    }

    @Override
    protected @NonNull TanglingVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
        
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
