package net.ssehub.kernel_haven.metric_haven.multi_results;

import java.util.Arrays;

import net.ssehub.kernel_haven.util.io.ITableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Data object storing multiple results for one measured item, e.g., one function.
 * 
 * @author El-Sharkawy
 */
public class MultiMetricResult implements ITableRow {
    
    private @Nullable String @NonNull [] header;
    private @Nullable Object @NonNull [] values;

    /**
     * Sole constructor.
     * 
     * @param header The header labels; first the measured item (file, included file, line number, element), afterwards
     *     the names of the measured metrics.
     * @param values The measured values (as {@link Double}s, if a value was not measured it should be <tt>null</tt>,
     *     should be as long as {@link #getHeader()} and start with the measured item
     *     (file, included file, line number, element).
     */
    public MultiMetricResult(@Nullable String @NonNull [] header, @Nullable Object @NonNull [] values) {
        this.header = header;
        this.values = values;
    }

    @Override
    public @Nullable Object @NonNull [] getContent() {
        return values;
    }

    @Override
    public @Nullable String @NonNull [] getHeader() {
        return header;
    }
    
    @Override
    public String toString() {
        return "MultiMetricResult: " + Arrays.toString(values);
    }
}
