package net.ssehub.kernel_haven.metric_haven.filter_components;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    private AnalysisComponent<SourceFile> codeModelProvider;
    
    private SourceFile currentFile;
    
    public CodeFunctionFilter(@NonNull Configuration config, AnalysisComponent<SourceFile> codeModelProvider) {
        super(config);
        this.codeModelProvider = codeModelProvider;
    }

    @Override
    protected void execute() {
        SourceFile file;
        while ((file = codeModelProvider.getNextResult()) != null) {
            LOGGER.logInfo("Running metric for functions in " + file.getPath().getPath());
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
        addResult(new CodeFunction(function.getName(), function, currentFile));
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

    @Override
    public void visitCode(Code code) {
        // do nothing
    }

}
