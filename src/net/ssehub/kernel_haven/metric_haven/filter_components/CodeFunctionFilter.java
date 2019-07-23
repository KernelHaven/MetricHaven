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

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance.
 *
 * @author Adam
 */
public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    public static final @NonNull Setting<@NonNull Boolean> SKIP_ERROR_FUNCTIONS_SETTING
            = new Setting<>("analysis.function_filter.skip_error_functions", Type.BOOLEAN, true, "false",
                    "If set to true, this setting causes the " + CodeFunctionFilter.class.getSimpleName()
                    + " to discard all functions that contain an ErrorElement.");
    
    private @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider;

    private boolean skipErrorFunctions;
    
    private int numSkipped;
    
    private SourceFile<ISyntaxElement> currentFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     * 
     * @throws SetUpException If reading the configuration fails.
     */
    public CodeFunctionFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider) throws SetUpException {
        super(config);
        
        config.registerSetting(SKIP_ERROR_FUNCTIONS_SETTING);
        this.skipErrorFunctions = config.getValue(SKIP_ERROR_FUNCTIONS_SETTING);
        
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
        
        if (numSkipped > 0) {
            LOGGER.logInfo("Discarded " + numSkipped + " functions that contained an ErrorElement");
        }
        
        progress.close();
    }

    @Override
    public void visitFunction(Function function) {
        if (skipErrorFunctions && function.containsErrorElement()) {
            numSkipped++;
        } else {
            addResult(new CodeFunction(function.getName(), function, notNull(currentFile)));
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

}
