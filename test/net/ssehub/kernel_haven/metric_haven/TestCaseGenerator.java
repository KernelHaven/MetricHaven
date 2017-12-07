package net.ssehub.kernel_haven.metric_haven;

import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
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
    public static SyntaxElement externalExpressionFunc() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aORb = new Disjunction(varA, varB);
        
        // AST
        SyntaxElement func = new SyntaxElement(SyntaxElementTypes.FUNCTION_DEF, aORb, aORb);
        
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
    public static SyntaxElement cyclomaticFunction() {
        // Outer expression
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        // AST
        SyntaxElement func = new SyntaxElement(SyntaxElementTypes.FUNCTION_DEF, varA, varA);
        SyntaxElement ifStatement = new SyntaxElement(SyntaxElementTypes.IF_STATEMENT, varA, varA);
        SyntaxElement elseStatement = new SyntaxElement(SyntaxElementTypes.ELIF_STATEMENT, varA, varA);
        SyntaxElement whileStatement = new SyntaxElement(SyntaxElementTypes.WHILE_STATEMENT, varB, aANDb);
        
        func.addNestedElement(ifStatement);
        func.addNestedElement(elseStatement);
        func.addNestedElement(whileStatement);
        
        return func;
    }
    
}
