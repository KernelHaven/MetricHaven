package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Does nothing. 
 * 
 * @author Adam
 */
public class FanInOutVisitor extends AbstractFunctionVisitor {
    
    /**
     * Creates a new {@link FanInOutVisitor}.
     * 
     * @param varModel The variability model (optional).
     */
    public FanInOutVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
    }
    
}
