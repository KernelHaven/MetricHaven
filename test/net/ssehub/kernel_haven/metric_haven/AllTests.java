package net.ssehub.kernel_haven.metric_haven;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.metric_components.AllMetricComponentTests;
import net.ssehub.kernel_haven.metric_haven.multi_results.AllMultiResultTests;

/**
 * Test suite for this pluig-in.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    AllMetricComponentTests.class,
    AllMultiResultTests.class,
    })
public class AllTests {

}
