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
     * Test the correct result of 0 variation points if there was no formula passed.
     */
    @Test
    public void testNoVP() {
        VariationPointerCounter counter = new VariationPointerCounter();
        Assert.assertEquals(0, counter.countVPs());
    }
    
    /**
     * Tests that the {@link VariationPointerCounter} can be initialized with a {@link Formula}, which is used as
     * baseline.
     */
    @Test
    public void testInitializeWithFormula() {
        VariationPointerCounter counter = new VariationPointerCounter(new Variable("A"));
        Assert.assertEquals(0, counter.countVPs());
    }
    
    /**
     * Tests the correct determination of a new variation point by means of a new presence condition.
     */
    @Test
    public void testNewVariationPoint() {
        VariationPointerCounter counter = new VariationPointerCounter();
        counter.add(new Variable("A"));
        Assert.assertEquals(1, counter.countVPs());        
    }
    
    /**
     * Tests that an alternative to a variation point is not counted as new variation point.
     */
    @Test
    public void testAlternativeBlockOfVP() {
        VariationPointerCounter counter = new VariationPointerCounter();
        counter.add(new Variable("A"));
        counter.add(new Negation(new Variable("A")));
        Assert.assertEquals(1, counter.countVPs());        
    }
    
    /**
     * Tests the correct determination of a nested variation points.
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
     * Tests the correct determination of a nested variation points together with their alternatives.
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

}
