package net.ssehub.kernel_haven.metric_haven;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link VariationPointerCounter}.
 * @author El-Sharkawy
 *
 */
public class VariationPointerCounterTest {

    /**
     * Test the correct result of 0 variation points if there was no formula passed.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     *   ...
     * }
     * </code></pre>
     */
    @Test
    public void testNoVP() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Assert.assertEquals(0, counter.countVPs());
    }
    
    /**
     * Tests that the {@link VariationPointerCounter} can be initialized with a {@link Formula}, which is used as
     * baseline.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * #ifdef(A)
     * function() {
     *   ...
     * }
     * #endif
     * </code></pre>
     */
    @Test
    public void testInitializeWithFormula() {
        VariationPointerCounter counter = new VariationPointerCounter(new Variable("A"));
        Assert.assertEquals(0, counter.countVPs());
    }
    
    /**
     * Tests the correct determination of a new variation point by means of a new presence condition.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testNewVariationPoint() {
        VariationPointerCounter counter = new VariationPointerCounter();
        counter.add(new Variable("A"));
        Assert.assertEquals(1, counter.countVPs());        
    }
    
    /**
     * Tests that an alternative to a variation point is not counted as new variation point.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #else
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testAlternativeBlockOfVP() {
        VariationPointerCounter counter = new VariationPointerCounter();
        counter.add(new Variable("A"));
        counter.add(new Negation(new Variable("A")));
        Assert.assertEquals(1, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination of a nested variation points.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #ifdef(B)
     *   ...
     * #endif
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testNestedVariationPoints() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula nested = new Conjunction(varA, varB);
        counter.add(varA);
        counter.add(nested);
        Assert.assertEquals(2, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination of a nested variation points together with their alternatives.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #ifdef(B)
     *   ...
     * #else
     *   ...
     * #endif
     * #else
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testNestedVariationPointsWithAlternatives() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula nested = new Conjunction(varA, varB);
        Formula nestedAlternative = new Conjunction(varA, new Negation(varB));
        counter.add(varA);
        counter.add(nested);
        counter.add(nestedAlternative);
        counter.add(new Negation(varA));
        Assert.assertEquals(2, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination of a nested variation points together with
     * an alternative for the toplevel element.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #ifdef(B)
     *   ...
     * #endif
     * #else
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testNestedVariationPointsAndToplevelAlternative() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula nested = new Conjunction(varA, varB);
        counter.add(varA);
        counter.add(nested);
        counter.add(new Negation(varA));
        Assert.assertEquals(2, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination of variation points with an elif statement.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)  \\ +1
     *   ...
     * #ifdef(B)  \\ +1
     *   ...
     * #endif
     * #elif (c)  \\ +1
     *   ...
     * #else
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testElseIfVPs() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Variable varC = new Variable("C");
        Formula nested = new Conjunction(varA, varB);
        Formula elif = new Conjunction(varA, varC);
        counter.add(varA);
        counter.add(nested);
        counter.add(elif);
        counter.add(new Negation(varA));
        Assert.assertEquals(3, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination toplevel variation points.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #endif
     * #ifdef(B)
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testMultipleToplevel() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        counter.add(varA);
        counter.add(varB);
        Assert.assertEquals(2, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination toplevel variation points.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #else
     *   ...
     * #endif
     * #ifdef(B)
     *   ...
     * #else
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testMultipleToplevelWithAlternatives() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        counter.add(varA);
        counter.add(new Negation(varA));
        counter.add(varB);
        counter.add(new Negation(varB));
        Assert.assertEquals(2, counter.countVPs());        
    }

    /**
     * Tests the correct determination of toplevel variation points, with the same expression.<br/>
     * Simulates (if we would measure variation points inside the function):
     * <pre><code>
     * function() {
     * #ifdef(A)
     *   ...
     * #endif
     * #ifdef(B)
     *   ...
     * #endif
     * #ifdef(A)
     *   ...
     * #endif
     * }
     * </code></pre>
     */
    @Test
    public void testReuccringToplevelVP() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        counter.add(varA);
        counter.add(varB);
        counter.add(varA);
        Assert.assertEquals(3, counter.countVPs());        
    }
}
