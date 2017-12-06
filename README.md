# MetricHaven
Infrastructure for running (variability-aware) metrics on Software Product Lines.

## Provided Metrics
For the sake of simplicity we omit the full qualified class names in the following table. All class names start with the
prefix `net.ssehub.kernel_haven.metric_haven.metrics`.

<table style="width:100%">
  <tr>
    <th>Class</th>
    <th>Description</th>
    <th>Options</th>
  </tr>
  <!-- VariablesPerFunctionMetric -->
  <tr>
    <td><code>VariablesPerFunctionMetric</code></td>
    <td>Realizes the <i>Number of internal/external configuration options</i> metric from
        <a href="https://doi.org/10.1145/2934466.2934467"> Do #ifdefs influence the occurrence of vulnerabilities? an
        empirical study of the linux kernel</a> paper.
    </td>
    <td><ul>
        <li><code>EXTERNAL</code>: Counts the number of variables used outside the function</li>
        <li><code>INTERNAL</code>: Counts the number of variables used inside the function</li>
        <li><code>ALL</code>: <code>EXTERNAL + INTERNAL</code></li>
    </ul></td>
  </tr>
</table>

### Dependencies

This analysis requires:
* A code extractor, which extracts an AST (`SyntaxElement`s).