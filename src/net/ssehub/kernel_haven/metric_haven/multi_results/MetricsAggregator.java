package net.ssehub.kernel_haven.metric_haven.multi_results;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Collects the results of multiple metric analysis and aggregates them.
 * @author El-Sharkawy
 * @author Adam
 *
 */
public class MetricsAggregator extends AnalysisComponent<MultiMetricResult> {
    
    public static final @NonNull Setting<@NonNull Boolean> ROUND_RESULTS = new Setting<>("metrics.round_results",
        Type.BOOLEAN, true, "false", "If turned on, results will be limited to 2 digits after the comma (0.005 will be "
        + "rounded up). This is maybe neccessary to limit the disk usage.");
    
    private @NonNull AnalysisComponent<MetricResult> @NonNull [] metrics;
    
    private @NonNull Map<String, ValueRow> valueTable = new HashMap<>();
    private @NonNull String @NonNull [] metricNames;
    private @NonNull Map<String, MeasuredItem> ids = new HashMap<>();
    private boolean hasIncludedFiles = false;
    private @NonNull String resultName;
    private boolean round = false;

    /**
     * Creates a {@link MetricsAggregator} for the given metric components, with a fixed name for the results.
     * 
     * @param config The pipeline configuration.
     * @param metrics The metric components to aggregate the results for.
     */
    @SafeVarargs
    public MetricsAggregator(@NonNull Configuration config,
            @NonNull AnalysisComponent<MetricResult> /*@NonNull*/ ... metrics) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
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
    public MetricsAggregator(@NonNull Configuration config, @NonNull String resultName,
            @NonNull AnalysisComponent<MetricResult> /*@NonNull*/ ... metrics) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        super(config);
        
        try {
            config.registerSetting(ROUND_RESULTS);
            round = config.getValue(ROUND_RESULTS);
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + ROUND_RESULTS, exc);
        }
        
        this.metrics = notNull(metrics);
        this.resultName =  resultName;
        
        int nMetrics = (null != metrics) ? metrics.length : 0;
        metricNames = new @NonNull String[nMetrics];
        for (int i = 0; i < nMetrics; i++) {
            metricNames[i] = notNull(metrics)[i].getResultName();
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
    private synchronized void addValue(@NonNull String mainFile, @Nullable String includedFile, int lineNo,
            @NonNull String element, @NonNull String metricName, double value) {
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
        if (round) {
            value = Math.floor(value * 100) / 100;
        }
        column.addValue(metricName, value);
    }
    
    /**
     * Returns the row for the given measured element.
     * @param id A unique identifier for an measured element.
     * @return The row values (maybe empty) for the measured element.
     */
    private @NonNull ValueRow getRow(@NonNull String id) {
        ValueRow column = valueTable.get(id);
        if (null == column) {
            column = new ValueRow();
            valueTable.put(id, column);
        }
        
        return column;
    }
    
    /**
     * Creates and sends a table of {@link MultiMetricResult}s based on the passed data of
     * {@link #addValue(String, String, int, String, String, double)}.
     */
    private void createTable() {
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
        @NonNull String[] columnIDs = ids.keySet().toArray(new @NonNull String[0]);
        Arrays.sort(columnIDs);
        
        // Create Values
//        @NonNull MultiMetricResult[] result = new @NonNull MultiMetricResult[columnIDs.length];
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
            
            addResult(new MultiMetricResult(header, values));
        }
    }

    @Override
    protected void execute() {
        // start threads to poll from each input metric
        
        List<Thread> threads = new LinkedList<>();
        for (AnalysisComponent<MetricResult> metric : metrics) {
            
            Thread th = new Thread(() -> {
                
                MetricResult result;
                while ((result = metric.getNextResult()) != null) {
                    File f = result.getSourceFile();
                    String sourceFile = f != null ? notNull(f.getPath()) : "<unknown>";
                    f = result.getIncludedFile();
                    String includedFile = f != null ? f.getPath() : null;
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
        
        LOGGER.logInfo2("All metrics done, merging ", threads.size(), " results.");
        createTable();
//        for (MultiMetricResult result : createTable()) {
//            addResult(result);
//        }
    }

    @Override
    public @NonNull String getResultName() {
        return resultName;
    }
}
