/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        StringBuilder errMsg = new StringBuilder("Cannot apply the selected combination of options to the selected"
                + " metric. Setting was:\n - Metric: ");
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
        StringBuilder errMsg = new StringBuilder("Cannot apply the selected combination of options to the selected "
                + "metric. Setting was:\n - Metric with weight: ");
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
