package net.ssehub.kernel_haven.metric_haven;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;

/**
 * Represents the result of a metric execution.
 * 
 * @author Adam
 */
@TableRow
public class MetricResult {

    private String context;
    
    private double value;

    /**
     * Creates a new metric execution result.
     * 
     * @param context The context (i.e. the thing that the metric was execute on) of the result. Must not be
     *      <code>null</code>.
     * @param value The value that the metric returned.
     */
    public MetricResult(String context, double value) {
        this.context = context;
        this.value = value;
    }
    
    /**
     * Returns the context, that is the thing that the metric was executed on. For example, this could be the code
     * function name.
     * 
     * @return The context of this result. Never <code>null</code>.
     */
    @TableElement(name = "Context", index = 0)
    public String getContext() {
        return context;
    }
    
    /**
     * Returns the value that the metric calculated.
     * 
     * @return The value of this result.
     */
    @TableElement(name = "Value", index = 1)
    public double getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return context + ": " + value;
    }
    
}
