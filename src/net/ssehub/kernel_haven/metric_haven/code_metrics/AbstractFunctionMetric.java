package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFunctionVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Super class to create a metric which computes a value for a metric a single code function only.
 * @author El-Sharkawy
 * @param <V> The internally used function visitor to compute results.
 */
public abstract class AbstractFunctionMetric<V extends AbstractFunctionVisitor> {
    
    /**
     * Shorthand for the global singleton logger.
     */
    protected static final Logger LOGGER = Logger.get();
    
    private @NonNull V functionVisitor;
    
    /**
     * Sole constructor, to create a new code function metric. Creates also an appropriate function visitor.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     */
    AbstractFunctionMetric(@Nullable VariabilityModel varModel, @NonNull IVariableWeight weight) {
        functionVisitor = createVisitor(varModel, weight);
    }
    
    /**
     * Stateless measurement of a code function (public interface). 
     * @param func The code function to measure.
     * 
     * @return The measured result &ge; 0.
     */
    public Number compute(CodeFunction func) {
        functionVisitor.reset();
        func.getFunction().accept(functionVisitor);
        return computeResult(functionVisitor);
    }

    /**
     * Creates the visitor to use.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * 
     * @return The visitor to be used by the metric.
     */
    protected abstract @NonNull V createVisitor(@Nullable VariabilityModel varModel, @NonNull IVariableWeight weight);
    
    /**
     * Collects the measured value and may performs a post processing of the values.
     * @param functionVisitor The visitor created by {@link #createVisitor(VariabilityModel, IVariableWeight)}.
     * 
     * @return The value to be returned by {@link #compute(CodeFunction)}, or <tt>null</tt> if no valid value could be
     *     computed (should not happen).
     */
    protected abstract Number computeResult(@NonNull V functionVisitor);
    
    /**
     * The name to display to users for the output of this component. For example, this will be used to name the
     * output tables.
     * 
     * @return The name describing the output of this component.
     */
    public abstract @NonNull String getResultName();
}
