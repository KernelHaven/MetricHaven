# MetricHaven

![Build Status](https://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=KernelHaven_MetricHaven)

An analysis plugin for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

Contains analysis components for running (variability-aware) metrics on Software Product Lines.

## Usage

Place [`MetricHaven.jar`](https://jenkins.sse.uni-hildesheim.de/view/KernelHaven/job/KernelHaven_MetricHaven/lastSuccessfulBuild/artifact/build/jar/MetricHaven.jar) in the plugins folder of KernelHaven.

### Provided Metrics
For the sake of simplicity we omit the full qualified class names in the following table. All class names start with the
prefix `net.ssehub.kernel_haven.metric_haven.metric_components`.

<table style="width:100%">
  <tr>
    <th>Class</th>
    <th>Variations</th>
    <th>Description</th>
    <th>Options</th>
  </tr>
  <!-- VariablesPerFunctionMetric -->
  <tr>
    <td><code>VariablesPerFunctionMetric</code></td>
    <td>60</td>
    <td>Realizes the <i>Number of internal/external configuration options</i> metric from
        <a href="https://doi.org/10.1145/2934466.2934467"> Do #ifdefs influence the occurrence of vulnerabilities? an
        empirical study of the linux kernel</a> paper.
    </td>
    <td>
      <code>metric.variables_per_function.measured_variables_type</code>:
      <ul>
        <li><code>INTERNAL</code>: Counts the number of variables used inside the function</li>
        <li><code>EXTERNAL</code>: Counts the number of variables used outside the function</li>
        <li><code>EXTERNAL_WITH_BUILD_VARS</code>: Counts the number of variables used outside the function (considers variables of the build model (requires provided build model))</li>
        <li><code>ALL</code>: <code>EXTERNAL + INTERNAL</code></li>
        <li><code>ALL_WITH_BUILD_VARS</code>: <code>EXTERNAL_WITH_BUILD_VARS + INTERNAL</code></li>
    </ul>
    <code>metric.function_measures.consider_scattering_degree</code>:
    <ul>
      <li><code>NO_SCATTERING</code>: Won't consider scattering degree of measured variables</li>
      <li><code>SD_VP</code>: Weights each variable with its variation point scattering (e.g., no of ifdefs a variable is used in).</li>
      <li><code>SD_FILE</code>: Weights each variable with its file scattering.</li>
    </ul>
    <code>metric.function_measures.consider_ctcr</code> (requires an extracted variability model):
    <ul>
      <li><code>NO_CTCR</code>: Won't consider constraints of the variability model</li>
      <li><code>INCOMIG_CONNECTIONS</code>: Weights each variable with the no. of distinct variables, specifying a constraint <b>TO</b> the measured variable.</li>
      <li><code>OUTGOING_CONNECTIONS</code>: Weights each variable with the no. of distinct variables, referenced in constraints defined <b>BY</b> the measured variable.</li>
      <li><code>ALL_CTCR</code>: Weights each variable with the (INCOMIG_CONNECTIONS + OUTGOING_CONNECTIONS).</li>
    </ul>
  </td>
  </tr>
  <!-- CyclomaticComplexityMetric -->
  <tr>
    <td><code>CyclomaticComplexityMetric</code></td>
    <td>25</td>
    <td>Measures the Cyclomatic Complexity of code functions.
    </td>
    <td>
      <code>metric.cyclomatic_complexity.measured_type</code>:
      <ul>
        <li><code>MCCABE</code>: Measures the cyclomatic complexity of classical code elements as defined by McCabe; uses a simplification that only the following keywords will be counted: <tt>if, for, while, case</tt>.</li>
        <li><code>VARIATION_POINTS</code>: Measures the cyclomatic complexity of variation points only; uses a simplification that only the following keywords will be counted: <tt>if, elif</tt>.</li>
        <li><code>ALL</code>: <code>MCCABE + VARIATION_POINTS</code></li>
      </ul>
      <code>metric.function_measures.consider_scattering_degree</code> (not applyable to <code>MCCABE</code>):
    <ul>
      <li><code>NO_SCATTERING</code>: Won't consider scattering degree of measured variables</li>
      <li><code>SD_VP</code>: Weights each variable with its variation point scattering (e.g., no of ifdefs a variable is used in).</li>
      <li><code>SD_FILE</code>: Weights each variable with its file scattering.</li>
    </ul>
    <code>metric.function_measures.consider_ctcr</code> (requires an extracted variability model, not applyable to <code>MCCABE</code>):
    <ul>
      <li><code>NO_CTCR</code>: Won't consider constraints of the variability model</li>
      <li><code>INCOMIG_CONNECTIONS</code>: Weights each variable with the no. of distinct variables, specifying a constraint <b>TO</b> the measured variable.</li>
      <li><code>OUTGOING_CONNECTIONS</code>: Weights each variable with the no. of distinct variables, referenced in constraints defined <b>BY</b> the measured variable.</li>
      <li><code>ALL_CTCR</code>: Weights each variable with the (INCOMIG_CONNECTIONS + OUTGOING_CONNECTIONS).</li>
    </ul>
    </td>
  </tr>
  <tr>
    <th>Total</th>
    <td>75</td>
    <td></td>
    <td></td>
  </tr>
</table>

## Dependencies

In addition to KernelHaven, this plugin has the following dependencies:
* A code extractor, which extracts an AST (`SyntaxElement`s).
* A variability model extractor, which computes cross-tree constraint ratios for <code>CTCR</code> options.
* A build model extractor, for some of the options (see description from above).

## Guidance
The follwoing classes execute all currently available metrics in one step, without requiring a detailed configuration:
<table style="width:100%">
  <tr>
    <th>Class</th>
    <th>Description</th>
  </tr>
  <tr>
    <td><code>net.ssehub.kernel_haven.metric_haven.metric_components.AllFunctionMetrics</code></td>
    <td>All code function metrics</td>
  </tr>
</table>

## License

This plugin is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
