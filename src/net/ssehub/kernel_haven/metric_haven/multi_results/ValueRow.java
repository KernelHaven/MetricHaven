package net.ssehub.kernel_haven.metric_haven.multi_results;

import java.util.HashMap;
import java.util.Map;

/**
 * Data class of {@link MetricsAggregator}, representing a row of values.
 * @author El-Sharkawy
 *
 */
class ValueRow {

    private Map<String, Double> valueRow = new HashMap<>();
    
    /**
     * Adds an value for the specified metric.
     * @param metricName The unique name of the measured metric.
     * @param value The value of the metric.
     */
    void addValue(String metricName, Double value) {
        valueRow.put(metricName, value);
    }
    
    /**
     * Returns the measured value for the specified metric.
     * @param metricName The unique name of a measured metric, must not be <tt>null</tt>.
     * @return The measured value, maybe <tt>null</tt> if the metric was not applied to the
     *     represented row (measured item).
     */
    Double getValue(String metricName) {
        return valueRow.get(metricName);
    }
}
