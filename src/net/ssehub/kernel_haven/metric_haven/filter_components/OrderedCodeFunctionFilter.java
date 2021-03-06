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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance and sorts them by file, name, line no.
 *
 * @author Sascha El-Sharkawy
 * @author Adam
 */
public class OrderedCodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    private @NonNull AnalysisComponent<CodeFunction> codeModelProvider;
    
    private SourceFile<ISyntaxElement> currentFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     * 
     * @throws SetUpException If reading the configuration fails.
     */
    public OrderedCodeFunctionFilter(@NonNull Configuration config,
        @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider) throws SetUpException {
        
        super(config);
        this.codeModelProvider = new CodeFunctionFilter(config, codeModelProvider);
    }

    @Override
    protected void execute() {
        List<CodeFunction> functions = new ArrayList<>(400000);
        CodeFunction func;
        while ((func = codeModelProvider.getNextResult()) != null) {
            functions.add(func);
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        Comparator<CodeFunction> funcComparator = new Comparator<CodeFunction>() {

            @Override
            public int compare(CodeFunction func1, CodeFunction func2) {
                int result = func1.getSourceFile().getPath().getAbsolutePath().compareTo(
                    func2.getSourceFile().getPath().getAbsolutePath());
                
                if (0 == result) {
                    result = func1.getName().compareTo(func2.getName());
                }
                
                if (0 == result) {
                    result = Integer.compare(func1.getFunction().getLineStart(), func2.getFunction().getLineStart());
                }
                
                progress.processedOne();
                
                return result;
            }
            
        };
        
        Collections.sort(functions, funcComparator);
        
        for (CodeFunction codeFunction : functions) {
            addResult(NullHelpers.notNull(codeFunction));
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
