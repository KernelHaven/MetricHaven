package net.ssehub.kernel_haven.metric_haven.filter_components;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A filter component that filters {@link CodeFunction}s by line numbers. A configuration option specifies a line
 * number, and only function where this line number is inside (i.e.
 * <code>func.getLineStart() <= lineNumber && func.getLineEnd() >= lineNumber</code>) are passed along.
 * 
 * @author Adam
 */
public class CodeFunctionByLineFilter extends AnalysisComponent<CodeFunction> {

    private @NonNull AnalysisComponent<CodeFunction> functionSource;
    
    /**
     * Represents a (file, line number) tuple.
     */
    private static class FileAndLine {
        
        private @NonNull File file;
        
        private int lineNumber;

        /**
         * Creates a new {@link FileAndLine} tuple.
         * 
         * @param file The file to store.
         * @param lineNumber The line number to store.
         */
        public FileAndLine(@NonNull File file, int lineNumber) {
            this.file = file;
            this.lineNumber = lineNumber;
        }
        
        /**
         * Checks whether the file and line number that this tuple represents is inside the given function. I.e., this
         * method checks whether the function lies within the file of this tuple, and the line number of this tuple
         * is inside the function block.
         * 
         * @param func The function to check.
         * 
         * @return Whether the given function matches this (file, line number) tuple.
         */
        public boolean matches(@NonNull Function func) {
            return func.getSourceFile().equals(file)
                    && func.getLineStart() <= lineNumber && func.getLineEnd() >= lineNumber;
        }
        
    }
    
    private @NonNull List<@NonNull FileAndLine> lines;
    
    /**
     * Creates this filter.
     * 
     * @param config The pipeline configuration.
     * @param functionSource The component to get the {@link CodeFunction}s to filter from.
     * 
     * @throws SetUpException If reading the line number setting fails.
     */
    public CodeFunctionByLineFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> functionSource) throws SetUpException {
        
        super(config);
        
        config.registerSetting(MetricSettings.LINE_NUMBER_SETTING);
        List<@NonNull String> list = config.getValue(MetricSettings.LINE_NUMBER_SETTING);
        
        if (null != list) {
            lines = new LinkedList<>();
            for (String element : list) {
                String[] parts = element.split(":");
                if (parts.length != 2) {
                    throw new SetUpException("Each element in " + MetricSettings.LINE_NUMBER_SETTING.getKey()
                            + " should have the format dir/file.c:51 (got " + element + ")");
                }
                
                try {
                    lines.add(new FileAndLine(new File(parts[0]), Integer.parseInt(parts[1])));
                } catch (NumberFormatException e) {
                    throw new SetUpException("Can't parse line number of " + element, e);
                }
            }
            
        } else {
            throw new SetUpException(MetricSettings.LINE_NUMBER_SETTING.getKey() + "missing, but required for "
                + this.getClass().getCanonicalName());
        }
        
        this.functionSource = functionSource;
    }

    @Override
    protected void execute() {
        CodeFunction function;
        while ((function = functionSource.getNextResult()) != null) {
            
            Function func = function.getFunction();
            for (FileAndLine line : lines) {
                if (line.matches(func)) {
                    addResult(function);
                    break;
                }
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions (filtered by line number)";
    }

}
