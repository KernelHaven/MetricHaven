/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.filter_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;

/**
 * A filter component that filters {@link CodeFunction}s by file path and function names.
 * 
 * @author Sascha El-Sharkawy
 * @author Adam
 */
public class CodeFunctionByPathAndNameFilter extends AnalysisComponent<CodeFunction> {

    private @NonNull AnalysisComponent<CodeFunction> functionSource;
    
    /**
     * Represents a (file, function name) tuple.
     */
    private static class FileAndFuncName {
        
        private @NonNull File file;
        
        private @NonNull String funcName;

        /**
         * Creates a new {@link FileAndFuncName} tuple.
         * 
         * @param file The file to store.
         * @param funcName The name of the function to store.
         */
        public FileAndFuncName(@NonNull File file, @NonNull String funcName) {
            this.file = file;
            this.funcName = funcName;
        }
        
        /**
         * Checks whether a given function is specified by stored the file and function name.
         * 
         * @param func The function to check.
         * 
         * @return Whether the given function matches this (file, function name) tuple.
         */
        public boolean matches(@NonNull Function func) {
            return func.getSourceFile().equals(file) && func.getName().equals(funcName);
        }
        
    }
    
    private @NonNull List<@NonNull FileAndFuncName> lines;
    
    /**
     * Creates this filter.
     * 
     * @param config The pipeline configuration.
     * @param functionSource The component to get the {@link CodeFunction}s to filter from.
     * 
     * @throws SetUpException If reading the line number setting fails.
     */
    public CodeFunctionByPathAndNameFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> functionSource) throws SetUpException {
        
        super(config);
        
        config.registerSetting(MetricSettings.FILTER_BY_FUNCTIONS);
        List<@NonNull String> list = config.getValue(MetricSettings.FILTER_BY_FUNCTIONS);
        
        if (!list.isEmpty()) {
            lines = new LinkedList<>();
            for (String element : list) {
                String[] parts = element.split(":");
                if (parts.length != 2) {
                    throw new SetUpException("Each element in " + MetricSettings.FILTER_BY_FUNCTIONS.getKey()
                            + " should have the format dir/file.c:function_name (got " + element + ")");
                }
                lines.add(new FileAndFuncName(new File(parts[0]), NullHelpers.notNull(parts[1])));
            }
            
        } else {
            throw new SetUpException(MetricSettings.FILTER_BY_FUNCTIONS.getKey() + " missing, but required for "
                + this.getClass().getCanonicalName());
        }
        
        this.functionSource = functionSource;
    }

    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        CodeFunction function;
        while ((function = functionSource.getNextResult()) != null) {
            
            Function func = function.getFunction();
            for (FileAndFuncName line : lines) {
                if (line.matches(func)) {
                    addResult(function);
                    break;
                }
            }
            
            progress.processedOne();
        }
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions (filtered by function name)";
    }

}
