package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.LiteralSyntaxElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;

/**
 * Aggregates the code model to a function level. Passes along each function as an individual {@link SyntaxElement} to
 * the next component.
 * 
 * @author Adam
 * 
 * @deprecated This component filters functions in the old {@link SyntaxElement} style.
 */
@Deprecated
public class OldCodeFunctionFilter extends AnalysisComponent<OldCodeFunction> {

    private AnalysisComponent<SourceFile> codeModelProvider;
    
    /**
     * Creates this component.
     * 
     * @param config The global configuration.
     * @param codeModelProvider The component to get the code model from.
     */
    public OldCodeFunctionFilter(Configuration config, AnalysisComponent<SourceFile> codeModelProvider) {
        super(config);
        
        this.codeModelProvider = codeModelProvider;
    }

    @Override
    protected void execute() {
        SourceFile file;
        while ((file = codeModelProvider.getNextResult()) != null) {
            LOGGER.logInfo("Running metric for functions in " + file.getPath().getPath());
            for (CodeElement b : file) {
                if (!(b instanceof SyntaxElement)) {
                    LOGGER.logError("This filter only works with SyntaxElements");
                }
                visitSyntaxElement((SyntaxElement) b, file);
            }
        }
    }
    
    /**
     * Recursively walks through the AST to find functions. Calls {@link #addResult(SyntaxElement)} for each function
     * found.
     * 
     * @param element The AST node we are currently at.
     * @param file The C-file containing the function directly or indirectly via an included H-file.
     */
    private void visitSyntaxElement(SyntaxElement element, SourceFile file) {
        
        if (element.getType().equals(SyntaxElementTypes.FUNCTION_DEF)) {
            addResult(new OldCodeFunction(getFunctionName(element), element, file));
            
        } else {
            for (SyntaxElement b1 : element.iterateNestedSyntaxElements()) {
                visitSyntaxElement(b1, file);
            }
        }
        
    }
    
    /**
     * Reads the name of a given function definition.
     * 
     * @param functionDef The function to read the name from.
     * 
     * @return The name of the function. Never <code>null</code>.
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
            // try srcML format TODO: adapt if format changes
            if (functionDef.getNestedElementCount() >= 2
                    && functionDef.getNestedElement(1).getType() == SyntaxElementTypes.ID) {
                SyntaxElement id = functionDef.getNestedElement(1);
                if (id.getNestedElementCount() >= 1 
                        && id.getNestedElement(0).getType() instanceof LiteralSyntaxElement) {
                    LiteralSyntaxElement lit = (LiteralSyntaxElement) id.getNestedElement(0).getType();
                    name = lit.getContent();
                } else {
                    LOGGER.logWarning("Can't find literal in functionDef ID:\n" + functionDef.toString());
                }
                
            } else {
                LOGGER.logWarning("Can't find declarator in functionDef:\n" + functionDef.toString());
            }
        }
        
        return name;
    }

    @Override
    public String getResultName() {
        return "Code Functions";
    }

}
