package net.ssehub.kernel_haven.metrics.filters;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter that aggregates the code model to file ASTs. This filter calls the metric once for
 * each source file.
 * 
 * @author Adam
 */
public abstract class CodeFileAstMetric extends AbstractMetric {

    /**
     * Creates a new code AST metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public CodeFileAstMetric(Configuration config) {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new LinkedList<>();
        
        SourceFile file;
        while ((file = codeModel.get()) != null) {
            if (file.iterator().hasNext() && !(file.iterator().next() instanceof SyntaxElement)) {
                LOGGER.logError("This filter only works with the SyntaxElements");
                // TODO: detect this a different way?
                break;
            }
            
            LOGGER.logInfo("Running metric for " + file.getPath().getPath());
            
            double r = calc(file);
            result.add(new MetricResult(file.getPath().getPath(), r));
        }
        
        return result;
    }
    
    /**
     * Calculates the metric for this source file. Called once for each source file in the product line.<br />
     * The complete source file is represented as an AST.
     * 
     * @param file The file to run on.
     * @return The result of the metric.
     */
    protected abstract double calc(SourceFile file);

}
