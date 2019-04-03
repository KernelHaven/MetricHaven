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
package net.ssehub.kernel_haven.metric_haven;

import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement.Type;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement.LoopType;
import net.ssehub.kernel_haven.code_model.simple_ast.SyntaxElement;
import net.ssehub.kernel_haven.code_model.simple_ast.SyntaxElementTypes;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Utility class to generate test cases for test methods.
 * @author El-Sharkawy
 *
 */
public class TestCaseGenerator {
    
    /**
     * Generates a function with an expression containing 2 variables outside of the function, no expression inside.
     * <br/><br/>
     * <b>Condition:</b><tt>A or B </tt>
     * @return An empty function with the specified expression.
     */
    public static Function externalExpressionFunc() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aORb = new Disjunction(varA, varB);
        
        // AST
        Code header = new Code(aORb, "void func()");
        Function func = new Function(aORb, "func", header);
        
        return func;
    }
    
    /**
     * Generates a function with an expression containing 2 variables inside of the function, no expression inside.
     * <br/><br/>
     * <b>Condition:</b><tt>A and B </tt>
     * @return An empty function with the specified expression.
     */
    public static SyntaxElement internalExpressionFunc() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        // AST
        SyntaxElement func = new SyntaxElement(SyntaxElementTypes.FUNCTION_DEF, True.INSTANCE, True.INSTANCE);
        SyntaxElement statement = new SyntaxElement(SyntaxElementTypes.EMPTY_STATEMENT, aANDb, aANDb);
        func.addNestedElement(statement);
        
        return func;
    }
    
    /**
     * Generates a function with a surrounding and an internal expression.
     * <br/><br/>
     * <b>External Condition:</b><tt>A and B </tt>
     * <b>Internal Condition:</b><tt>A OR C </tt>
     * @return An empty function with the specified expression.
     */
    public static SyntaxElement allExpressionFunc() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Variable varC = new Variable("C");
        Formula aANDb = new Conjunction(varA, varB);
        Formula aORc = new Disjunction(varA, varC);
        
        // AST
        SyntaxElement func = new SyntaxElement(SyntaxElementTypes.FUNCTION_DEF, aANDb, aANDb);
        SyntaxElement statement = new SyntaxElement(SyntaxElementTypes.EMPTY_STATEMENT, aORc,
            new Conjunction(aORc, aANDb));
        func.addNestedElement(statement);
        
        return func;
    }

    /**
     * Generates a function with different cylcomatic complexity for the VPs as well as for the classical code.
     * @return An function with cyclomatic complexity. Simulates:
     * <pre><code>
     * #ifdef A
     * func() {
     *     if() {     // McCabe + 1
     *         ...
     *     } elseif { // McCabe + 1
     *        ...
     *     }
     *     
     *     #ifdef B   // VP + 1
     *     while () { // McCabe + 1
     *         ...
     *     }
     *     #endif
     * }
     * </code></pre>
     */
    public static Function cyclomaticFunction() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        // AST
        Code header = new Code(varA, "void func()");
        Function func = new Function(varA, "func", header);
        BranchStatement ifStatement = new BranchStatement(varA, Type.IF, new Code(varA, "if(a)"));
        BranchStatement elseStatement = new BranchStatement(varA, Type.ELSE_IF, new Code(varA, "else"));
        CppBlock ifDef = new CppBlock(aANDb, varB, CppBlock.Type.IFDEF);
        LoopStatement whileStatement = new LoopStatement(aANDb, new Code(varA, "while(true)"), LoopType.WHILE);
        ifDef.addNestedElement(whileStatement);
        
        func.addNestedElement(ifStatement);
        func.addNestedElement(elseStatement);
        func.addNestedElement(ifDef);
        
        return func;
    }
    
}
