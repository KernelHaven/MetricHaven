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
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
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
     * Specification which kind of TD-metric shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum TDType {
        /**
         * Considers all variations points, also else parts without a visible expression.
         */
        TD_ALL,
        
        /**
         * Considers only variation points with visible conditions.
         */
        TD_NO_ELSE;
    }
    
    private @NonNull TDType type;
    
    /**
     * Creates a new TanglingDegree metric.
     * @param params The parameters for creating this metric.
     * 
     * @throws SetUpException If creating the metric fails.
     */
    TanglingDegree(@NonNull MetricCreationParameters params) throws SetUpException {
        super(params);
        
        this.type = params.getMetricSpecificSettingValue(TDType.class);  
        
        // All weights are always supported
        
        init();
    }

    @Override
    // CHECKSTYLE:OFF
    protected @NonNull TanglingVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
    // CHECKSTYLE:ON
        
        return new TanglingVisitor(varModel, weight, type);
    }

    @Override
    protected Number computeResult(@NonNull TanglingVisitor functionVisitor, CodeFunction func) {
        return functionVisitor.getResult();
    }

    @Override
    public @NonNull String getMetricName() {
        String resultName;
        switch(type) {
        case TD_ALL:
            resultName = "Full-TD";
            break;
        case TD_NO_ELSE:
            resultName = "Visible-TD";
            break;
        default:
            resultName = "Unsupported metric specified";
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-analysis: ",
                MetricSettings.LOC_TYPE_SETTING.getKey(), "=", type.name());
            break;
        }
        
        return resultName;
    }

}
