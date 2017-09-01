package net.ssehub.kernel_haven.metrics.filters;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A filter that executes the metrics once for each variability variable.
 * 
 * @author Adam
 */
public abstract class VariabilityVariableMetric extends AbstractMetric {

    /**
     * Creates a new variability variable metric.
     * 
     * @param config The global pipeline configuration.
     */
    public VariabilityVariableMetric(Configuration config) {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new LinkedList<>();
        
        for (VariabilityVariable variable : varModel.getVariables()) {
            double r = run(variable, varModel);
            result.add(new MetricResult(variable.getName(), r));
        }
        
        return result;
    }
    
    /**
     * Calculates the metric for the given variability variable. This is called once for each variability variable
     * in the variability model.
     * 
     * @param variable The variability variable to run the metric on.
     * @param model The variability model.
     * @return The result from the metric.
     */
    protected abstract double run(VariabilityVariable variable, VariabilityModel model);

}
