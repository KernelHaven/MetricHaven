package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.io.csv.CsvReader;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link ScatteringDegreeWriter}.
 * 
 * @author Adam
 */
public class ScatteringDegreeWriterTest {
    
    /**
     * Tests that the result file has correct content.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    @SuppressWarnings("null")
    public void testResultFile() throws SetUpException, IOException {
        File cacheFile = new File("testdata/tmpSdCache.csv");
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        
        assertThat(cacheFile.exists(), is(false));
        
        Properties props = new Properties();
        props.setProperty(ScatteringDegreeReader.SD_CACHE_FILE.getKey(), cacheFile.getPath());
        TestConfiguration config = new TestConfiguration(props);
        
        List<ScatteringDegree> sds = new LinkedList<>();
        sds.add(new ScatteringDegree(new VariabilityVariable("VAR_A", "bool"), 5, 3));
        sds.add(new ScatteringDegree(new VariabilityVariable("VAR_B", "bool"), 6, 2));
        sds.add(new ScatteringDegree(new VariabilityVariable("VAR_C", "bool"), 8, 4));
        
        ScatteringDegreeContainer sdContainer = new ScatteringDegreeContainer(sds);
        
        List<Object> result = AnalysisComponentExecuter.executeComponent(ScatteringDegreeWriter.class, config,
            new ScatteringDegreeContainer[] {
                sdContainer
            });
        
        assertThat(result.size(), is(1));
        assertThat(result.get(0), sameInstance(sdContainer));
        
        try (CsvReader in = new CsvReader(new FileInputStream(cacheFile))) {
            assertThat(in.readNextRow(), is(new String[] {"Variable", "#ifdef Count", "File Count"}));
            assertThat(in.readNextRow(), is(new String[] {"VAR_B", "6", "2"}));
            assertThat(in.readNextRow(), is(new String[] {"VAR_C", "8", "4"}));
            assertThat(in.readNextRow(), is(new String[] {"VAR_A", "5", "3"}));
            
            assertThat(in.readNextRow(), nullValue());
        }
        
        cacheFile.delete();
    }

}
