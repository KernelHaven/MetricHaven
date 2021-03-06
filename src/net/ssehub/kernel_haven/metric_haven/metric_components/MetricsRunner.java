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
package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByLineFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByPathAndNameFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.FunctionMapCreator;
import net.ssehub.kernel_haven.metric_haven.filter_components.OrderedCodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeEstimator;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.VariabilityCounter;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs metrics.
 * 
 * @author El-Sharkawy
 * @author Adam
 */
public class MetricsRunner extends PipelineAnalysis {
    
    public static final @NonNull Setting<@NonNull Boolean> RUN_ATOMIC_METRICS = new Setting<>("metrics.run.atomic_set",
        Type.BOOLEAN, true, "false", "If turned on, only the atomic set of metrics is executed. We treat all"
                + "metric variations as atomic set that either are the metric without any weight or with at"
                + "most one weight to avoid all combinations of weights.");
    
    /**
     * Switch to disable default behavior to ignore configuration and run coded selection of metrics.
     * By default this should be set to <tt>false</tt> to process the configuration. 
     */
    private boolean runAtomicSet;
    
    /**
     * Whether a line filter has been configured. If this is <code>true</code>, a {@link CodeFunctionByLineFilter}
     * should be added in the pipeline.
     */
    private boolean lineFilter;
    
    /**
     * Specifies whether a function name filter should be used (only checked and used if not already a line filter was
     * specified before). If this is <code>true</code>, a {@link CodeFunctionByPathAndNameFilter} should be added in 
     * the pipeline.
     */
    private boolean nameFilter;
    
    /**
     * Single constructor to instantiate and execute all variations of a single metric-analysis class.
     * 
     * @param config The global configuration.
     * 
     * @throws SetUpException In case of problems with the configuration.
     */
    public MetricsRunner(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        lineFilter = false;
        nameFilter = false;
        config.registerSetting(MetricSettings.LINE_NUMBER_SETTING);
        List<String> value = config.getValue(MetricSettings.LINE_NUMBER_SETTING);
        if (!value.isEmpty()) {
            lineFilter = true;
        } else {
            config.registerSetting(MetricSettings.FILTER_BY_FUNCTIONS);
            value = config.getValue(MetricSettings.FILTER_BY_FUNCTIONS);
            if (!value.isEmpty()) {
                nameFilter = true;
            }
        }
        
        try {
            config.registerSetting(RUN_ATOMIC_METRICS);
            runAtomicSet = config.getValue(RUN_ATOMIC_METRICS);
        } catch (SetUpException exc) {
            throw new SetUpException("Could not load configuration setting " + RUN_ATOMIC_METRICS.getKey());
        }
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        /*
         * Unfiltered:
         * 
         * Code Model --+---+-> OrderedCodeFunctionFilter -> Split -+-> FunctionMapCreator -> |
         *              |   |                                       |                         |
         *              |   +-> |                                   +-----------------------> |
         *              |       | VariabilityCounter -+                                       |
         *              |   +-> |                     +-------------------------------------> | CodeMetricsRunner
         *              |   |                                                                 |
         * Var Model ----+--+---------------------------------------------------------------> |
         *              ||                                                                    |
         * Build Model ---+-----------------------------------------------------------------> |
         *              |||                                                                   |
         *              ||+-> |                                                               |
         *              |+--> | FeatureSizeEstimator ---------------------------------------> |
         *              +---> |
         */
        
        /*
         * Filtered:
         * 
         * Code Model --+---+-> OrderedCodeFunctionFilter -> Split -+-> FunctionMapCreator -------> |
         *              |   |                                       |                               |
         *              |   +-> |                                   +-> CodeFunctionByLineFilter -> |
         *              |       | VariabilityCounter -+                                             |
         *              |   +-> |                     +-------------------------------------------> | CodeMetricsRunner
         *              |   |                                                                       |
         * Var Model ----+--+---------------------------------------------------------------------> |
         *              ||                                                                          |
         * Build Model ---+-----------------------------------------------------------------------> |
         *              |||                                                                         |
         *              ||+-> |                                                                     |
         *              |+--> | FeatureSizeEstimator ---------------------------------------------> |
         *              +---> |
         */
        
        AnalysisComponent<CodeFunction> orderedFunctionFilter = new OrderedCodeFunctionFilter(config, getCmComponent());
        SplitComponent<CodeFunction> split = new SplitComponent<>(config, orderedFunctionFilter);
        
        AnalysisComponent<ScatteringDegreeContainer> variabilityCounter
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        AnalysisComponent<FunctionMap> functionMapCreator = new FunctionMapCreator(config,
            split.createOutputComponent());
        
        AnalysisComponent<FeatureSizeContainer> featureSizeCreator = new FeatureSizeEstimator(config,
                getVmComponent(), getCmComponent(), getBmComponent());
        
        @NonNull AnalysisComponent<CodeFunction> functionInput = split.createOutputComponent();
        if (lineFilter) {
            functionInput = new CodeFunctionByLineFilter(config, functionInput);
        } else if (nameFilter) {
            functionInput = new CodeFunctionByPathAndNameFilter(config, functionInput);
        }
        
        AnalysisComponent<?> metricAnalysis;  
        if (!runAtomicSet) {
            // Default case
            metricAnalysis = new CodeMetricsRunner(config, functionInput, getVmComponent(),
                getBmComponent(), variabilityCounter, functionMapCreator, featureSizeCreator);
        } else {
            LOGGER.logInfo2("MetricHaven executes atomic set and ignores further metric-specific configuration "
                + "settings.");
            metricAnalysis = new IndividualCodeMetricsRunner(config, functionInput, getVmComponent(),
                getBmComponent(), variabilityCounter, functionMapCreator, featureSizeCreator);
        }
        
        return metricAnalysis;
    }
    
}
