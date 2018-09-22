package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.TestCaseGenerator;
import net.ssehub.kernel_haven.metric_haven.code_metrics.CyclomaticComplexity;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;

/**
 * Tests the {@link CyclomaticComplexityMetric}.
 * @author El-Sharkawy
 *
 */
public class CyclomaticComplexityMetricTest {
    
    /**
     * Tests computation of classical McCabe.
     */
    @Test
    public void testClassicalMcCabe() {
        Function testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CyclomaticComplexity.CCType.MCCABE);
        
        Assert.assertEquals(4, (int) mcCabe);
    }
    
    /**
     * Tests computation of McCabe on variation points.
     */
    @Test
    public void testVPMcCabe() {
        Function testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CyclomaticComplexity.CCType.VARIATION_POINTS);
        
        Assert.assertEquals(2, (int) mcCabe);
    }
    
    /**
     * Tests computation of McCabe combined on classical code and variation points.
     */
    @Test
    public void testCombinedMcCabe() {
        Function testFunction = TestCaseGenerator.cyclomaticFunction();
        double mcCabe = runMetricOnFunction(testFunction, CyclomaticComplexity.CCType.ALL);
        
        Assert.assertEquals(6, (int) mcCabe);
    }

    /**
     * Calls the {@link CyclomaticComplexityMetric} on the function to test (AST to test).
     * @param testFunc The Function/AST to test.
     * @param type Specifies, which kind of code type should be used to measure the cyclomatic complexity.
     * @return The result of the Metric
     */
    private double runMetricOnFunction(Function testFunc, CyclomaticComplexity.CCType type) {
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
            CodeFunctionFilter funcFilter = new CodeFunctionFilter(config, cmComponent);
            
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
