package net.ssehub.kernel_haven.metric_haven.multi_results;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.io.csv.CsvWriter;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link MetricsAggregator} and {@link MultiMetricResult}s.
 * 
 * @author Adam
 */
public class MetricsAggregatorTest {
    
    /**
     * Creates a {@link MetricsAggregator} for the given input metrics.
     * 
     * @param names The names for the input metrics.
     * @param metrics The input metric results.
     * @return A {@link MetricsAggregator} for the given inputs.
     * 
     * @throws SetUpException Shouldn't happen.
     */
    private MetricsAggregator createAggreagtor(String[] names, MetricResult[]... metrics) throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        
        @SuppressWarnings("unchecked")
        @NonNull AnalysisComponent<MetricResult>[] inputs = new @NonNull AnalysisComponent[names.length];
        
        for (int i = 0; i < names.length; i++) {
            inputs[i] = new TestAnalysisComponentProvider<>(names[i], metrics[i]);
        }
        
        MetricsAggregator result = new MetricsAggregator(config, inputs);
        return result;
    }
    
    /**
     * Tests two metrics that return values for the same contexts.
     * 
     * @throws IOException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testTwoMetricsSameContext() throws IOException, SetUpException {
        MetricsAggregator aggregator = createAggreagtor(
                new String[] {"McCabe", "Vars"},
                new MetricResult[] {
                    new MetricResult(new File("test.c"), null, 1, "funcA", 2.3),
                    new MetricResult(new File("test.c"), null, 1, "funcB", 23.2),
                    new MetricResult(new File("test.c"), null, 1, "funcC", 23.2),
                },
                new MetricResult[] {
                    new MetricResult(new File("test.c"), null, 1, "funcA", 3),
                    new MetricResult(new File("test.c"), null, 1, "funcB", 2),
                    new MetricResult(new File("test.c"), null, 1, "funcC", 8),
                }
        );
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            MultiMetricResult result;
            while ((result = aggregator.getNextResult()) != null) {
                out.writeObject(result);
            }
        }
        
        assertThat(str.toString(), is("Source File;Line No.;Element;McCabe;Vars\n"
                + "test.c;1;funcA;2.3;3.0\n"
                + "test.c;1;funcB;23.2;2.0\n"
                + "test.c;1;funcC;23.2;8.0\n"));
    }
    
    /**
     * Tests two metrics that return values for different contexts.
     * 
     * @throws IOException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testTwoMetricsDifferentContext() throws IOException, SetUpException {
        MetricsAggregator aggregator = createAggreagtor(
                new String[] {"McCabe", "Vars"},
                new MetricResult[] {
                    new MetricResult(new File("test.c"), null, 1, "funcA", 2.3),
                    new MetricResult(new File("test.c"), null, 1, "funcB", 23.2),
                    new MetricResult(new File("test.c"), null, 1, "funcC", 5.2),
                },
                new MetricResult[] {
                    new MetricResult(new File("test2.c"), null, 1, "funcA", 3),
                    new MetricResult(new File("test.c"), null, 1, "funcD", 2),
                    new MetricResult(new File("test.c"), new File("header.h"), 1, "funcA", 8),
                }
        );
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            MultiMetricResult result;
            while ((result = aggregator.getNextResult()) != null) {
                out.writeObject(result);
            }
        }
        
        assertThat(str.toString(), is("Source File;Included File;Line No.;Element;McCabe;Vars\n"
                + "test.c;;1;funcA;2.3;\n"
                + "test.c;;1;funcB;23.2;\n"
                + "test.c;;1;funcC;5.2;\n"
                + "test.c;;1;funcD;;2.0\n"
                + "test.c;header.h;1;funcA;;8.0\n"
                + "test2.c;;1;funcA;;3.0\n"
        ));
    }
    
    /**
     * Tests two metrics that return values for partly overlapping contexts.
     * 
     * @throws IOException unwanted.
     * @throws SetUpException unwanted.
     */
    @Test
    public void testTwoMetricsOverlappingContext() throws IOException, SetUpException {
        MetricsAggregator aggregator = createAggreagtor(
                new String[] {"McCabe", "Vars"},
                new MetricResult[] {
                    new MetricResult(new File("test.c"), null, 1, "funcA", 2.3),
                    new MetricResult(new File("test.c"), null, 1, "funcB", 23.2),
                },
                new MetricResult[] {
                    new MetricResult(new File("test.c"), null, 1, "funcA", 3),
                    new MetricResult(new File("test.c"), null, 1, "funcC", 2),
                }
        );
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            MultiMetricResult result;
            while ((result = aggregator.getNextResult()) != null) {
                out.writeObject(result);
            }
        }
        
        assertThat(str.toString(), is("Source File;Line No.;Element;McCabe;Vars\n"
                + "test.c;1;funcA;2.3;3.0\n"
                + "test.c;1;funcB;23.2;\n"
                + "test.c;1;funcC;;2.0\n"
        ));
    }

}
