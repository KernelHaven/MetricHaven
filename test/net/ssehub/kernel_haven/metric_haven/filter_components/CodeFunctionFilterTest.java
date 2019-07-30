/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CompoundStatement;
import net.ssehub.kernel_haven.code_model.ast.File;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link CodeFunctionFilter}.
 *
 * @author Adam
 */
public class CodeFunctionFilterTest {

    /**
     * Tests a special case where functions are nested inside other functions.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNestedFunctions() throws SetUpException {
        SourceFile<ISyntaxElement> sourceFile = new SourceFile<>(new java.io.File("test.c"));
        
        File file = new File(True.INSTANCE, sourceFile.getPath());
        file.setLineStart(1);
        file.setLineEnd(10);
        sourceFile.addElement(file);
        
        Function outer = new Function(True.INSTANCE, "outer", new Code(True.INSTANCE, "void outer ()"));
        outer.setLineStart(2);
        outer.setLineEnd(8);
        file.addNestedElement(outer);
        
        CompoundStatement outerBlock = new CompoundStatement(True.INSTANCE);
        outerBlock.setLineStart(3);
        outerBlock.setLineEnd(8);
        outer.addNestedElement(outerBlock);
        
        SingleStatement outerStmt = new SingleStatement(True.INSTANCE, new Code(True.INSTANCE, ";"),
                SingleStatement.Type.INSTRUCTION);
        outerStmt.setLineStart(4);
        outerStmt.setLineEnd(4);
        outerBlock.addNestedElement(outerStmt);
        
        Function inner = new Function(True.INSTANCE, "inner", new Code(True.INSTANCE, "void inner ()"));
        inner.setLineStart(5);
        inner.setLineEnd(7);
        outerBlock.addNestedElement(inner);
        
        CompoundStatement innerBlock = new CompoundStatement(True.INSTANCE);
        innerBlock.setLineStart(5);
        innerBlock.setLineEnd(7);
        inner.addNestedElement(innerBlock);
        
        SingleStatement innerStmt = new SingleStatement(True.INSTANCE, new Code(True.INSTANCE, ";"),
                SingleStatement.Type.INSTRUCTION);
        innerStmt.setLineStart(6);
        innerStmt.setLineEnd(6);
        innerBlock.addNestedElement(innerStmt);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        List<@NonNull CodeFunction> result = AnalysisComponentExecuter.executeComponent(CodeFunctionFilter.class,
                config, new SourceFile<?>[] {sourceFile});
        
        assertThat(result.size(), is(2));
        
        CodeFunction r1 = notNull(result.get(0));
        CodeFunction r2 = notNull(result.get(1));
        
        assertThat(r1.getName(), is("inner"));
        assertThat(r1.getFunction(), sameInstance(inner));
        
        assertThat(r2.getName(), is("outer"));
        assertThat(r2.getFunction(), not(sameInstance(outer)));
        
        Function actualOuter = r2.getFunction();
        assertThat(actualOuter.getLineStart(), is(2));
        assertThat(actualOuter.getLineEnd(), is(8));
        assertThat(actualOuter.getNestedElementCount(), is(1));
        
        CompoundStatement actualOuterBlock = (CompoundStatement) actualOuter.getNestedElement(0);
        assertThat(actualOuterBlock.getLineStart(), is(3));
        assertThat(actualOuterBlock.getLineEnd(), is(8));
        assertThat(actualOuterBlock.getNestedElementCount(), is(1)); // only statement is nested now
        
        SingleStatement actualOuterStmt = (SingleStatement) actualOuterBlock.getNestedElement(0);
        assertThat(actualOuterStmt.getLineStart(), is(4));
        assertThat(actualOuterStmt.getLineEnd(), is(4));
        assertThat(((Code) actualOuterStmt.getCode()).getText(), is(";"));
    }
    
}
