package net.ssehub.kernel_haven.metric_haven.filter_components;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.CppBlock.Type;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Negation;
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
     * Inits the logger.
     */
    @BeforeClass
    public static void initLogger() {
        Logger.init();
    }

    /**
     * Tests counting in ifdefs in a single file with a variable that is not present in the variability model.
     */
    @Test
    public void testNonVariabilityVariable() {
        SourceFile file1 = new SourceFile(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE,
                new Conjunction(new Variable("CONFIG_A"), new Variable("CONFIG_B")), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE,
                new Conjunction(new Variable("NOT_A_CONFIG"), new Variable("CONFIG_A")), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        List<ScatteringDegree> result = runComponent(varModel, Arrays.asList(file1));
        
        ScatteringDegree varA = result.get(0);
        ScatteringDegree varB = result.get(1);

        if (!varA.getVariable().getName().equals("CONFIG_A")) {
            varA = result.get(1);
            varB = result.get(0);
        }
        
        assertThat(varA.getVariable().getName(), is("CONFIG_A"));
        assertThat(varB.getVariable().getName(), is("CONFIG_B"));
        
        assertThat(varA.getIfdefs(), is(2));
        assertThat(varB.getIfdefs(), is(1));
        
        assertThat(varA.getFiles(), is(1));
        assertThat(varB.getFiles(), is(1));
        
        assertThat(result.size(), is(2));
    }
    
    /**
     * Tests counting variables in a complex condition.
     */
    @Test
    public void testComplexCondition() {
        SourceFile file1 = new SourceFile(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE,
                new Conjunction(new Disjunction(True.INSTANCE, new Negation(new Variable("CONFIG_A"))),
                        new Variable("CONFIG_B")), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        List<ScatteringDegree> result = runComponent(varModel, Arrays.asList(file1));
        
        ScatteringDegree varA = result.get(0);
        ScatteringDegree varB = result.get(1);

        if (!varA.getVariable().getName().equals("CONFIG_A")) {
            varA = result.get(1);
            varB = result.get(0);
        }
        
        assertThat(varA.getVariable().getName(), is("CONFIG_A"));
        assertThat(varB.getVariable().getName(), is("CONFIG_B"));
        
        assertThat(varA.getIfdefs(), is(1));
        assertThat(varB.getIfdefs(), is(1));
        
        assertThat(varA.getFiles(), is(1));
        assertThat(varB.getFiles(), is(1));
        
        assertThat(result.size(), is(2));
    }
    
    /**
     * Tests counting a condition with the same variable multiples times in it.
     */
    @Test
    public void testMultipleTimesInSameCondition() {
        SourceFile file1 = new SourceFile(new File("some/file.c"));
        
        file1.addElement(new CppBlock(True.INSTANCE,
                new Conjunction(new Disjunction(False.INSTANCE, new Negation(new Variable("CONFIG_A"))),
                        new Variable("CONFIG_A")), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        List<ScatteringDegree> result = runComponent(varModel, Arrays.asList(file1));
        
        ScatteringDegree varA = result.get(0);
        ScatteringDegree varB = result.get(1);

        if (!varA.getVariable().getName().equals("CONFIG_A")) {
            varA = result.get(1);
            varB = result.get(0);
        }
        
        assertThat(varA.getVariable().getName(), is("CONFIG_A"));
        assertThat(varB.getVariable().getName(), is("CONFIG_B"));
        
        assertThat(varA.getIfdefs(), is(1));
        assertThat(varB.getIfdefs(), is(0));
        
        assertThat(varA.getFiles(), is(1));
        assertThat(varB.getFiles(), is(0));
        
        assertThat(result.size(), is(2));
    }
    
    /**
     * Tests counting in multiple different files.
     */
    @Test
    public void testMultipleFiles() {
        SourceFile file1 = new SourceFile(new File("some/file.c"));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_B"), Type.IF));
        
        SourceFile file2 = new SourceFile(new File("some/other/file.c"));
        file2.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        List<ScatteringDegree> result = runComponent(varModel, Arrays.asList(file1, file2));
        
        ScatteringDegree varA = result.get(0);
        ScatteringDegree varB = result.get(1);

        if (!varA.getVariable().getName().equals("CONFIG_A")) {
            varA = result.get(1);
            varB = result.get(0);
        }
        
        assertThat(varA.getVariable().getName(), is("CONFIG_A"));
        assertThat(varB.getVariable().getName(), is("CONFIG_B"));
        
        assertThat(varA.getIfdefs(), is(2));
        assertThat(varB.getIfdefs(), is(1));
        
        assertThat(varA.getFiles(), is(2));
        assertThat(varB.getFiles(), is(1));
        
        assertThat(result.size(), is(2));
    }
    
    /**
     * Runs the {@link VariabilityCounter} on the given input and returns its output as a list.
     * 
     * @param varModel The variability model to run on.
     * @param sourceFiles The code model to run on.
     * 
     * @return The result of the component.
     */
    private List<ScatteringDegree> runComponent(VariabilityModel varModel, List<SourceFile> sourceFiles) {
        List<ScatteringDegree> result = new ArrayList<>();
        try {
            Properties prop = new Properties();
            Configuration config = new TestConfiguration(prop);
            
            AnalysisComponent<SourceFile> cmComponent = new TestAnalysisComponentProvider<>(sourceFiles);
            AnalysisComponent<VariabilityModel> vmComponent = new TestAnalysisComponentProvider<>(varModel);
            VariabilityCounter counter = new VariabilityCounter(config, vmComponent, cmComponent);
            
            ScatteringDegree variable;
            while ((variable = counter.getNextResult()) != null) {
                result.add(variable);
            }
            
        } catch (SetUpException e) {
            e.printStackTrace();
            Assert.fail("Settings up component failed due: " + e.getMessage());
        }
        
        return result;
    }
    
}
