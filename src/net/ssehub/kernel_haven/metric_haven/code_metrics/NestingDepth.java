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
import net.ssehub.kernel_haven.metric_haven.code_metrics.LoCMetric.LoCType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.NestingDepthVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A metric, which measures the maximum/average nesting depth. These metrics are based on:
 * <ul>
 *     <li>Classical Nesting Depth as specified by <a href="https://dl.acm.org/citation.cfm?id=42168">[Conte et al.
 *         1986]</a></li>
 *     <li>Nesting Depth of Variation Points (e.g., used/specified by
 *         <a href="https://ieeexplore.ieee.org/abstract/document/6062078/">[Liebig et al. 2010]</a>, 
 *         <a href="https://link.springer.com/article/10.1007/s10664-015-9360-1">[Hunsen et al. 2016]</a>)</li>
 * </ul>
 * However, we did a small variation as we count the nested statements instead of the branching elements itself. This is
 * done to count the average for nested elements inside a function and also to avoid counting of nesting structures,
 * which do not have at least one nested element. 
 * @author El-Sharkawy
 *
 */
public class NestingDepth extends AbstractFunctionMetric<NestingDepthVisitor> {

    /**
     * Specification which kind of nesting depth metric shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum NDType {
        CLASSIC_ND_MAX(false), CLASSIC_ND_AVG(false),
        VP_ND_MAX(true), VP_ND_AVG(true),
        COMBINED_ND_MAX(true), COMBINED_ND_AVG(true);
        
        private boolean isVariabilityMetric;
        
        /**
         * Private constructor to specify whether this enums denotes a metric operating on variability information.
         * @param isVariabilityMetric <tt>true</tt> operates on variability, <tt>false</tt> classical code metric
         */
        private NDType(Boolean isVariabilityMetric) {
            this.isVariabilityMetric = isVariabilityMetric;
        }
        
        /**
         * Specifies whether the setting is used measure variability attributes and, thus, may be combined with a
         * variability weight.
         * @return <tt>true</tt> if it may be combined with a {@link IVariableWeight}, <tt>false</tt> otherwise.
         */
        public boolean isVariabilityMetric() {
            return isVariabilityMetric;
        }
    }

    private @NonNull NDType type;
    
    /**
     * Creates a new Nesting depth metric, which can also be created by the {@link MetricFactory}.
     * 
     * @param params The parameters for creating this metric.
     * 
     * @throws UnsupportedMetricVariationException In case that classical code structures shall be measured and a
     *     different {@link IVariableWeight} was specified than {@link NoWeight#INSTANCE}.
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoCType} is used for {@link CyclomaticComplexity}.
     */
    NestingDepth(@NonNull MetricCreationParameters params) throws UnsupportedMetricVariationException, SetUpException {
        
        super(params);
        this.type = params.getMetricSpecificSettingValue(NDType.class);

        if (!type.isVariabilityMetric && params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        init();
    }

    @Override
    // CHECKSTYLE:OFF
    protected @NonNull NestingDepthVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
    // CHECKSTYLE:ON
        return type.isVariabilityMetric ? new NestingDepthVisitor(varModel, weight)
            : new NestingDepthVisitor(varModel);
    }

    @Override
    protected Number computeResult(@NonNull NestingDepthVisitor functionVisitor, CodeFunction func) {
        double result;
        switch(type) {
        case CLASSIC_ND_MAX:
            result = functionVisitor.getClassicalNestingDepth(true);
            break;
        case CLASSIC_ND_AVG:
            result = functionVisitor.getClassicalNestingDepth(false);
            break;
        case VP_ND_MAX:
            result = functionVisitor.getVariationPointNestingDepth(true);
            break;
        case VP_ND_AVG:
            result = functionVisitor.getVariationPointNestingDepth(false);
            break;
        case COMBINED_ND_MAX:
            result = functionVisitor.getClassicalNestingDepth(true)
                + functionVisitor.getVariationPointNestingDepth(true);
            break;
        case COMBINED_ND_AVG:
            result = functionVisitor.getClassicalNestingDepth(false)
                + functionVisitor.getVariationPointNestingDepth(false);
            break;
        default:
            LOGGER.logError2("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                type.getClass().getSimpleName(), "=", type.name());
            result = Double.NaN;
            break;
        }
        
        return result;
    }

    @Override
    public @NonNull String getMetricName() {
        String resultName;
        switch(type) {
        case CLASSIC_ND_MAX:
            resultName = "Classic ND_Max";
            break;
        case CLASSIC_ND_AVG:
            resultName = "Classic ND_Avg";
            break;
        case VP_ND_MAX:
            resultName = "VP ND_Max";
            break;
        case VP_ND_AVG:
            resultName = "VP ND_Avg";
            break;
        case COMBINED_ND_MAX:
            resultName = "Combined ND_Max";
            break;
        case COMBINED_ND_AVG:
            resultName = "Combined ND_Avg";
            break;
        default:
            resultName = "Unsupported metric specified";
            LOGGER.logError2("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                type.getClass().getSimpleName(), "=", type.name());
            break;
        }
        
        return resultName;
    }

}
