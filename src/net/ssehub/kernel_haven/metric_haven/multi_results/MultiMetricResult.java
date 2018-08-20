package net.ssehub.kernel_haven.metric_haven.multi_results;

import net.ssehub.kernel_haven.util.io.ITableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Data object storing multiple results for one measured item, e.g., one function.
 * 
 * @author El-Sharkawy
 * @author Adam
 */
public class MultiMetricResult implements ITableRow {
    
    private @NonNull MeasuredItem measuredItem;
    
    private @Nullable String @NonNull [] internalHeader;
    
    private @Nullable Object @NonNull [] internalContent;
    
    private @NonNull String @NonNull [] metrics;
    
    private @Nullable Double @NonNull [] values;

    /**
     * Sole constructor.
     * 
     * @param measuredItem The item that was measured.
     * 
     * @param metrics The names of the measured metrics.
     * @param values The measured values. If a value was not measured it should be <tt>null</tt>.
     *     Must be as long as the metric array (and in same order).
     */
    public MultiMetricResult(@NonNull MeasuredItem measuredItem, @NonNull String @NonNull [] metrics,
            @Nullable Double @NonNull [] values) {
        
        this.measuredItem = measuredItem;
        this.metrics = metrics;
        this.values = values;
        
        boolean hasIncludedFile = measuredItem.isConsiderIncludedFile();
        int headerIndex = 0;
        int contentIndex = 0;
        internalHeader = new @Nullable String[metrics.length + (hasIncludedFile ? 4 : 3)];
        internalContent = new @Nullable Object[metrics.length + (hasIncludedFile ? 4 : 3)];
        
        internalHeader[headerIndex++] = "Source File";
        internalContent[contentIndex++] = measuredItem.getMainFile();
        
        if (hasIncludedFile) {
            internalHeader[headerIndex++] = "Included File";
            internalContent[contentIndex++] = measuredItem.getIncludedFile();
        }
        
        internalHeader[headerIndex++] = "Line No.";
        internalContent[contentIndex++] = measuredItem.getLineNo();
        
        internalHeader[headerIndex++] = "Element";
        internalContent[contentIndex++] = measuredItem.getElement();
        
        for (int i = 0; i < metrics.length; i++) {
            internalHeader[headerIndex++] = metrics[i];
            internalContent[contentIndex++] = values[i];
        }
    }

    @Override
    public @Nullable Object @NonNull [] getContent() {
        return internalContent;
    }

    @Override
    public @Nullable String @NonNull [] getHeader() {
        return internalHeader;
    }
    
    /**
     * Returns the measured item.
     * 
     * @return The item that the metric result have been measured for.
     */
    public @NonNull MeasuredItem getMeasuredItem() {
        return measuredItem;
    }
    
    /**
     * Returns the list of metrics that this result contains.
     * 
     * @return The list of metrics.
     */
    public @NonNull String @NonNull [] getMetrics() {
        return metrics;
    }
    
    /**
     * Returns the list of values for the measured metrics. In same order and same size as {@link #getMetrics()}.
     * 
     * @return The measured values. Missing values are <code>null</code>.
     */
    public @Nullable Double @NonNull [] getValues() {
        return values;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("MultiMetricResult for ");
        result.append(measuredItem.toString()).append(" (");
        
        for (int i = 0; i < metrics.length; i++) {
            result.append(metrics[i]).append("=").append(values[i]).append(", ");
        }
        result.replace(result.length() - 2, result.length(), ""); // remove trailing ", "
        
        result.append(")");
        return result.toString();
    }
}
