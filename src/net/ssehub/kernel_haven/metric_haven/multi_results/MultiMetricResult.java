package net.ssehub.kernel_haven.metric_haven.multi_results;

import net.ssehub.kernel_haven.util.io.ITableRow;

/**
 * Data object storing multiple results for one measured item, e.g., one function.
 * 
 * @author El-Sharkawy
 */
public class MultiMetricResult implements ITableRow {
    
    private String[] header;
    private Object[] values;

    /**
     * Sole constructor, should not be visible from outside of this package.
     * @param header The header labels; first the measured item (file, included file, line number, element), afterwards
     *     the names of the measured metrics.
     * @param values The measured values (as {@link Double}s, if a value was not measured it should be <tt>null</tt>,
     *     should be as long as {@link #getHeader()} and start with the measured item
     *     (file, included file, line number, element).
     */
    MultiMetricResult(String[] header, Object[] values) {
        this.header = header;
        this.values = values;
    }

    @Override
    public String[] getContent() {
        String[] result = new String[values.length];
        
        for (int i = 0; i < values.length; i++) {
            // values are either a String or a number -> toString returns something useful
            result[i] = (null != values[i]) ? values[i].toString() : "";
        }
        
        return result;
    }

    @Override
    public String[] getHeader() {
        return header;
    }
}
