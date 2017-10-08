package net.ssehub.kernel_haven.metrics.filters;

import java.util.ArrayList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter that executes the metric on the variability model.
 * 
 * @author Adam
 */
public abstract class VariabilityModelMetric extends AbstractMetric {

    /**
     * Creates a new instance of this metric.
     * 
     * @param config The global pipeline configuration.
     * 
     * @throws SetUpException If creating this metric fails. 
     */
    public VariabilityModelMetric(Configuration config) throws SetUpException {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new ArrayList<>(1);
        
        result.add(new MetricResult("VariabilityModel", calc(varModel)));
        
        return result;
    }

    /**
     * Calculates the metric for the given variability model.
     * 
     * @param varModel The variability model.
     * @return The result of th metric.
     */
    protected abstract double calc(VariabilityModel varModel);

}
