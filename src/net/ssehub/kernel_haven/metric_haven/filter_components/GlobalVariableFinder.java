package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
import net.ssehub.kernel_haven.util.ProgressLogger;
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
    
    // From {*}, but no inner }, e.g. int[] a = {1, 2, 3}, b = {4, 5, 6}; -> int[] a = , b = ;
    private Pattern arrayInitializer = Pattern.compile("\\{[^\\}]*\\}");
    
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
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
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
            
            progress.processedOne();
        }
        
        progress.close();
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
            extractVariables(declaration.getText());
        }
    }
    
    @Override
    public void visitSingleStatement(SingleStatement declaration) {
        if (declaration.getCode() instanceof Code) {
            // Approximation of name
            Code declarationCode = (Code) declaration.getCode();
            extractVariables(declarationCode.getText());
        }
    }
    
    /**
     * Heuristically extracts all variable declarations from the given code.
     * @param codeString An expression of a struct declaration or a single line statement.
     */
    private void extractVariables(String codeString) {
        // Remove array initializations
        codeString = arrayInitializer.matcher(codeString).replaceAll("");
        
        // Merge multi declarations and instantiations
        codeString = codeString.replace(" = ", "=").replace(" , ", ",");
        
        // Keep only declaration and initialization part
        String[] tmp = codeString.split(" ");
        if (tmp.length > 1) {
            codeString = tmp[tmp.length - 2];
            
            // Split per declaration
            String[] declarations = codeString.split(",");
            for (String declaration : declarations) {
                int endIndex = declaration.indexOf('=');
                if (endIndex != -1) {
                    declaration = declaration.substring(0, endIndex);
                }
                declaration = declaration.trim();
                
                // Check that we did not accidentally extracted a keyword, parenthesis or other language elements.
                if (!KEYWORDS.contains(declaration)) {
                    addResult(new GlobalVariable(currentFile, declaration));
                }
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

}
