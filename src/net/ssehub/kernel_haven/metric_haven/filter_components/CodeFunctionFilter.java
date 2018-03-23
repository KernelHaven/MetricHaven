package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance.
 *
 * @author Adam
 */
public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    private @NonNull AnalysisComponent<SourceFile> codeModelProvider;
    
    private SourceFile currentFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     */
    public CodeFunctionFilter(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile> codeModelProvider) {
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
        addResult(new CodeFunction(function.getName(), function, notNull(currentFile)));
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

}
