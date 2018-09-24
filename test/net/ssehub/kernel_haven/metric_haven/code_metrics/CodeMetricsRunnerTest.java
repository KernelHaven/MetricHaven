package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.AllAstTests;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;

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
        
        VariabilityModel varModel = new VariabilityModel(new File("doesnt_exist"), new HashSet<>());
        varModel.getDescriptor().addAttribute(Attribute.CONSTRAINT_USAGE);
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        varModel.getDescriptor().addAttribute(Attribute.HIERARCHICAL);
        BuildModel bm = new BuildModel();
        ScatteringDegreeContainer sdc = new ScatteringDegreeContainer(new HashSet<>());
        
        List<MultiMetricResult> result = AnalysisComponentExecuter.executeComponent(CodeMetricsRunner.class,
                new TestConfiguration(new Properties()),
                new CodeFunction[] {f1},
                new VariabilityModel[] {varModel},
                new BuildModel[] {bm},
                new ScatteringDegreeContainer[] {sdc});
        
        assertThat(result.size(), is(1));
        
        assertThat(result.get(0).getMetrics().length, is(5189));
        assertThat(result.get(0).getValues().length, is(5189));
        // TODO: not all metrics are currently implemented, should be:
//      assertThat(result.get(0).getMetrics().length, is(7358));
//      assertThat(result.get(0).getValues().length, is(7358));
        
        assertThat(result.get(0).getValues()[0], is(14.0));
        assertThat(result.get(0).getValues()[1], is(0.0));
        assertThat(result.get(0).getValues()[2], is(0.0));

        assertThat(result.get(0).getMeasuredItem().getElement(), is("simpleFunction"));
        assertThat(result.get(0).getMeasuredItem().getMainFile(), is("dummy_test.c"));
        assertThat(result.get(0).getMeasuredItem().getLineNo(), is(-1));
    }
    
}
