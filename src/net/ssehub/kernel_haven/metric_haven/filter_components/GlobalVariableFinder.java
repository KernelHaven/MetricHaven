package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition.TypeDefType;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.filter_components.GlobalVariableFinder.GlobalVariable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance.
 *
 * @author El-Sharkawy
 */
public class GlobalVariableFinder extends AnalysisComponent<GlobalVariable> implements ISyntaxElementVisitor {

    /**
     * Datatype representing a global accessible variable.
     * @author El-Sharkawy
     *
     */
    public static class GlobalVariable {
        private SourceFile declaringFile;
        private String name;
        
        /**
         * Sole constructor.
         * @param declaringFile The file which declares the global variable.
         * @param name The name of the variable.
         */
        private GlobalVariable(SourceFile declaringFile, String name) {
            this.declaringFile = declaringFile;
            this.name = name;
        }
        
        /**
         * Returns the name of the variable.
         * @return The name of the variable.
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns the declaring file of the variable.
         * @return The declaring file of the variable.
         */
        public SourceFile getFile() {
            return declaringFile;
        }
    }

    private static final Set<String> KEYWORDS = new HashSet<>();
    
    static {
        KEYWORDS.add("static");
        KEYWORDS.add("extern");
        KEYWORDS.add("typedef");
        KEYWORDS.add("union");
        KEYWORDS.add(";");
        KEYWORDS.add("}");
        KEYWORDS.add("{");
    }
    
    private @NonNull AnalysisComponent<SourceFile> codeModelProvider;
    
    private SourceFile currentFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     */
    public GlobalVariableFinder(@NonNull Configuration config,
        @NonNull AnalysisComponent<SourceFile> codeModelProvider) {
        
        super(config);
        this.codeModelProvider = codeModelProvider;
    }

    @Override
    protected void execute() {
        SourceFile file;
        while ((file = codeModelProvider.getNextResult()) != null) {
            LOGGER.logInfo2("Running metric for functions in ", file.getPath().getPath());
            currentFile = file;
            for (CodeElement b : file) {
                if (!(b instanceof ISyntaxElement)) {
                    LOGGER.logError("This filter only works with ISyntaxElement");
                } else {
                    ((ISyntaxElement) b).accept(this);
                }
            }
        }
    }

    @Override
    public void visitFunction(Function function) {
        // Skip content of all functions.
    }
    
    @Override
    public void visitTypeDefinition(TypeDefinition typeDef) {
        if (typeDef.getType() == TypeDefType.STRUCT && typeDef.getDeclaration() instanceof Code) {
            // Approximation of name
            Code declaration = (Code) typeDef.getDeclaration();
            String[] code = declaration.getText().split(" ");
            if (code.length > 2) {
                String name = code[code.length - 2];
                if (!KEYWORDS.contains(name)) {
                    addResult(new GlobalVariable(currentFile, name));
                }
            }
        }
    }
    
    @Override
    public void visitSingleStatement(SingleStatement declaration) {
        if (declaration.getCode() instanceof Code) {
            // Approximation of name
            Code declarationCode = (Code) declaration.getCode();
            String codeAsText = declarationCode.getText().replace(" , ", ",");
            String[] code = codeAsText.split(" ");
            
            int offSet = 2;
            if (codeAsText.contains("=")) {
                // declaration and assignment
                offSet = -1;
                for (int i = code.length - 1; i >= 0 && offSet == -1; i--) {
                    if ("=".equals(code[i])) {
                        offSet = code.length - i - 2;
                    }
                }
            }
            String relevantPart = null;
            if (code.length > offSet && offSet >= 0) {
                relevantPart = code[code.length - offSet];
            }
            
            if (null != relevantPart) {
                // Maybe there are multiple declarations in one line separated by a comma
                String[] variableNames = relevantPart.split(",");
                
                for (String varName : variableNames) {
                    if (!KEYWORDS.contains(varName)) {
                        addResult(new GlobalVariable(currentFile, varName));
                    }
                }
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

}
