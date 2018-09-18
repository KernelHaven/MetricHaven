package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link ScatteringDegreeReader}.
 * 
 * @author Adam
 */
public class ScatteringDegreeReaderTest {

    /**
     * Tests that reading an existing file produces expected results.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testReadExistingFile() throws SetUpException {
        File cacheFile = new File("testdata/scatteringDegreeCache.csv");
        assertThat(cacheFile.isFile(), is(true));
        
        Properties props = new Properties();
        props.setProperty(ScatteringDegreeReader.SD_CACHE_FILE.getKey(), cacheFile.getPath());
        TestConfiguration config = new TestConfiguration(props);
        
        Set<VariabilityVariable> vars = new HashSet<>();
        vars.add(new VariabilityVariable("VAR_A", "bool"));
        vars.add(new VariabilityVariable("VAR_B", "bool"));
        vars.add(new VariabilityVariable("VAR_C", "bool"));
        
        VariabilityModel vm = new VariabilityModel(new File("doesnt_exist"), vars);
        
        List<Object> result = AnalysisComponentExecuter.executeComponent(ScatteringDegreeReader.class, config,
            new VariabilityModel[] {
                vm
            });
        
        assertThat(result.size(), is(1));
        
        ScatteringDegreeContainer container = (ScatteringDegreeContainer) result.get(0);
        
        assertThat(container.getSDVariationPoint("VAR_A"), is(5));
        assertThat(container.getSDFile("VAR_A"), is(3));
        
        assertThat(container.getSDVariationPoint("VAR_B"), is(6));
        assertThat(container.getSDFile("VAR_B"), is(2));
        
        assertThat(container.getSDVariationPoint("VAR_C"), is(8));
        assertThat(container.getSDFile("VAR_C"), is(4));
    }
    
}
