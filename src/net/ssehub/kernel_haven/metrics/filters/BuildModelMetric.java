package net.ssehub.kernel_haven.metrics.filters;

import java.util.ArrayList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter for a metric that runs on the complete build model.
 * 
 * @author Adam
 */
public abstract class BuildModelMetric extends AbstractMetric {

    /**
     * Creates a new instance.
     * 
     * @param config The global pipeline configuration.
     */
    public BuildModelMetric(Configuration config) {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new ArrayList<>(1);
        
        result.add(new MetricResult("BuildModel", calc(buildModel)));
        
        return result;
    }
    
    /**
     * Calculates the metric on the given build model.
     * 
     * @param buildModel The build model.
     * 
     * @return The result of the metric.
     */
    protected abstract double calc(BuildModel buildModel);

}
