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
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.StatementCountLoCVisitor;
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
public class LoCMetric extends AbstractFunctionMetric<StatementCountLoCVisitor> {

    /**
     * Specification which kind of LoC-metric shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum LoCType {
        // Statement-based
        /**
          * Statement Count Of Code: Lines of Code based on Statements.
          */
        SCOC,
        /**
          * Statement Count Of Feature code: Lines of Feature Code based on Statements.
          */
        SCOF,
        /**
         * Percentage of Statement Count Of Feature code: SCOC / SCOF.
         */     
        PSCOF,
        
        // Lines-based
        /**
          * Statement Count Of Code: Lines of Code based on Statements.
          */
        LOC,
        /**
          * Statement Count Of Feature code: Lines of Feature Code based on Statements.
          */
        LOF,
        /**
         * Percentage of Statement Count Of Feature code: SCOC / SCOF.
         */     
        PLOF;
    }

    private @NonNull LoCType type;
    
    /**
     * Creates a new DLoC metric, which can also be created by the {@link MetricFactory}.
     * 
     * @param params The metric creation parameters.
     * 
     * @throws UnsupportedMetricVariationException In case that not {@link NoWeight} was used (this metric does not
     *     support any weights).
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoCType} is used for {@link CyclomaticComplexity}.
     */
    LoCMetric(@NonNull MetricCreationParameters params) throws UnsupportedMetricVariationException, SetUpException {
        
        super(params);
        this.type = params.getMetricSpecificSettingValue(LoCType.class);            
        
        if (params.getWeight() != NoWeight.INSTANCE) {
            throw new UnsupportedMetricVariationException(getClass(), params.getWeight());
        }
        
        init();
    }

    @Override
    protected @NonNull StatementCountLoCVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {

        StatementCountLoCVisitor visitor;
        
        switch(type) {
        case SCOC:
         // falls through
        case SCOF:
         // falls through
        case PSCOF:
            visitor = new StatementCountLoCVisitor(varModel);
            break;
        case LOF:
            // falls through
        case LOC:
            // falls through
        case PLOF:
            visitor = new LoCVisitor(varModel);
            break;
        default:
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-analysis: ",
                MetricSettings.LOC_TYPE_SETTING.getKey(), "=", type.name(), " using ",
                StatementCountLoCVisitor.class.getName());
            visitor = new StatementCountLoCVisitor(varModel);
            break;
        }
        
        return visitor;
    }

    @Override
    protected Number computeResult(@NonNull StatementCountLoCVisitor functionVisitor, CodeFunction func) {
        Number result;
        switch(type) {
        case LOC:
            // falls through
        case SCOC:
            result = functionVisitor.getDLoC();
            break;
        case LOF:
            // falls through
        case SCOF:
            result = functionVisitor.getLoF();
            break;
        case PLOF:
            // falls through
        case PSCOF:
            result = functionVisitor.getPLoF();
            break;
        default:
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-analysis: ",
                    MetricSettings.LOC_TYPE_SETTING.getKey(), "=", type.name());
            result = null;
            break;
        }
        
        return result;
    }
    
    @Override
    public @NonNull String getMetricName() {
        String resultName;
        switch(type) {
        case SCOC:
            resultName = "SCoC";
            break;
        case SCOF:
            resultName = "SCoF";
            break;
        case PSCOF:
            resultName = "PSCoF";
            break;
        case LOC:
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
                MetricSettings.LOC_TYPE_SETTING.getKey(), "=", type.name());
            break;
        }
        
        return resultName;
    }

}
