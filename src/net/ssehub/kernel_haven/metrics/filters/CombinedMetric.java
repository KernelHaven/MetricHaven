package net.ssehub.kernel_haven.metrics.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter for combining two metrics. Another metric is executed first, and its result is passed as a parameter
 * to this metric.
 * 
 * @author Adam
 */
public abstract class CombinedMetric extends AbstractMetric {

    /**
     * Creates a new combined metric.
     * 
     * @param config The global pipeline configuration.
     */
    public CombinedMetric(Configuration config) {
        super(config);
    }
    
    /**
     * Creates an instance of the other metric. The result of this metric is passed as a parameter to this one.
     * 
     * @param config The global piepline configuration. Can be passed to the constructor of the metric.
     * @return The other metric that runs before this one.
     */
    protected abstract AbstractMetric createOtherMetric(Configuration config);

    @Override
    public List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        AbstractMetric otherMetric = createOtherMetric(config);
        
        List<MetricResult> resultList = otherMetric.run(codeModel, buildModel, varModel);
        
        Map<String, Double> resultMap = new HashMap<>((int) (resultList.size() * 1.4));
        
        for (MetricResult r : resultList) {
            resultMap.put(r.getContext(), r.getValue());
        }
        
        return run(codeModel, buildModel, varModel, resultMap);
    }
    
    /**
     * Executes the metric.
     * 
     * @param codeModel The code model of the product line.
     * @param buildModel The build model of the product line.
     * @param varModel The variability model of the product line.
     * @param otherMetricResult The result of the other metric.
     * @return The result of the metric.
     */
    protected abstract List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel, Map<String, Double> otherMetricResult);

}
