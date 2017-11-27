package net.ssehub.kernel_haven.metrics.example;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests suite to test all metrics.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    VariablesPerFunctionMetricTests.class
    })
public class AllMetricTests {

}
