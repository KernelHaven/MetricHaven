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
package net.ssehub.kernel_haven.metric_haven.code_metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.AllAstTests;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionLocation;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
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
    @Test
    public void testSimple() throws SetUpException {
        
        ISyntaxElement fullAst = AllAstTests.createFullAst();
        Function f = (Function) fullAst.getNestedElement(1);
        SourceFile<ISyntaxElement> sourceFile = new SourceFile<>(fullAst.getSourceFile());
        sourceFile.addElement(fullAst);
        
        CodeFunction f1 = new CodeFunction("simpleFunction", f, sourceFile);
        
        VariabilityModel varModel = new VariabilityModel(new File("doesnt_exist"), new HashSet<>());
        varModel.getDescriptor().addAttribute(Attribute.CONSTRAINT_USAGE);
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        varModel.getDescriptor().addAttribute(Attribute.HIERARCHICAL);
        BuildModel bm = new BuildModel();
        ScatteringDegreeContainer sdc = new ScatteringDegreeContainer(new HashSet<>());
        FeatureSizeContainer fsContainer = new FeatureSizeContainer(varModel);
        @NonNull Map<String, List<FunctionLocation>> allFunctionLocations = new HashMap<>();
        @NonNull List<@NonNull CodeFunction> allFunctions = new ArrayList<>();
        FunctionMap emptyMap = new FunctionMap(allFunctionLocations, allFunctions);
        
        List<MultiMetricResult> result = AnalysisComponentExecuter.executeComponent(CodeMetricsRunner.class,
                new TestConfiguration(new Properties()),
                new CodeFunction[] {f1},
                new VariabilityModel[] {varModel},
                new BuildModel[] {bm},
                new ScatteringDegreeContainer[] {sdc},
                new FunctionMap[]{emptyMap},
                new FeatureSizeContainer[] {fsContainer});
        
        assertThat(result.size(), is(1));
        
        assertThat(result.get(0).getMetrics().length, is(44086));
        assertThat(result.get(0).getValues().length, is(44086));
        
        assertThat(result.get(0).getValues()[0], is(14.0));
        assertThat(result.get(0).getValues()[1], is(0.0));
        assertThat(result.get(0).getValues()[2], is(0.0));
        
        for (Double value : result.get(0).getValues()) {
            assertThat(value, notNullValue());
        }

        assertThat(result.get(0).getMeasuredItem().getElement(), is("simpleFunction"));
        assertThat(result.get(0).getMeasuredItem().getMainFile(), is("dummy_test.c"));
        assertThat(result.get(0).getMeasuredItem().getLineNo(), is(-1));
    }
    
    /**
     * Tests a simple scenario with multiple threads.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimpleMultithreaded() throws SetUpException {
        
        ISyntaxElement fullAst = AllAstTests.createFullAst();
        Function f = (Function) fullAst.getNestedElement(1);
        SourceFile<ISyntaxElement> sourceFile = new SourceFile<>(fullAst.getSourceFile());
        sourceFile.addElement(fullAst);
        
        CodeFunction f1 = new CodeFunction("simpleFunction", f, sourceFile);
        
        VariabilityModel varModel = new VariabilityModel(new File("doesnt_exist"), new HashSet<>());
        varModel.getDescriptor().addAttribute(Attribute.CONSTRAINT_USAGE);
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        varModel.getDescriptor().addAttribute(Attribute.HIERARCHICAL);
        BuildModel bm = new BuildModel();
        ScatteringDegreeContainer sdc = new ScatteringDegreeContainer(new HashSet<>());
        FeatureSizeContainer fsContainer = new FeatureSizeContainer(varModel);
        @NonNull Map<String, List<FunctionLocation>> allFunctionLocations = new HashMap<>();
        @NonNull List<@NonNull CodeFunction> allFunctions = new ArrayList<>();
        allFunctions.add(f1);
        FunctionMap emptyMap = new FunctionMap(allFunctionLocations, allFunctions);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(CodeMetricsRunner.MAX_THREADS);
        config.setValue(CodeMetricsRunner.MAX_THREADS, 4);
        
        List<MultiMetricResult> result = AnalysisComponentExecuter.executeComponent(CodeMetricsRunner.class,
                config,
                new CodeFunction[] {f1},
                new VariabilityModel[] {varModel},
                new BuildModel[] {bm},
                new ScatteringDegreeContainer[] {sdc},
                new FunctionMap[]{emptyMap},
                new FeatureSizeContainer[] {fsContainer});
        
        assertThat(result.size(), is(1));
        
        assertThat(result.get(0).getMetrics().length, is(44086));
        assertThat(result.get(0).getValues().length, is(44086));
        
        assertThat(result.get(0).getValues()[0], is(14.0));
        assertThat(result.get(0).getValues()[1], is(0.0));
        assertThat(result.get(0).getValues()[2], is(0.0));
        
        for (Double value : result.get(0).getValues()) {
            assertThat(value, notNullValue());
        }

        assertThat(result.get(0).getMeasuredItem().getElement(), is("simpleFunction"));
        assertThat(result.get(0).getMeasuredItem().getMainFile(), is("dummy_test.c"));
        assertThat(result.get(0).getMeasuredItem().getLineNo(), is(-1));
    }
    
}
