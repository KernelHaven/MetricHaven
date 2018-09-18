package net.ssehub.kernel_haven.metric_haven.filter_components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeReaderTest;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeWriterTest;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.VariabilityCounterTest;

/**
 * All test classes for filter components.
 * 
 * @author Adam
 */
@RunWith(Suite.class)
@SuiteClasses({
    VariabilityCounterTest.class,
    ScatteringDegreeReaderTest.class,
    ScatteringDegreeWriterTest.class,
    })
public class AllFilterComponentTests {

}
