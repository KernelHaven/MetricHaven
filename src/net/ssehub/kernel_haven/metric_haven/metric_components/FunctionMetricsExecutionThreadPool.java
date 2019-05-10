/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.List;

import net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Runs multiple metrics in threads and reuses the threads.
 *
 * @author el-sharkawy
 * @author Adam
 */
class FunctionMetricsExecutionThreadPool {
    
    /**
     * One Thread, responsible for running one partition of metrics.
     */
    private class MetricThread extends Thread {
        
        private CodeFunction function;
        
        private int startIndex;
        
        private int endIndex;
        
        /**
         * Creates a {@link MetricThread} for the given interval of metrics.
         * 
         * @param startIndex The index of the first metric that this thread should calculate, inclusive.
         * @param endIndex The index of the last metric that this thread should calculate, exclusive.
         */
        public MetricThread(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        @Override
        public void run() {
            for (int i = startIndex; i < endIndex; i++) {
                Number result = notNull(metrics.get(i)).compute(function);
                if (result instanceof Double && round) {
                    resultValues[i] = Math.floor(result.doubleValue() * 100) / 100;
                } else {
                    resultValues[i] = (null != result) ? result.doubleValue() : null;
                }
            }
        }
        
        /**
         * Starts the metric execution on the specified function.
         * @param function The function to measure.
         */
        private void start(@NonNull CodeFunction function) {
            this.function = function;
            start();
        }
        
    }
    
    /**
     * Shorthand for the global singleton logger.
     */
    private static final Logger LOGGER = Logger.get();
    
    /**
     * A list of all metrics that should be calculated.
     */
    private final @NonNull List<@NonNull AbstractFunctionMetric<?>> metrics;
    
    /**
     * The list of names of the {@link #metrics}. Must be same size as {@link #metrics}.
     */
    private final @NonNull String @NonNull [] metricNames;
    
    /**
     * The threads that will compute the metric values.
     */
    private final @NonNull MetricThread @NonNull [] metricThreads;
    
    private final boolean round;
    
    /**
     * {@link #metricThreads} write directly into this. Must be same size as {@link #metrics}.
     */
    private final @Nullable Double @NonNull [] resultValues;
    
    /**
     * Cache first result, to allow for cheaper creation of following {@link MultiMetricResult}s.
     */
    private MultiMetricResult firstResult;
    
    
    /**
     * Sole constructor for this class, initializes all threads, but does not start them.
     * @param allMetrics All metric instances to run.
     * @param metricNames The name of the metrics in the same order.
     * @param nThreads Specifies how many threads shall be used (&gt; 1)
     * @param round <tt>true</tt> round results to 2 digits behind the comma.
     */
    FunctionMetricsExecutionThreadPool(@NonNull List<@NonNull AbstractFunctionMetric<?>> allMetrics,
        @NonNull String @NonNull [] metricNames, int nThreads, boolean round) {
        
        this.metrics = allMetrics;
        this.metricNames = metricNames;
        this.round = round;
        this.resultValues = new @Nullable Double[allMetrics.size()];
        
        int partitionSize = (int) Math.ceil((double) allMetrics.size() / nThreads);
        
        metricThreads = new @NonNull MetricThread[nThreads];
        for (int i = 0; i < metricThreads.length; i++) {
            // Start of interval (inclusive)
            int partitionStart = i * partitionSize;
            // End of interval (exclusive)
            int partitionEnd = Math.min((i + 1) * partitionSize, allMetrics.size());
            
            metricThreads[i] = new MetricThread(partitionStart, partitionEnd);
            metricThreads[i].setName("MetricThread-" + (i + 1));
        }
    }
    
    /**
     * Compute all metric results for the specified code function. 
     * @param function The function to measure.
     * @return The result of the metric execution.
     */
    public @NonNull MultiMetricResult compute(@NonNull CodeFunction function) {
        // Start all threads on the passed function
        for (MetricThread metricThread : metricThreads) {
            metricThread.start(function);
        }
        
        // Wait for all threads to be finished
        for (MetricThread metricThread : metricThreads) {
            try {
                metricThread.join();
            } catch (InterruptedException e) {
                LOGGER.logException("Could not join metric threads for joining the result", e);
            }
        }
        
        MeasuredItem funcDescription = new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
            function.getFunction().getLineStart(), function.getName());
        MultiMetricResult firstResult = this.firstResult;
        MultiMetricResult result;
        if (null == firstResult) {
            // Initializes header
            firstResult = new MultiMetricResult(funcDescription, metricNames, resultValues);
            result = firstResult;
        } else {
            // Less memory/time consuming
            result = new MultiMetricResult(funcDescription, firstResult, resultValues);
        }
        
        return result;
    }
}