package net.ssehub.kernel_haven.metric_haven.multi_results;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Collects the results of multiple metric analysis and aggregates them.
 * @author El-Sharkawy
 * @author Adam
 *
 */
public class MetricsAggregator extends AnalysisComponent<MultiMetricResult> {
    
    /**
     * Responsible for renaming a sub thread, when the thread tis started.
     * @author El-Sharkawy
     *
     */
    private static class DefaultThreadFactory {
        private int threadNumber = 1;
               
        /**
         * Gives the currently executed thread a meaningful name.
         */
        private void rename() {
            Thread th = Thread.currentThread();
            Field thField;
            try {
                thField = th.getClass().getDeclaredField("target");
                thField.setAccessible(true);
                Object fieldValue = thField.get(th);
                String name = "MetricsThread #" + threadNumber++;
                if (fieldValue instanceof NamedRunnable) {
                    name += " - " + ((NamedRunnable) fieldValue).getName();
                } else {
                    // Maybe a private Worker
                    Field f = fieldValue.getClass().getDeclaredField("firstTask");
                    f.setAccessible(true);
                    Object workerdValue = f.get(fieldValue);
                    if (workerdValue instanceof NamedRunnable) {
                        name += " - " + ((NamedRunnable) fieldValue).getName();
                    }
                }
                Thread.currentThread().setName(name);
            } catch (ReflectiveOperationException | SecurityException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static final @NonNull Setting<@NonNull Boolean> ROUND_RESULTS = new Setting<>("metrics.round_results",
        Type.BOOLEAN, true, "false", "If turned on, results will be limited to 2 digits after the comma (0.005 will be "
            + "rounded up). This is maybe neccessary to limit the disk usage.");
    
    public static final @NonNull Setting<@NonNull Integer> MAX_THREADS = new Setting<>("metrics.max_parallel_threads",
        Type.INTEGER, true, "0", "If greater than 0, a thread pool is used to limit the maximum number of threads "
            + "executed in parallel.");
    
    private @NonNull AnalysisComponent<MetricResult> @NonNull [] metrics;
    
    private @NonNull Map<String, ValueRow> valueTable = new HashMap<>();
    private @NonNull String @NonNull [] metricNames;
    private @NonNull Map<String, MeasuredItem> ids = new HashMap<>();
    private boolean hasIncludedFiles = false;
    private @NonNull String resultName;
    private boolean round = false;
    private int nThreads;
    private @Nullable Set<String> fileNameFilter;
    
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
            LOGGER.logException("Could not load configuration setting " + ROUND_RESULTS.getKey(), exc);
        }
        
        try {
            config.registerSetting(MetricSettings.FILTER_BY_FILES);
            List<String> filterList = config.getValue(MetricSettings.FILTER_BY_FILES);
            if (null != filterList && !filterList.isEmpty()) {
                fileNameFilter = new HashSet<>();
                for (String filePattern : filterList) {
                    if (File.separatorChar == '\\') {
                        // Make pattern platform independent (file names are generated from java.io.File objects)
                        fileNameFilter.add(filePattern.replace('/', File.separatorChar));
                    } else {
                        fileNameFilter.add(filePattern);
                    }
                }
            }
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + MetricSettings.FILTER_BY_FILES.getKey(), exc);
        }
        
        try {
            config.registerSetting(MAX_THREADS);
            nThreads = config.getValue(MAX_THREADS);
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + MAX_THREADS.getKey(), exc);
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
     * Determines whether the given element shall be rejected (<tt>false</tt>) or be accepted (<tt>true</tt>)
     * by this filter.
     * @param mainFile The measured source file (e.g., a C-file).
     * @param includedFile Optional: an included file (e.g., a H-file).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     * @return <tt>true</tt>: Element shall be kept; <tt>false</tt>: Element shall be skipped.
     */
    private boolean filter(@NonNull String mainFile, @Nullable String includedFile, int lineNo,
        @NonNull String element) {
        
        boolean accept = fileNameFilter == null;
        if (!accept) {
            accept = fileNameFilter.contains(mainFile);
        }
        
        return accept;
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
        
        
        StringBuffer id = new StringBuffer(mainFile);
        if (null != includedFile) {
            id.append(":");
            id.append(includedFile);
        }
        id.append(":");
        id.append(lineNo);
        id.append(":");
        id.append(element);
        
        // Store information to create rows and columns
        String key = NullHelpers.notNull(id.toString());
        if (!ids.containsKey(key)) {
            MeasuredItem item = new MeasuredItem(mainFile, includedFile, lineNo, element);
            ids.put(key, item);
        }
        hasIncludedFiles |= (null != includedFile);
        
        // Add the value
        ValueRow column = getRow(key);
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
            valueTable.put(id.toString(), column);
        }
        
