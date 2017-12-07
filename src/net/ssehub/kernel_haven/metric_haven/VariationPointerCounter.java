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
    
    /**
     * Counts the presence conditions of counted variation points.
     */
    private int nVPs;
    
    // Used as stack: addFirst(), removeFirst(), peekFirst()
    private Deque<Formula> formulaStack = new ArrayDeque<>();
    
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
        if (null != pc && !pc.equals(formulaStack.peekFirst())) {
            if (pc instanceof Conjunction) {
                Formula predecessor = ((Conjunction) pc).getLeft();
                Formula negatedPre;
                if (pc instanceof Conjunction) {
                    Formula left = ((Conjunction) pc).getLeft();
                    Formula right = ((Conjunction) pc).getRight();
                    if (right instanceof Negation) {
                        right = ((Negation) right).getFormula();
                    } else {
                        right = new Negation(right);
                    }
                    negatedPre = new Conjunction(left, right);
                } else {
                    negatedPre = new Negation(predecessor);
                }
                
                insertPC(predecessor, negatedPre, pc);
            } else {
                // New top level element (is not necessary a conjunction)
                while (formulaStack.size() > 2) {
                    // Remove everything except for the baseline element and the first PC
                    formulaStack.removeFirst();
                }
                
                // First check if this is the alternative to the existing top PC (or the same)
                boolean found = false;
                if (formulaStack.size() == 2) {
                    Formula oldPC = formulaStack.peekFirst();
                    if (pc.equals(oldPC)) {
                        // The passed PC is already the top element of stack -> skip
                        found = true;
                    } else {
                        // Check if this alternative to the top element
                        // CHECKSTYLE:OFF    SE: Not nice, please refactor
                        if (oldPC instanceof Negation) {
                            oldPC = ((Negation) oldPC).getFormula();
                        } else {
                            oldPC = new Negation(oldPC);
                        }
                        if (pc.equals(oldPC)) {
                            // This is the alternative -> exchange elements
                            formulaStack.removeFirst();
                            formulaStack.addFirst(pc);
                            found = true;
                        }
                        // CHECKSTYLE:ON
                    }
                }
                
                if (!found) {
                    // New top level PC detected
                    if (formulaStack.size() == 2) {
                        // remove last toplevel element
                        formulaStack.removeFirst();
                    }
                    
                    formulaStack.addFirst(pc);
                    nVPs++;
                }
            }
        }
    }

    /**
     * Inserts the given presence condition to the correct position of the stack and increments {@link #nVPs} if
     * necessary.
     * @param pc The presence condition to insert, e.g., <tt>(A AND B) AND C</tt>
     * @param predecessor The predecessor of the presence condition, e.g., <tt>A AND B</tt>
     * @param negatedPre The alternative for the predecessor, e.g., <tt>A AND !B</tt>
     */
    private void insertPC(Formula predecessor, Formula negatedPre, Formula pc) {
        boolean elementFound = false;
        Formula topElement;
        do {
            topElement = formulaStack.peekFirst();
            if (topElement.equals(pc)) {
                elementFound = true;
            } else if (topElement.equals(predecessor)) {
                // New child for (existing) predecessor -> Add child (PC)
                formulaStack.addFirst(pc);
                nVPs++;
                elementFound = true;
            } else if (topElement.equals(negatedPre)) {
                // Alternative for existing predecessor -> exchange: predecessor with negatedPre
                formulaStack.removeFirst();
                formulaStack.addFirst(negatedPre);
                elementFound = true;
                // Do not increment as this is an alternative for the same block, i.e., an ELSE
            } else if (formulaStack.size() > 1) {
                // Continue search
                formulaStack.removeFirst();
            } else {
                // End reached, this is a new element on top level
                formulaStack.addFirst(pc);
                nVPs++;
                elementFound = true;
            }
        } while (!elementFound);
    }
    
    /**
     * Computes the number of variation points based on the {@link Formula}s passed to {@link #add(Formula)}.
     * @return The estimated number of variation points, will be at least 0 (if there was no variation point).
     */
    public int countVPs() {
        return nVPs;
    }
}
