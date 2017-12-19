package net.ssehub.kernel_haven.metric_haven.multi_results;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import net.ssehub.kernel_haven.util.io.csv.CsvWriter;

/**
 * Tests the {@link MetricsAggregator} and {@link MultiMetricResult}s.
 * 
 * @author Adam
 */
public class MetricsAggregatorTest {
    
    /**
     * Tests two metrics that return values for the same contexts.
     * 
     * @throws IOException unwanted.
     */
    @Test
    public void testTwoMetricsSameContext() throws IOException {
        MetricsAggregator aggregator = new MetricsAggregator();
        
        aggregator.addValue("test.c", null, 1, "funcA", "McCabe", 2.3);
        aggregator.addValue("test.c", null, 1, "funcB", "McCabe", 23.2);
        aggregator.addValue("test.c", null, 1, "funcC", "McCabe", 23.2);
        
        aggregator.addValue("test.c", null, 1, "funcB", "Vars", 3);
        aggregator.addValue("test.c", null, 1, "funcA", "Vars", 2);
        aggregator.addValue("test.c", null, 1, "funcC", "Vars", 8);
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            for (MultiMetricResult result : aggregator.createTable()) {
                out.writeRow(result);
            }
        }
        
        assertThat(str.toString(), is("Source File;Line No.;Element;McCabe;Vars\n"
                + "test.c;1;funcA;2.3;2.0\n"
                + "test.c;1;funcB;23.2;3.0\n"
                + "test.c;1;funcC;23.2;8.0\n"));
    }
    
    /**
     * Tests two metrics that return values for different contexts.
     * 
     * @throws IOException unwanted.
     */
    @Test
    public void testTwoMetricsDifferentContext() throws IOException {
        MetricsAggregator aggregator = new MetricsAggregator();
        
        aggregator.addValue("test.c", null, 1, "funcA", "McCabe", 2.3);
        aggregator.addValue("test.c", null, 1, "funcB", "McCabe", 23.2);
        aggregator.addValue("test.c", null, 1, "funcC", "McCabe", 5.2);
        
        aggregator.addValue("test2.c", null, 1, "funcA", "Vars", 3);
        aggregator.addValue("test.c", null, 1, "funcD", "Vars", 2);
        aggregator.addValue("test.c", "header.h", 1, "funcA", "Vars", 8);
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            for (MultiMetricResult result : aggregator.createTable()) {
                out.writeRow(result);
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
     */
    @Test
    public void testTwoMetricsOverlappingContext() throws IOException {
        MetricsAggregator aggregator = new MetricsAggregator();
        
        aggregator.addValue("test.c", null, 1, "funcA", "McCabe", 2.3);
        aggregator.addValue("test.c", null, 1, "funcB", "McCabe", 23.2);
        
        aggregator.addValue("test.c", null, 1, "funcA", "Vars", 3);
        aggregator.addValue("test.c", null, 1, "funcC", "Vars", 2);
        
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        try (CsvWriter out = new CsvWriter(str)) {
            for (MultiMetricResult result : aggregator.createTable()) {
                out.writeRow(result);
            }
        }
        
        assertThat(str.toString(), is("Source File;Line No.;Element;McCabe;Vars\n"
                + "test.c;1;funcA;2.3;3.0\n"
                + "test.c;1;funcB;23.2;\n"
                + "test.c;1;funcC;;2.0\n"
        ));
    }

}
