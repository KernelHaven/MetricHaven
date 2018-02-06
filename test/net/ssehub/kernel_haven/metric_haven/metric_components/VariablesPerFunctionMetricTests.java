package net.ssehub.kernel_haven.metric_haven.metric_components;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.TestCaseGenerator;
import net.ssehub.kernel_haven.metric_haven.filter_components.OldCodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;

/**
 * Tests the {@link VariablesPerFunctionMetric}.
 * @author El-Sharkawy
 *
 */
public class VariablesPerFunctionMetricTests {
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }

    /**
     * Tests that external variables are detected correctly.
     */
    @Test
    public void testExternalVariables() {
        SyntaxElement testFunc = TestCaseGenerator.externalExpressionFunc();
        int externalResult = runMetricOnFunction(testFunc, VarType.EXTERNAL);
        int internalResult = runMetricOnFunction(testFunc, VarType.INTERNAL);
        int allResult = runMetricOnFunction(testFunc, VarType.ALL);
        
        Assert.assertEquals(2, externalResult);
        Assert.assertEquals(0, internalResult);
        Assert.assertEquals(2, allResult);
    }
    
    /**
     * Tests that internal variables are detected correctly.
     */
    @Test
    public void testInternalVariables() {
        SyntaxElement testFunc = TestCaseGenerator.internalExpressionFunc();
        int externalResult = runMetricOnFunction(testFunc, VarType.EXTERNAL);
        int internalResult = runMetricOnFunction(testFunc, VarType.INTERNAL);
        int allResult = runMetricOnFunction(testFunc, VarType.ALL);
        
        Assert.assertEquals(0, externalResult);
        Assert.assertEquals(2, internalResult);
        Assert.assertEquals(2, allResult);
    }
    
    /**
     * Tests that internal and external variables are detected correctly.
     */
    @Test
    public void testAllVariables() {
        SyntaxElement testFunc = TestCaseGenerator.allExpressionFunc();
        int externalResult = runMetricOnFunction(testFunc, VarType.EXTERNAL);
        int internalResult = runMetricOnFunction(testFunc, VarType.INTERNAL);
        int allResult = runMetricOnFunction(testFunc, VarType.ALL);
        
        // A, B
        Assert.assertEquals(2, externalResult);
        // C (A was already part of outside condition)
        Assert.assertEquals(1, internalResult);
        // A, B, C
        Assert.assertEquals(3, allResult);
    }

    /**
     * Calls the {@link VariablesPerFunctionMetric} on the function to test (AST to test).
     * @param testFunc The Function/AST to test.
     * @param type Specifies, which kind of variables should be detected.
     * @return The result of the Metric
     */
    private int runMetricOnFunction(SyntaxElement testFunc, VarType type) {
        Double result = null;
        try {
            Properties prop = new Properties();
            prop.setProperty(VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING.getKey(), type.name());
            Configuration config = new TestConfiguration(prop);
            
            List<MetricResult> metricResults = AnalysisComponentExecuter.executeComponent(
                    VariablesPerFunctionMetric.class, config,
                    new Object[] {
                        new OldCodeFunction("testfunc", testFunc, new SourceFile(new File("test.c")))
                    });
            
            assertThat(metricResults.size(), is(1));
            
            result = metricResults.get(0).getValue();
            
        } catch (SetUpException e) {
            Assert.fail("Settings up metric \"" + VariablesPerFunctionMetric.class.getSimpleName() + "\" failed due: "
                + e.getMessage());
        }
        
        Assert.assertNotNull(result);
        return result.intValue();
    }

}
