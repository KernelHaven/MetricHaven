package net.ssehub.kernel_haven.metric_haven;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.metrics.AllMetricTests;

/**
 * Test suite for this pluig-in.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    AllMetricTests.class,
    VariationPointerCounterTest.class
    })
public class AllTests {

}
