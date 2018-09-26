package net.ssehub.kernel_haven.metric_haven;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.code_metrics.AllCodeMetricsTests;
import net.ssehub.kernel_haven.metric_haven.filter_components.AllFilterComponentTests;
import net.ssehub.kernel_haven.metric_haven.metric_components.AllMetricComponentTests;

/**
 * Test suite for this pluig-in.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    AllFilterComponentTests.class,
    AllMetricComponentTests.class,
    AllCodeMetricsTests.class,
    })
public class AllTests {

}
