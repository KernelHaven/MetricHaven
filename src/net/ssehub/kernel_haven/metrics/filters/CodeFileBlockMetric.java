package net.ssehub.kernel_haven.metrics.filters;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.undertaker.UndertakerBlock;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter that aggregates the code model to a hierarchy of conditonal blocks. This filter calls the metric once for
 * each source file.
 * 
 * @author Adam
 */
public abstract class CodeFileBlockMetric extends AbstractMetric {

    /**
     * Creates a new code block metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public CodeFileBlockMetric(Configuration config) {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new LinkedList<>();
        
        SourceFile file;
        while ((file = codeModel.get()) != null) {
            if (file.iterator().hasNext() && !(file.iterator().next() instanceof UndertakerBlock)) {
                LOGGER.logError("This filter only works with undertaker extractor");
                // TODO: convert other extractor output (e.g. typechef) into conditional block hierarchy
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
     * The blocks in the source file represent a hierarchy of conditional compilation blocks.
     * 
     * @param file The file to run on.
     * @return The result of the metric.
     */
    protected abstract double calc(SourceFile file);

}
