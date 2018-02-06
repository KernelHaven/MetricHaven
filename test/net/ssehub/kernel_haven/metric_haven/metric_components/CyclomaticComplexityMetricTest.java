package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.TestCaseGenerator;
import net.ssehub.kernel_haven.metric_haven.filter_components.OldCodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric.CCType;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.util.Logger;

/**
 * Tests the {@link CyclomaticComplexityMetric}.
 * @author El-Sharkawy
 *
 */
public class CyclomaticComplexityMetricTest {
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }
    
    /**
     * Tests computation of classical McCabe.
     */
    @Test
    public void testClassicalMcCabe() {
        SyntaxElement testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CCType.MCCABE);
        
        Assert.assertEquals(4, (int) mcCabe);
    }
    
    /**
     * Tests computation of McCabe on variation points.
     */
    @Test
    public void testVPMcCabe() {
        SyntaxElement testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CCType.VARIATION_POINTS);
        
        Assert.assertEquals(2, (int) mcCabe);
    }
    
    /**
     * Tests computation of McCabe combined on classical code and variation points.
     */
    @Test
    public void testCombinedMcCabe() {
        SyntaxElement testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CCType.ALL);
        
        Assert.assertEquals(6, (int) mcCabe);
    }

    /**
     * Calls the {@link CyclomaticComplexityMetric} on the function to test (AST to test).
     * @param testFunc The Function/AST to test.
     * @param type Specifies, which kind of code type should be used to measure the cyclomatic complexity.
     * @return The result of the Metric
     */
    private double runMetricOnFunction(SyntaxElement testFunc, CCType type) {
        MetricResult result = null;
        try {
            // Configuration
            Properties prop = new Properties();
            prop.setProperty(CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING.getKey(), type.name());
            Configuration config = new Configuration(prop);
            config.registerSetting(DefaultSettings.ANALYSIS_COMPONENTS_LOG);
            
            // AST
            // Create virtual files
            File file1 = new File("file1.c");
            SourceFile sourceFile1 = new SourceFile(file1);
            if (testFunc != null) {
                sourceFile1.addElement(testFunc);
            }
            AnalysisComponent<SourceFile> cmComponent = new TestAnalysisComponentProvider<SourceFile>(sourceFile1);
            OldCodeFunctionFilter funcFilter = new OldCodeFunctionFilter(config, cmComponent);
            
            // Metric
            CyclomaticComplexityMetric metric = new CyclomaticComplexityMetric(config, funcFilter);
            metric.execute();
            result = metric.getNextResult();
        } catch (SetUpException e) {
            Assert.fail("Settings up metric \"" + CyclomaticComplexityMetric.class.getSimpleName() + "\" failed due: "
                + e.getMessage());
        }
        
        Assert.assertNotNull(result);
        return result.getValue();
    }
}
