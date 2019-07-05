/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFunctionVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
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
    
    private @NonNull V functionVisitor; // will be initialized in init()
    
    private @NonNull IVariableWeight weight;
    
    private @NonNull MetricCreationParameters params;
    
    /**
     * Sole constructor, to create a new code function metric. Creates also an appropriate function visitor.
     * 
     * @param params The parameters for creating this metric.
     */
    @SuppressWarnings("null")
    AbstractFunctionMetric(@NonNull MetricCreationParameters params) {
        this.params = params;
        
        IVariableWeight weight = params.getWeight();
        if (weight == null) {
            throw new IllegalArgumentException("Weight is null");
        }
        this.weight = weight;
        
    }
    
    /**
     * Creates the visitor, must be called at the end of the constructor of inherited classes.
     * 
     * @throws SetUpException If the visitor could not be created due to a misconfiguration.
     */
    protected void init() throws SetUpException {
        functionVisitor = createVisitor(params.getVarModel(), params.getBuildModel(), weight);
    }
    
    /**
     * Allows optional (threaded) computations after initialization and before function-wise metric execution.
     */
    public void prepare() {}
    
    /**
     * Specifies whether a metric need a call of {@link #prepare()}.
     * This is done for optimization purpose to avoid that many metrics that need that method are called in same thread
     * and others that do not need that method are moved to other threads.
     * @return <tt>true</tt>{@link #prepare()} needs to be called before execution.
     */
    public boolean needsPreparation() {
        return false;
    }
    
    /**
     * Atomic measurement of a code function (public interface). 
     * @param func The code function to measure.
     * 
     * @return The measured result &ge; 0.
     */
    public Number compute(CodeFunction func) {
        functionVisitor.reset();
        func.getFunction().accept(functionVisitor);
        return computeResult(functionVisitor, func);
    }

    /**
     * Creates the visitor to use.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * @param buildModel Some metrics require a build model, for others this may be <tt>null</tt>.
     * 
     * @return The visitor to be used by the metric.
     * 
     * @throws SetUpException If the visitor could not be created due to a misconfiguration.
     */
    protected abstract @NonNull V createVisitor(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight) throws SetUpException;
    
    /**
     * Collects the measured value and may performs a post processing of the values.
     * @param functionVisitor The visitor created by {@link #createVisitor(VariabilityModel, IVariableWeight)}.
     * @param func The currently measured code function.
     * 
     * @return The value to be returned by {@link #compute(CodeFunction)}, or <tt>null</tt> if no valid value could be
     *     computed (should not happen).
     */
    protected abstract Number computeResult(@NonNull V functionVisitor, CodeFunction func);
    
    /**
     * The name of this metric (including the variation).
     * 
     * @return The name of this metric.
     */
    public abstract @NonNull String getMetricName();
    
    /**
     * Returns the {@link IVariableWeight}, if it is required by the metric outside of the
     * {@link AbstractFunctionVisitor}.
     * @return The used {@link IVariableWeight}, instead of <tt>null</tt> it may use {@link NoWeight#INSTANCE}.
     */
    protected @NonNull IVariableWeight getWeight() {
        return weight;
    }
    
    /**
     * The name to display to users for the output of this component. For example, this will be used to name the
     * output tables. This basically concatenates {@link #getMetricName()} and {@link IVariableWeight#getName()}.
     * 
     * @return The name describing the output of this component.
     */
    public final @NonNull String getResultName() {
        return (weight != NoWeight.INSTANCE)
            ? getMetricName() + " x " + weight.getName()
            : getMetricName();
    }
    
    /**
     * Returns whether this function metric need to run on all code functions or may run on a filtered sub set if
     * desired.
     * 
     * @return <tt>true</tt> if it may run on a filtered sub set, <tt>false</tt> if it needs always to be executed on
     *     all elements.
     */
    public boolean isFilterable() {
        return true;
    }
}
