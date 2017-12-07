package net.ssehub.kernel_haven.metric_haven.metrics;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetricTest;

/**
 * Tests suite to test all metrics.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    VariablesPerFunctionMetricTests.class,
    CyclomaticComplexityMetricTest.class
    })
public class AllMetricTests {

}
