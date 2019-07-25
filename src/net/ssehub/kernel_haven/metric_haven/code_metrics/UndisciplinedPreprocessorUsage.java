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
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.UndisciplinedCPPUsageVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Detects <a href="https://www.infosun.fim.uni-passau.de/publications/docs/MRF+17.pdf">undisciplined preprocessor usage
 * </a> and sums up how often this appears in a function.
 * @author El-Sharkawy
 *
 */
public class UndisciplinedPreprocessorUsage extends AbstractFunctionMetric<UndisciplinedCPPUsageVisitor> {

    /**
     * Creates a new TanglingDegree metric.
     * @param params The parameters for creating this metric.
     * 
     * @throws SetUpException If creating the metric fails.
     */
    UndisciplinedPreprocessorUsage(@NonNull MetricCreationParameters params) throws SetUpException {
        super(params);
        
        if (params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        init();
    }

    @Override
    // CHECKSTYLE:OFF
    protected @NonNull UndisciplinedCPPUsageVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
    // CHECKSTYLE:ON
        
        return new UndisciplinedCPPUsageVisitor();
    }

    @Override
    protected Number computeResult(@NonNull UndisciplinedCPPUsageVisitor functionVisitor, CodeFunction func) {
        return functionVisitor.getResult();
    }

    @Override
    public @NonNull String getMetricName() {
        return "Undisciplined CPP";
    }

}
