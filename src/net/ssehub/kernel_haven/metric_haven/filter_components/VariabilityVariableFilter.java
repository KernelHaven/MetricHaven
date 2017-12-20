package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A filter component that passes along each variability variable individually.
 * 
 * @author Adam
 */
public class VariabilityVariableFilter extends AnalysisComponent<VariabilityVariable> {

    private AnalysisComponent<VariabilityModel> varModel;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param varModel The component to get the variability model from.
     */
    public VariabilityVariableFilter(Configuration config, AnalysisComponent<VariabilityModel> varModel) {
        super(config);
        this.varModel = varModel;
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = this.varModel.getNextResult();
        
        for (VariabilityVariable variable : varModel.getVariables()) {
            addResult(variable);
        }
    }

    @Override
    public String getResultName() {
        return "Variability Variables";
    }
    
}
