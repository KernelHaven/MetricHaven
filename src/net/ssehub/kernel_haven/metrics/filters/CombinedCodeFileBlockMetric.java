package net.ssehub.kernel_haven.metrics.filters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.CombinedMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A combined metric that runs on source files. This first executes another metric, and passes this result to this
 * metric as a parameter.
 *   
 * @author Adam
 */
public abstract class CombinedCodeFileBlockMetric extends CombinedMetric {

    /**
     * Creates a new combined code file block metric.
     * 
     * @param config The global pipeline configuration.
     */
    public CombinedCodeFileBlockMetric(Configuration config) {
        super(config);
    }
    
    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel, Map<String, Double> otherMetricResult) {
        
        List<MetricResult> result = new LinkedList<>();
        
        SourceFile file;
        while ((file = codeModel.get()) != null) {
            if (file.iterator().hasNext() && !(file.iterator().next() instanceof CodeBlock)) {
                LOGGER.logError("This filter only works with CodeBlocks");
                // TODO: convert other extractor output (e.g. typechef) into conditional block hierarchy
                break;
            }
            
            LOGGER.logInfo("Running metric for " + file.getPath().getPath());
            
            double r = calc(file, otherMetricResult);
            result.add(new MetricResult(file.getPath().getPath(), r));
        }
        
        return result;
    }
    
    /**
     * Runs the metric on the given source code file. This method is called once for each source code file in the
     * product line.
     * 
     * @param file The source code file to run on.
     * @param otherMetricResult The result of the other metric.
     * @return The result for this source code file.
     */
    protected abstract double calc(SourceFile file, Map<String, Double> otherMetricResult);
    
}
