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
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * 
 * Runs multiple metrics in threads and reuses the threads.
 * @author el-sharkawy
 */
class FunctionMetricsExecutionThreadPool {
    
    /**
     * One Thread, responsible for running one partition of metrics.
     *
     * @author el-sharkawy
     */
    private class MetricThread extends Thread {
        /**
         * The offset specifies how many metrics are &quot;ordered&quot; before the first metric of this thread.
         * This is required to keep all results of all threads always in the same order.
         */
        private int offset;
        private AbstractFunctionMetric<?>[] metrics;
        private CodeFunction function;
        
        @Override
        public void run() {
            for (int i = 0; i < metrics.length; i++) {
                int index = offset + i;
                Number result = metrics[i].compute(function);
                if (result instanceof Double && round) {
                    values[index] = Math.floor(result.doubleValue() * 100) / 100;
                } else {
                    values[index] = (null != result) ? result.doubleValue() : null;
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
    
    private final MetricThread[] metricThreads;
    private final @NonNull String @NonNull [] metricNames;
    private final boolean round;
    private final @Nullable Double @NonNull [] values;
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
        
        this.round = round;
        this.metricNames = metricNames;
        values = new @Nullable Double[allMetrics.size()];
        int partitionSize = (int) Math.ceil((double) allMetrics.size() / nThreads);
        
        metricThreads = new MetricThread[nThreads];
        for (int i = 0; i < metricThreads.length; i++) {
            metricThreads[i] = new MetricThread();
            
            // Start of interval (inclusive)
            final int partionStart = i * partitionSize;
            // End of interval (exclusive)
            final int partitionEnd = Math.min((i + 1) * partitionSize, allMetrics.size());
            int thisPartionSize = partitionEnd - partionStart;
            
            int startIndex = 0;
            for (int j = partionStart; j < partitionEnd; j++) {
                metricThreads[i].offset = i * partitionSize;
                metricThreads[i].metrics = new AbstractFunctionMetric<?>[thisPartionSize];
                metricThreads[i].metrics[startIndex++] = NullHelpers.notNull(allMetrics.get(j));
            }
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
            firstResult = new MultiMetricResult(funcDescription, metricNames, values);
            result = firstResult;
        } else {
            // Less memory/time consuming
            result = new MultiMetricResult(funcDescription, firstResult, values);
        }
        
        return result;
    }
}
