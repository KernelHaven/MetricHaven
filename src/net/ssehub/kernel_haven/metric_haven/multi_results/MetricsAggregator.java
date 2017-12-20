package net.ssehub.kernel_haven.metric_haven.multi_results;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;

/**
 * Collects the results of multiple metric analysis and aggregates them.
 * @author El-Sharkawy
 * @author Adam
 *
 */
public class MetricsAggregator extends AnalysisComponent<MultiMetricResult> {
    
    private AnalysisComponent<MetricResult>[] metrics;
    
    private Map<String, ValueRow> valueTable = new HashMap<>();
    private String[] metricNames;
    private Map<String, MeasuredItem> ids = new HashMap<>();
    private boolean hasIncludedFiles = false;
    private String resultName;

    /**
     * Creates a {@link MetricsAggregator} for the given metric components, with a fixed name for the results.
     * 
     * @param config The pipeline configuration.
     * @param metrics The metric components to aggregate the results for.
     */
    @SafeVarargs
    public MetricsAggregator(Configuration config, AnalysisComponent<MetricResult>... metrics) {
        this(config, "Aggregated Metric Results", metrics);
    }
    
    /**
     * Creates a {@link MetricsAggregator} for the given metric components, allows to specify the result name.
     * 
     * @param config The pipeline configuration.
     * @param resultName The name of the results (i.e., the name of the Excel sheet or the CSV file).
     * @param metrics The metric components to aggregate the results for.
     */
    @SafeVarargs
    public MetricsAggregator(Configuration config, String resultName, AnalysisComponent<MetricResult>... metrics) {
        super(config);
        this.metrics = metrics;
        this.resultName =  resultName;
        
        int nMetrics = (null != metrics) ? metrics.length : 0;
        metricNames = new String[nMetrics];
        for (int i = 0; i < nMetrics; i++) {
            metricNames[i] = metrics[i].getResultName();
        }
    }

    /**
     * Adds a new metric results to this table.
     * 
     * @param mainFile The measured source file (e.g., a C-file).
     * @param includedFile Optional: an included file (e.g., a H-file).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     * @param metricName The unique name of the metric which computed the value.
     * @param value The measured/computed value for the given element.
     */
    // CHECKSTYLE:OFF
    private synchronized void addValue(String mainFile, String includedFile, int lineNo, String element, String metricName,
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
    private MultiMetricResult[] createTable() {
        // Create header/columns
        int nColumns = hasIncludedFiles ? metricNames.length + 4 : metricNames.length + 3;
        String[] header = new String[nColumns];
        int index = 0;
        header[index++] = "Source File";
        if (hasIncludedFiles) {
            header[index++] = "Included File";
        }
        header[index++] = "Line No.";
        header[index++] = "Element";
        System.arraycopy(metricNames, 0, header, index, metricNames.length);
        
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
            for (int j = 0; j < metricNames.length; j++) {
                values[index++] = column.getValue(metricNames[j]);
            }
            
            result[i] = new MultiMetricResult(header, values);
        }
        
        return result;
    }

    @Override
    protected void execute() {
        // start threads to poll from each input metric
        
        List<Thread> threads = new LinkedList<>();
        for (AnalysisComponent<MetricResult> metric : metrics) {
            
            Thread th = new Thread(() -> {
                
                MetricResult result;
                while ((result = metric.getNextResult()) != null) {
                    String sourceFile = result.getSourceFile() != null ? result.getSourceFile().getPath() : null;
                    String includedFile = result.getIncludedFile() != null ? result.getIncludedFile().getPath() : null;
                    addValue(sourceFile, includedFile, result.getLine(), result.getContext(),
                            metric.getResultName(), result.getValue());
                }
                
            }, "MetricsAggreagtorPollThread");
            
            threads.add(th);
            th.start();
            
        }
        
        for (Thread th : threads) {
            try {
                th.join();
            } catch (InterruptedException e) {
                // can't happen
            }
        }
        
        for (MultiMetricResult result : createTable()) {
            addResult(result);
        }
    }

    @Override
    public String getResultName() {
        return resultName;
    }
}
