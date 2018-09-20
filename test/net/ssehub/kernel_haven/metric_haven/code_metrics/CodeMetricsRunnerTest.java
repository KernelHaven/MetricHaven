package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.AllAstTests;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;

/**
 * Tests the {@link CodeMetricsRunner}.
 * 
 * @author Adam
 */
public class CodeMetricsRunnerTest {

    /**
     * Tests a simple scenario.
     * 
     * @throws SetUpException unwanted.
     */
    @SuppressWarnings("null")
    @Test
    public void testSimple() throws SetUpException {
        
        ISyntaxElement fullAst = AllAstTests.createFullAst();
        Function f = (Function) fullAst.getNestedElement(1);
        SourceFile sourceFile = new SourceFile(fullAst.getSourceFile());
        sourceFile.addElement(fullAst);
        
        CodeFunction f1 = new CodeFunction("simpleFunction", f, sourceFile);
        
        List<MultiMetricResult> result = AnalysisComponentExecuter.executeComponent(CodeMetricsRunner.class,
                new TestConfiguration(new Properties()), new CodeFunction[] {f1});
        
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getMetrics(), is(new String[] {"LoC No weight", "LoF No weight", "PLoF No weight"}));
        assertThat(result.get(0).getValues(), is(new Double[] {14.0, 0.0, 0.0}));

        assertThat(result.get(0).getMeasuredItem().getElement(), is("simpleFunction"));
        assertThat(result.get(0).getMeasuredItem().getMainFile(), is("dummy_test.c"));
        assertThat(result.get(0).getMeasuredItem().getLineNo(), is(-1));
    }
    
}
