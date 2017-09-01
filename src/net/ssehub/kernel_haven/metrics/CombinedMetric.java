package net.ssehub.kernel_haven.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter for combining two metrics. Another metric is executed first, and its result is passed as a parameter
 * to this metric.
 * 
 * @author Adam
 */
public abstract class CombinedMetric extends AbstractMetric {

    private AbstractMetric otherMetric;
    
    /**
     * Creates a new combined metric.
     * 
     * @param config The global pipeline configuration.
     */
    public CombinedMetric(Configuration config) {
        super(config);
        otherMetric = createOtherMetric(config);
    }
    
    /**
     * Creates an instance of the other metric. The result of this metric is passed as a parameter to this one.
     * The default implementation reads a class name from the configuration and creates it via reflection.
     * 
     * @param config The global pipeline configuration. Can be passed to the constructor of the metric.
     * @return The other metric that runs before this one.
     */
    protected AbstractMetric createOtherMetric(Configuration config) {
        String className = config.getProperty("analysis.input_metric");
        
        if (className == null) {
            throw new NullPointerException("analysis.input_metric is null");
        }
        
        AbstractMetric otherMetric;
        
        try {
            @SuppressWarnings("unchecked")
            Class<AbstractMetric> otherMetricClass = (Class<AbstractMetric>) Class.forName(className);
            
            otherMetric = otherMetricClass.getConstructor(Configuration.class).newInstance(config);
            
        } catch (ReflectiveOperationException e) {
            // throw a runtime exception here; this will be caught by the infrastructure which
            // instantiates us via reflection
            throw new RuntimeException(e);
        }
        
        return otherMetric;
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
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
