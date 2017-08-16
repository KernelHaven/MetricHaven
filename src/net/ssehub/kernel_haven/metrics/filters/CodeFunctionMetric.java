package net.ssehub.kernel_haven.metrics.filters;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.Block;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
import net.ssehub.kernel_haven.typechef.ast.TypeChefBlock;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A filter for the extractor data that aggregates the code model to a function level. The metric is called once
 * for each function found in the source code model. This filter only works with the TypeChef extractor.
 * 
 * @author Adam
 */
public abstract class CodeFunctionMetric extends AbstractMetric {

    /**
     * Creates a new code function metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public CodeFunctionMetric(Configuration config) {
        super(config);
    }

    @Override
    protected List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel) {
        
        List<MetricResult> result = new LinkedList<>();

        SourceFile file;
        while ((file = codeModel.get()) != null) {
            String filename = file.getPath().getPath();
            for (Block b : file) {
                visitCodeBlock(b, result, filename);
            }
        }
        
        
        return result;
    }
    
    /**
     * Recursively walks through the AST to find functions. Calls the metric for each function found.
     * 
     * @param block The AST node we are currently at.
     * @param result The list to add results of metric executions to.
     * @param filename The name of the file that we are currently in.
     */
    private void visitCodeBlock(Block block, List<MetricResult> result, String filename) {
        TypeChefBlock b = (TypeChefBlock) block;
        
        if (b.getText().equals("FunctionDef")) {
            String name = b.getChild("Declarator").getChild("ID").getChild("Name").getText();
            
            LOGGER.logInfo("Running metric for " + name);
            
            double r = run(b);
            result.add(new MetricResult(filename + ":" + b.getLineStart() + " " + name + "()", r));
            
        } else {
            for (Block b1 : b) {
                visitCodeBlock(b1, result, filename);
            }
        }
        
    }
    
    /**
     * Calculates the metric for the given code function. This is called once for each code function found in the
     * extractor data.
     * 
     * @param function The code function to calculate the metric for. Never <code>null</code>.
     * @return The result of the metric execution.
     */
    protected abstract double run(TypeChefBlock function);

}
