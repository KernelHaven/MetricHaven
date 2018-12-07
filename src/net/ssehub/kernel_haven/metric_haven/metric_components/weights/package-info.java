/**
 * Optional functions to weight variables for different characteristics.
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
 */
package net.ssehub.kernel_haven.metric_haven.metric_components.weights;
