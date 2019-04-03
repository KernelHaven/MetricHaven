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
package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.io.File;

import net.ssehub.kernel_haven.util.Logger;

/**
 * Specifies a weight for a variable of the variability model.
 * 
 * <h2>How to create new IVariableWeights</h2>
 * <ol>
 *   <li>Optionally: Create component computing required values for that weight and place it in package:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.filter_components}</li>
 *   <li>Create the new weight and place it in package:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights}</li>
 *   <li>Create configuration setting (and optionally an enumeration) in file:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings}</li>
 *   <li>Optionally consider caching of weights values, if values are re-computated dynamically:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.WeigthsCache}</li>
 *   <li>Add a static initialization method in:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory}</li>
 *   <li>Add the initialization method to the <tt>createAllCombinations</tt>-methods of:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory}</li>
 *   <li>Add the settings and and optional containers to:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters}</li>
 *   <li>Adapt the initialization methods of:<br/>
 *       {@link net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory}</li>
 *   <li>Adapt the <tt>execute</tt>-method of:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner}</li>
 *   <li>Consider settings in constructor, attributes and the <tt>loadSingleVariationSettings</tt>-method of :
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner}</li>
 *   <li>Configure the default metric-pipeline in:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.MetricsRunner}</li>
 *   <li>If you changed the constructor of
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner} than please also update the
 *       parameter list of the
 *       <tt>net.ssehub.kernel_haven.metric_haven.code_metrics.CodeMetricsRunnerTest.simpleTest</tt></li> 
 *   <li>Create test cases in ScenarioTests Plug-in</li>
 * </ol>
 * 
 * @author El-Sharkawy
 *
 */
public interface IVariableWeight {
    
    public static final Logger LOGGER = Logger.get();

    /**
     * Returns the weight for the specified variable.
     * @param variable An existing variable of the variability model, for which the weight shall be returned for.
     * @return A positive weight (&ge; 1), 1 denotes a neutral value.
     */
    public long getWeight(String variable);
    
    /**
     * Returns the weight for the specified variable and the currently measured code artifact.
     * @param variable An existing variable of the variability model, for which the weight shall be returned for.
     * @param codefile The currently measured code artifact (only required for some weights).
     * @return A positive weight (&ge; 1), 1 denotes a neutral value.
     */
    public default long getWeight(String variable, File codefile) {
        return getWeight(variable);
    }
    
    /**
     * Returns the name of the weight, may be used together with
     * {@link net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric#getResultName()}.
     * @return The name of activated weights.
     */
    public String getName();
}
