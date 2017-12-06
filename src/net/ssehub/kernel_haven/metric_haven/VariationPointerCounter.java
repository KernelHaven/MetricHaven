package net.ssehub.kernel_haven.metric_haven;

import java.util.ArrayDeque;
import java.util.Deque;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;

/**
 * Counts variation points, similarly to McCabe's Cyclomatic Complexity, this metric won't count an else block as a new
 * variation point. This class counts the variation points / code blocks based on different presence conditions.<br/>
 * <b>Note:</b> This is only a heuristic.
 * 
 * @author El-Sharkawy
 *
 */
public class VariationPointerCounter {
    
    // Used as stacks: addFirst(), removeFirst(), peekFirst()
    /**
     * Counts the presence conditions of counted variation points.
     */
    private int nVPs;
    
    private Deque<Formula> formulaStack = new ArrayDeque<>();
    
    /**
     * Traces the possible alternatives for a variation point, should be 1 smaller than the {@link #formulaStack}.
     */
    private Deque<Formula> alternativesStack = new ArrayDeque<>();
    
    /**
     * Default constructor, will assume {@link True#INSTANCE} as base line.
     */
    public VariationPointerCounter() {
        this(True.INSTANCE);
    }
    
    /**
     * Treats the passed {@link Formula} as base line, i.e., will count this presence conditions as <b>no</b>
     * variation point. This may be used if, if variation points shall be counted inside a function, which has already
     * a presence condition.
     * @param baseLine A {@link Formula}, which applies already for the very first element, without having a
     *     new variation point. For instance, this may be {@link True#INSTANCE}.
     */
    public VariationPointerCounter(Formula baseLine) {
        formulaStack.addFirst(baseLine);
        nVPs = 0;
    }
    
    /**
     * Counts the presence condition of a variation point. Based on the given formula, the counter determines
     * heuristically whether the variation point is a new variation point of belongs to an earlier variation point.
     * @param pc The presence condition of the current syntax element.
     */
    public void add(Formula pc) {
        // Check if the passed PC is not identical to the currently handled PC
        if (!formulaStack.peekFirst().equals(pc)) {
            // Check if the current PC is not the alternative to an earlier PC
            if (alternativesStack.isEmpty() || !alternativesStack.peekFirst().equals(pc)) {
                Formula predecessor = formulaStack.peekFirst();
                
                // Add the new variation point
                formulaStack.addFirst(pc);
                nVPs++;
                
                // Add the possible alternative for this variation point, i.e., the estimated else block
                // PC should be a conjunction of new part and old part, determine old part
                Formula newPart;
                Formula oldPart = null;
                if (pc instanceof Conjunction) {
                    Formula left = ((Conjunction) pc).getLeft();
                    Formula right = ((Conjunction) pc).getRight();
                    
                    if (left.equals(predecessor)) {
                        newPart = right;
                        oldPart = left;
                    } else if (right.equals(predecessor)) {
                        newPart = left;                        
                        oldPart = right;
                    } else {
                        // Fallback, hopefully never occur
                        newPart = pc;
                    }
                } else {
                    // Fallback, hopefully never occur
                    newPart = pc;
                }
                
                Formula negated;
                if (newPart instanceof Negation) {
                    negated = ((Negation) newPart).getFormula();
                } else {
                    negated = new Negation(newPart);
                }
                if (null != oldPart) {
                    negated = new Conjunction(oldPart, negated);
                }
                alternativesStack.addFirst(negated);
            }
        }
    }

    
    /**
     * Computes the number of variation points based on the {@link Formula}s passed to {@link #add(Formula)}.
     * @return The estimated number of variation points, will be at least 0 (if there was no variation point).
     */
    public int countVPs() {
        return nVPs;
    }
}
