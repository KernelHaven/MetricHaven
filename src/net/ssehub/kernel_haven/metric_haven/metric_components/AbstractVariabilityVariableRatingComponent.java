package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A component that assigns ratings to variability variables.
 * 
 * @author Adam
 */
public abstract class AbstractVariabilityVariableRatingComponent extends AnalysisComponent<MetricResult> {

    private AnalysisComponent<VariabilityVariable> variableSource;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param variableSource The component to get the {@link VariabilityVariable}s from.
     */
    public AbstractVariabilityVariableRatingComponent(Configuration config,
            AnalysisComponent<VariabilityVariable> variableSource) {
        super(config);
        this.variableSource = variableSource;
    }

    /**
     * Calculates the rating for this variable.
     * 
     * @param variable The variable to rate.
     * @return The rating of the variable.
     */
    protected abstract double getRating(VariabilityVariable variable);

    @Override
    protected void execute() {
        VariabilityVariable variable;
        while ((variable = variableSource.getNextResult()) != null) {
            addResult(new MetricResult(null, null, -1, variable.getName(), getRating(variable)));
        }
    }

    @Override
    public String getResultName() {
        return "Rated Variability Variables";
    }
    
}
