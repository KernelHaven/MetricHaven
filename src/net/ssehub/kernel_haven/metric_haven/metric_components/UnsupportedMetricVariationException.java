package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;

/**
 * Will be thrown if for a metric an unsupported combination of options was selected, e.g.,  a variability weight for a
 * metric operating on non variable code.
 * @author El-Sharkawy
 *
 */
public class UnsupportedMetricVariationException extends SetUpException {

    /**
     * Generated ID.
     */
    private static final long serialVersionUID = -9003130308340433328L;

    private @NonNull String msg;
    
    /**
     * Constructor of this class, for creating an exception if an unsupported combination of options was selected if
     * setting values are available.
     * @param metricClass The metric in which this exception occurred.
     * @param selectedOptions The selected options, which lead to this exception.
     * @deprecated Use {@link #UnsupportedMetricVariationException(Class, IVariableWeight)} instead.
     */
    public UnsupportedMetricVariationException(Class<?> metricClass, Enum<?>... selectedOptions) {
        StringBuffer errMsg = new StringBuffer("Cannot apply the selected combination of options to the selected metric"
            + ". Setting was:\n - Metric: ");
        errMsg.append(metricClass.getName());
        
        // Diagnosis of selected options
        for (Enum<?> option : selectedOptions) {
            errMsg.append("\n - ");
            errMsg.append(option.getClass().getSimpleName());
            errMsg.append(": ");
            errMsg.append(option.name());                
        }
        
        msg = NullHelpers.notNull(errMsg.toString());
    }
    
    /**
     * Constructor of this class, for creating an exception if an unsupported combination of options was selected if
     * an unsupported weight was already created.
     * @param metricClass The metric in which this exception occurred.
     * @param weight A weight, which is not supported by the metric.
     */
    public UnsupportedMetricVariationException(Class<?> metricClass, IVariableWeight weight) {
        StringBuffer errMsg = new StringBuffer("Cannot apply the selected combination of options to the selected metric"
            + ". Setting was:\n - Metric with weight: ");
        errMsg.append(weight.getName());
        
        msg = NullHelpers.notNull(errMsg.toString());
    }

    @Override
    public String getMessage() {
        return msg;
    }
    
    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
