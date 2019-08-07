/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner;
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
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1), null);
        
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
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1), null);
        
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
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1), null);
        
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
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1, file2), null);

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
        
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1), null);
        
        assertThat(result.getSDVariationPoint("CONFIG_A"), is(2));
        
        assertThat(result.getSDFile("CONFIG_A"), is(1));
        
        assertThat(result.getSize(), is(1));
    }
    
    /**
     * Tests running in multiple threads.
     */
    @Test
    public void testMultiThreaded() {
        SourceFile<ISyntaxElement> file1 = new SourceFile<>(new File("some/file1.c"));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        file1.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_B"), Type.IF));
        
        SourceFile<ISyntaxElement> file2 = new SourceFile<>(new File("some/other/file2.c"));
        file2.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A"), Type.IF));
        
        SourceFile<ISyntaxElement> file3 = new SourceFile<>(new File("some/file3.c"));
        file3.addElement(new CppBlock(True.INSTANCE, or("CONFIG_A", "CONFIG_A_MODULE"), Type.IF));
        file3.addElement(new CppBlock(True.INSTANCE, new Variable("CONFIG_A_MODULE"), Type.IF));
        
        SourceFile<ISyntaxElement> file4 = new SourceFile<>(new File("some/file4.c"));
        file4.addElement(new CppBlock(True.INSTANCE, and(or(False.INSTANCE, not("CONFIG_A")), "CONFIG_A"), Type.IF));
        
        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        VariabilityModel varModel = new VariabilityModel(new File("not_existing.vm"), variables);
        
        Properties prop = new Properties();
        prop.put(CodeMetricsRunner.MAX_THREADS.getKey(), "2");
        prop = null;
        ScatteringDegreeContainer result = runComponent(varModel, Arrays.asList(file1, file2, file3, file4), prop);

        assertThat(result.getSDVariationPoint("CONFIG_A"), is(5));
        assertThat(result.getSDVariationPoint("CONFIG_B"), is(1));
        
        assertThat(result.getSDFile("CONFIG_A"), is(4));
        assertThat(result.getSDFile("CONFIG_B"), is(1));
        
        assertThat(result.getSize(), is(2));
    }
    
    /**
     * Runs the {@link VariabilityCounter} on the given input and returns its output as a list.
     * 
     * @param varModel The variability model to run on.
     * @param sourceFiles The code model to run on.
     * @param prop Optional configuration parameters, maybe empty or <tt>null</tt>.
     * 
     * @return The result of the component.
     */
    private ScatteringDegreeContainer runComponent(VariabilityModel varModel,
            List<SourceFile<?>> sourceFiles, Properties prop) {
        ScatteringDegreeContainer result = null;
        try {
            if (null == prop) {
                prop = new Properties();
            }
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
