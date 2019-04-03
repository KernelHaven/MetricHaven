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
    private boolean considerHeaders;
    
    private final @Nullable String @NonNull [] internalHeader;
    
    private @Nullable Object @NonNull [] internalContent;
    
    private final @NonNull String @NonNull [] metrics;
    
    private @Nullable Double @NonNull [] values;

    /**
     * Creates a new instance and shares the header information with the previous result of the <b>same</b> kind of
     * result.
     * 
     * @param measuredItem The item that was measured.
     * 
     * @param previous Any previous metric result of the same type (both elements will share the header instance).
     * @param values The measured values. If a value was not measured it should be <tt>null</tt>.
     *     Must be as long as the metric array (and in same order).
     */
    public MultiMetricResult(@NonNull MeasuredItem measuredItem, @NonNull MultiMetricResult previous,
        @Nullable Double @NonNull [] values) {
        
        this.measuredItem = measuredItem;
        this.values = values;
        this.metrics = previous.metrics;
        this.internalHeader = previous.internalHeader;
        internalContent = new @Nullable Object[previous.internalContent.length];
        
        int contentIndex = 0;

        // Information about measured element
        internalContent[contentIndex++] = measuredItem.getMainFile();
        if (considerHeaders) {
            internalContent[contentIndex++] = measuredItem.getIncludedFile();
        }
        internalContent[contentIndex++] = measuredItem.getLineNo();
        internalContent[contentIndex++] = measuredItem.getElement();
        
        System.arraycopy(values, 0, internalContent, contentIndex, values.length);
    }
    
    /**
     * Creates a new {@link MultiMetricResult} if no header was defined before.
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
        
        considerHeaders = measuredItem.isConsiderIncludedFile();
        int headerIndex = 0;
        int contentIndex = 0;
        internalHeader = new @Nullable String[metrics.length + (considerHeaders ? 4 : 3)];
        internalContent = new @Nullable Object[metrics.length + (considerHeaders ? 4 : 3)];
        
        internalHeader[headerIndex++] = "Source File";
        internalContent[contentIndex++] = measuredItem.getMainFile();
        
        if (considerHeaders) {
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
