package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;

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
    
    private Enum<?>[] selectedOptions;
    private Class<?> metricClass;
    
    /**
     * Sole constructor of this class, for creating an exception if an unsupported combination of options was selected.
     * @param metricClass The metric in which this exception occurred.
     * @param selectedOptions The selected options, which lead to this exception.
     */
    public UnsupportedMetricVariationException(Class<?> metricClass, Enum<?>... selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    @Override
    public String getMessage() {
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
        
        return errMsg.toString();
    }
    
    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
