package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance.
 *
 * @author Adam
 */
public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    private @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider;
    
    private SourceFile<ISyntaxElement> currentFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     */
    public CodeFunctionFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider) {
        super(config);
        this.codeModelProvider = codeModelProvider;
    }

    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        while ((file = codeModelProvider.getNextResult()) != null) {
            currentFile = file.castTo(ISyntaxElement.class);
            for (ISyntaxElement b : currentFile) {
                b.accept(this);
            }
            
            progress.processedOne();
        }
        
        progress.close();
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