        return column;
    }
    
    /**
     * Creates and sends a table of {@link MultiMetricResult}s based on the passed data of
     * {@link #addValue(String, String, int, String, String, double)}.
     */
    private void createTable() {
        // Create rows
        @NonNull String[] columnIDs = ids.keySet().toArray(new @NonNull String[0]);
        Arrays.sort(columnIDs);
        
        // Create Values
        for (int i = 0; i < columnIDs.length; i++) {
            String id = columnIDs[i];
            ValueRow column = getRow(id);
            MeasuredItem item = notNull(ids.get(id)); // not null since we iterate over the known key set
            item.setConsiderIncludedFile(hasIncludedFiles);
            
            Double[] values = new Double[metricNames.length];
            for (int j = 0; j < metricNames.length; j++) {
                values[j] = column.getValue(metricNames[j]);
            }
            
            addResult(new MultiMetricResult(item, metricNames, values));
        }
        
        // Deallocate memory
        clear();
    }
    
    @Override
    protected void execute() {
        // See for thread pools: https://stackoverflow.com/a/8651450
        // See for join threads: https://stackoverflow.com/a/20495490
        // start threads to poll from each input metric
        DefaultThreadFactory thFactory = new DefaultThreadFactory();
        ThreadPoolExecutor thPool = (ThreadPoolExecutor) ( (nThreads > 0)
            ? Executors.newFixedThreadPool(nThreads)
            : Executors.newCachedThreadPool());
        int totalNoOfThreads = 0;
        AtomicInteger nThreadsProcessed = new AtomicInteger(0);
        for (AnalysisComponent<MetricResult> metric : metrics) {
            totalNoOfThreads++;
            NamedRunnable r = new NamedRunnable() {
                
                @Override
                public void run() {
                    thFactory.rename();
                    MetricResult result;
                    while ((result = metric.getNextResult()) != null) {
                        File f = result.getSourceFile();
                        String sourceFile = f != null ? notNull(f.getPath()) : "<unknown>";
                        f = result.getIncludedFile();
                        String includedFile = f != null ? f.getPath() : null;
                        if (filter(sourceFile, includedFile, result.getLine(), result.getContext())) {
                            addValue(sourceFile, includedFile, result.getLine(), result.getContext(),
                                metric.getResultName(), result.getValue());
                        }
                    }
                    
                    LOGGER.logInfo2("Metric result collection finished: ", getName());
                    nThreadsProcessed.incrementAndGet();
                }

                @Override
                public String getName() {
                    return metric.getResultName();
                }
                
            };

            thPool.execute(r);           
        }

        thPool.shutdown();
        final int submittedThreads = totalNoOfThreads;
        Runnable monitor = () -> {
            while (!thPool.isTerminated()) {
                LOGGER.logStatusLines("Joining components:",
                    "Total: " + submittedThreads, 
                    "Finished: " + nThreadsProcessed.get(),
                    "Processing: " + thPool.getActiveCount());
                try {
                    Thread.sleep(3 * 60 * 1000);
                } catch (InterruptedException exc) {
                    LOGGER.logException("", exc);
                }
            }
        };
        Thread th = new Thread(monitor, getClass().getSimpleName());
        th.start();
        try {
            thPool.awaitTermination(96L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            LOGGER.logException("", e);
        }
        
        LOGGER.logInfo2("All metrics done. Merging ", totalNoOfThreads, " results.");
        createTable();
    }

    @Override
    public @NonNull String getResultName() {
        return resultName;
    }
    
    /**
     * Deallocates memory after aggregation is finished.
     */
    @SuppressWarnings("null")
    private void clear() {
        ids.clear();
        valueTable.clear();
        
        // May only be called after execution/createTable loop is finished, must be considered while refactoring
        metrics = null;
        
        System.gc();
    }
}
