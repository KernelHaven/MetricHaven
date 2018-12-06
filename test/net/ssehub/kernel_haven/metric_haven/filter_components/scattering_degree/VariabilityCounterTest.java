package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.CppBlock.Type;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link VariabilityCounter} component.
 *
 * @author Adam
 */
public class VariabilityCounterTest {
    
    /**
     * Tests counting in ifdefs in a single file with a variable that is not present in the variability model.
     */
    @Test
    public void testNonVariabilityVariable() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE, and("CONFIG_A", "CONFIG_B"), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE, and("NOT_A_CONFIG", "CONFIG_A"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1));
        
        assertThat(result.getSDVariationPoint("CONFIG_A"), is(2));
        assertThat(result.getSDVariationPoint("CONFIG_B"), is(1));
        
        assertThat(result.getSDFile("CONFIG_A"), is(1));
        assertThat(result.getSDFile("CONFIG_B"), is(1));
        
        assertThat(result.getSize(), is(2));
    }
    
    /**
     * Tests counting variables in a complex condition.
     */
    @Test
    public void testComplexCondition() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE, and(or(True.INSTANCE, not("CONFIG_A")), "CONFIG_B"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1));
        
        assertThat(result.getSDVariationPoint("CONFIG_A"), is(1));
        assertThat(result.getSDVariationPoint("CONFIG_B"), is(1));
        
        assertThat(result.getSDFile("CONFIG_A"), is(1));
        assertThat(result.getSDFile("CONFIG_B"), is(1));
        
        assertThat(result.getSize(), is(2));
    }
    
    /**
     * Tests counting a condition with the same variable multiples times in it.
     */
    @Test
    public void testMultipleTimesInSameCondition() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE, and(or(False.INSTANCE, not("CONFIG_A")), "CONFIG_A"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1));
        
        assertThat(result.getSDVariationPoint("CONFIG_A"), is(1));
        assertThat(result.getSDVariationPoint("CONFIG_B"), is(0));
        
        assertThat(result.getSDFile("CONFIG_A"), is(1));
        assertThat(result.getSDFile("CONFIG_B"), is(0));
        
        assertThat(result.getSize(), is(2));
    }
    
    /**
     * Tests counting in multiple different files.
     */
    @Test
    public void testMultipleFiles() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file.c"));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_B"), Type.IF));
        
        SourceFile<ISyntaxElement> file2 = new SourceFile<>(new File("some/other/file.c"));
        file2.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1, file2));

        assertThat(result.getSDVariationPoint("CONFIG_A"), is(2));
        assertThat(result.getSDVariationPoint("CONFIG_B"), is(1));
        
        assertThat(result.getSDFile("CONFIG_A"), is(2));
        assertThat(result.getSDFile("CONFIG_B"), is(1));
        
        assertThat(result.getSize(), is(2));
    }
    
    /**
     * Tests counting _MODULE variables.
     */
    @Test
    public void testModuleVariables() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE, or("CONFIG_A", "CONFIG_A_MODULE"), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A_MODULE"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "tristate"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1));
        
        assertThat(result.getSDVariationPoint("CONFIG_A"), is(2));
        
        assertThat(result.getSDFile("CONFIG_A"), is(1));
        
        assertThat(result.getSize(), is(1));
    }
    
    /**
     * Runs the {@link VariabilityCounter} on the given input and returns its output as a list.
     * 
     * @param varModel The variability model to run on.
     * @param sourceFiles The code model to run on.
     * 
     * @return The result of the component.
     */
    private ScatteringDegreeContainer runComponent(VariabilityModel varModel,
            List<SourceFile<?>> sourceFiles) {
        ScatteringDegreeContainer result = null;
        try {
            Properties prop = new Properties();
            Configuration config = new TestConfiguration(prop);
            
            AnalysisComponent<SourceFile<?>> cmComponent
                    = new TestAnalysisComponentProvider<>(sourceFiles);
            AnalysisComponent<VariabilityModel> vmComponent = new TestAnalysisComponentProvider<>(varModel);
            VariabilityCounter counter = new VariabilityCounter(config, vmComponent, cmComponent);
            
            result = counter.getNextResult();
        } catch (SetUpException e) {
            e.printStackTrace();
            Assert.fail("Settings up component failed due: " + e.getMessage());
        }
        
        return result;
    }
    
}
