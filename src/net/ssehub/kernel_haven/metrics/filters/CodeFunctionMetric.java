package net.ssehub.kernel_haven.metrics.filters;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.LiteralSyntaxElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metrics.AbstractMetric;
import net.ssehub.kernel_haven.metrics.MetricResult;
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
            LOGGER.logInfo("Running metric for functions in " + file.getPath().getPath());
            for (CodeElement b : file) {
                if (!(b instanceof SyntaxElement)) {
                    LOGGER.logError("This filter only works with SyntaxElements");
                }
                visitSyntaxElement((SyntaxElement) b, result);
            }
        }
        
        
        return result;
    }
    
    /**
     * Reads the name of a given function definition.
     * 
     * @param functionDef The function to read the name from.
     * 
     * @return The name of the function
     */
    private String getFunctionName(SyntaxElement functionDef) {
        String name = "<error: can't find ID in function>";
        
        SyntaxElement declarator = functionDef.getNestedElement("Declarator");
        if (declarator != null) {
            
            SyntaxElement id = declarator.getNestedElement("ID");
            if (id != null) {
                
                SyntaxElement value = id.getNestedElement("Name");
                if (value != null) {
                    name = ((LiteralSyntaxElement) value.getType()).getContent();
                    
                } else {
                    LOGGER.logWarning("Can't find Name in ID:\n" + declarator.toString());
                }
                
            } else {
                LOGGER.logWarning("Can't find ID in declarator:\n" + declarator.toString());
            }
            
        } else {
            LOGGER.logWarning("Can't find declarator in functionDef:\n" + functionDef.toString());
        }
        
        return name;
    }
    
    /**
     * Recursively walks through the AST to find functions. Calls the metric for each function found.
     * 
     * @param element The AST node we are currently at.
     * @param result The list to add results of metric executions to.
     */
    private void visitSyntaxElement(SyntaxElement element, List<MetricResult> result) {
        
        if (element.getType().equals(SyntaxElementTypes.FUNCTION_DEF)) {
            String name = getFunctionName(element);
            
            //LOGGER.logDebug("Running metric for " + name);
            
            double r = calc(element);
            
            String position = element.getSourceFile() + ":" + element.getLineStart();
            
            result.add(new MetricResult(position + " " + name + "()", r));
            
        } else {
            for (SyntaxElement b1 : element.iterateNestedSyntaxElements()) {
                visitSyntaxElement(b1, result);
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
    protected abstract double calc(SyntaxElement function);

}
