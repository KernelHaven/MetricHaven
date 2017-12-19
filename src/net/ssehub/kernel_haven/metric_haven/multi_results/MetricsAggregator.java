package net.ssehub.kernel_haven.metric_haven.multi_results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects the results of multiple metric analysis and aggregates them.
 * @author El-Sharkawy
 *
 */
public class MetricsAggregator {
    
    private Map<String, ValueRow> valueTable = new HashMap<>();
    private Set<String> metricNames = new HashSet<>();
    private Map<String, MeasuredItem> ids = new HashMap<>();
    private boolean hasIncludedFiles = false;
    
    /**
     * Adds a new metric results to this table.
     * @param mainFile The measured source file (e.g., a C-file).
     * @param includedFile Optional: an included file (e.g., a H-file).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     * @param metricName The unique name of the metric which computed the value.
     * @param value The measured/computed value for the given element.
     */
    // CHECKSTYLE:OFF
    public void addValue(String mainFile, String includedFile, int lineNo, String element, String metricName,
        double value) {
    // CHECKSTYLE:ON
        
        String id = mainFile;
        if (null != includedFile) {
            id += ":" + includedFile;
        }
        id += ":" + lineNo + ":" + element;
        
        // Store information to create rows and columns
        if (!ids.containsKey(id)) {
            MeasuredItem item = new MeasuredItem(mainFile, includedFile, lineNo, element);
            ids.put(id, item);
        }
        metricNames.add(metricName);
        hasIncludedFiles |= (null != includedFile);
        
        // Add the value
        ValueRow column = getRow(id);
        column.addValue(metricName, value);
    }
    
    /**
     * Returns the row for the given measured element.
     * @param id A unique identifier for an measured element.
     * @return The row values (maybe empty) for the measured element.
     */
    private ValueRow getRow(String id) {
        ValueRow column = valueTable.get(id);
        if (null == column) {
            column = new ValueRow();
            valueTable.put(id, column);
        }
        
        return column;
    }
    
    /**
     * Creates and returns a table of {@link MultiMetricResult}s based on the passed data of
     * {@link #addValue(String, String, int, String, String, double)}.
     * @return An ordered list of {@link MultiMetricResult}s.
     */
    public MultiMetricResult[] createTable() {
        // Create header/columns
        String[] metricColumns = metricNames.toArray(new String[0]);
        Arrays.sort(metricColumns);
        int nColumns = hasIncludedFiles ? metricColumns.length + 4 : metricColumns.length + 3;
        String[] header = new String[nColumns];
        int index = 0;
        header[index++] = "Source File";
        if (hasIncludedFiles) {
            header[index++] = "Included File";
        }
        header[index++] = "Line No.";
        header[index++] = "Element";
        System.arraycopy(metricColumns, 0, header, index, metricColumns.length);
        
        // Create rows
        String[] columnIDs = ids.keySet().toArray(new String[0]);
        Arrays.sort(columnIDs);
        
        // Create Values
        MultiMetricResult[] result = new MultiMetricResult[columnIDs.length];
        for (int i = 0; i < columnIDs.length; i++) {
            String id = columnIDs[i];
            ValueRow column = getRow(id);
            MeasuredItem item = ids.get(id);
            
            Object[] values = new Object[header.length];
            index = 0;
            // The measured element
            values[index++] = item.getMainFile();
            if (hasIncludedFiles) {
                values[index++] = item.getIncludedFile();
            }
            values[index++] = item.getLineNo();
            values[index++] = item.getElement();
            // The measured values
            for (int j = 0; j < metricColumns.length; j++) {
                values[index++] = column.getValue(metricColumns[j]);
            }
            
            result[i] = new MultiMetricResult(header, values);
        }
        
        return result;
    }
}
