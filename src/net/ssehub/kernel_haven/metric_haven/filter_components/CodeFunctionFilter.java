package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.io.File;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.LiteralSyntaxElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.SyntaxElement;
import net.ssehub.kernel_haven.code_model.SyntaxElementTypes;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter.CodeFunction;

/**
 * Aggregates the code model to a function level. Passes along each function as an individual {@link SyntaxElement} to
 * the next component.
 * 
 * @author Adam
 */
public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> {

    /**
     * Holds a function from the build model.
     *  
     * @author Adam
     */
    public static class CodeFunction {
        
        private String name;
        
        private SyntaxElement function;
  
        private SourceFile file;
        
        private String id;

        /**
         * Creates a {@link CodeFunction}.
         * 
         * @param name The name of the function.
         * @param function The function that should be held by this object.
         * @param file The C-file containing the function directly or indirectly via an included H-file.
         */
        public CodeFunction(String name, SyntaxElement function, SourceFile file) {
            this.name = name;
            this.function = function;
            this.file = file;
            
            File functionSource = function.getSourceFile();
            File fileSource = file.getPath();
            if (!functionSource.equals(fileSource)) {
                id = fileSource.getPath() + ":" + functionSource.getPath();
            } else {
                id = fileSource.getPath();
            }
        }

        /**
         * Retrieves the name of this function.
         * 
         * @return The name of this function. Never <code>null</code>.
         */
        public String getName() {
            return name;
        }

        /**
         * Retrieves the function held in this object.
         * 
         * @return The function. Never <code>null</code>.
         */
        public SyntaxElement getFunction() {
            return function;
        }
        
        /**
         * Returns the (main) source file containing the function. <br/>
         * <b>Please note:</b> This is not necessary identical to {@link SyntaxElement#getSourceFile()} as this
         * points to the direct location of an (included) source file.
         * @return The location of the C-file (not of an included H-file of a macro).
         */
        public SourceFile getSourceFile() {
            return file;
        }
        
        /**
         * Returns the full qualified name of the function, which considers the path of the C-file and included
         * H-files if existent.
         * @return The full qualified name of the function.
         */
        public String getQualifiedName() {
            return id;
        }
    }
    
    private AnalysisComponent<SourceFile> codeModelProvider;
    
    /**
     * Creates this component.
     * 
     * @param config The global configuration.
     * @param codeModelProvider The component to get the code model from.
     */
    public CodeFunctionFilter(Configuration config, AnalysisComponent<SourceFile> codeModelProvider) {
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
            addResult(new CodeFunction(getFunctionName(element), element, file));
            
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
