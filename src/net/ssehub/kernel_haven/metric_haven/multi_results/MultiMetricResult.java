package net.ssehub.kernel_haven.metric_haven.multi_results;

import net.ssehub.kernel_haven.util.io.ITableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Data object storing multiple results for one measured item, e.g., one function.
 * 
 * @author El-Sharkawy
 */
public class MultiMetricResult implements ITableRow {
    
    private @NonNull String[] header;
    private @NonNull Object[] values;

    /**
     * Sole constructor, should not be visible from outside of this package.
     * @param header The header labels; first the measured item (file, included file, line number, element), afterwards
     *     the names of the measured metrics.
     * @param values The measured values (as {@link Double}s, if a value was not measured it should be <tt>null</tt>,
     *     should be as long as {@link #getHeader()} and start with the measured item
     *     (file, included file, line number, element).
     */
    MultiMetricResult(@NonNull String[] header, @NonNull Object[] values) {
        this.header = header;
        this.values = values;
    }

    @Override
    public @NonNull Object[] getContent() {
        return values;
    }

    @Override
    public @NonNull String[] getHeader() {
        return header;
    }
    
}
