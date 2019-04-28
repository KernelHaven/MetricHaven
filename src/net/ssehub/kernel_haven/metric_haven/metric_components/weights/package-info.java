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
/**
 * Optional functions to weight variables for different characteristics.
 * 
 * <h2>How to create new IVariableWeights</h2>
 * <ol>
 *   <li>Optionally: Create component computing required values for that weight and place it in package:
 *       {@link net.ssehub.kernel_haven.metric_haven.filter_components}</li>
 *   <li>Create the new weight and place it in package:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights}</li>
 *   <li>Create configuration setting (and optionally an enumeration) in file:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings}</li>
 *   <li>Optionally consider caching of weights values, if values are re-computated dynamically:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.WeigthsCache}</li>
 *   <li>Add a static initialization method in:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory}</li>
 *   <li>Add the initialization method to the <tt>createAllCombinations</tt>-methods of:
 *       {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory}</li>
 *   <li>Add the settings and and optional containers to:
 *       {@link net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters}</li>
 *   <li>Adapt the initialization methods of:
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
 */
package net.ssehub.kernel_haven.metric_haven.metric_components.weights;
