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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionCall;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionLocation;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Creates a {@link FunctionMap} for the given code model. Requires an unfiltered list of {@link CodeFunction}s (i.e.
 * a complete code model for the whole source tree).
 * 
 * @author Adam
 * @author El-Sharkawy
 */
public class FunctionMapCreator extends AnalysisComponent<FunctionMap> {

    private @NonNull AnalysisComponent<CodeFunction> cmProvider;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param cmProvider The component to get the code model, filtered for code functions, from. Probably one of
     *      {@link CodeFunctionFilter}, {@link CodeFunctionByLineFilter}, or {@link OrderedCodeFunctionFilter}
     */
    public FunctionMapCreator(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> cmProvider) {
        
        super(config);
        this.cmProvider = cmProvider;
    }
    
    @Override
    protected void execute() {
        List<@NonNull CodeFunction> allFunctions = new ArrayList<>();
        Map<String, List<FunctionLocation>> functionLocations = new HashMap<>();
        
        /*
         * Step one: Collect FunctionLocations for all functions
         * (also store allFunctions since we need to iterate over it again)
         */
        CodeFunction function;
        while ((function = cmProvider.getNextResult()) != null)  {
            allFunctions.add(function);
            
            String name = function.getName();
            functionLocations.putIfAbsent(function.getName(), new ArrayList<>());
            FunctionLocation funcImpl = determineFunctionLocation(function);
            functionLocations.get(name).add(funcImpl);
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()), allFunctions.size());
        
        /*
         * Step two: visit all functions to find function calls
         */
        FunctionMap result = new FunctionMap();
        
        for (@NonNull CodeFunction func : allFunctions) {
            FunctionLocation source = determineFunctionLocation(func);
            
            for (int i = 0; i < func.getFunction().getNestedElementCount(); i++) {
                func.getFunction().getNestedElement(i).accept(new ISyntaxElementVisitor() {
                    
                    @Override
                    public void visitCode(@NonNull Code code) {
                        
                        String[] fragments = code.getText().split(" ");
                        for (int i = 0; i < fragments.length - 1; i++) {
                            
                            if (fragments[i + 1].startsWith("(") && functionLocations.containsKey(fragments[i])) {
                                
                                List<FunctionLocation> locations = functionLocations.get(fragments[i]);
                                if (!locations.isEmpty()) {
//                                    // TODO SE: Misses too much, but alternative seems to be too computation intensive
//                                    result.addFunctionCall(new FunctionCall(source, notNull(locations.get(0))));
                                    
                                    // TODO SE: Check if correct
                                    for (FunctionLocation target : locations) {
                                        result.addFunctionCall(new FunctionCall(source, notNull(target),
                                            notNull(code.getCondition())));
                                    }
                                }
                            }
                            
                        }
                        
                        ISyntaxElementVisitor.super.visitCode(code);
                    }
                    
                });
            }
            
            progress.processedOne();
        }
        
        addResult(result);
        
        progress.close();
    }

    /**
     * Converts a {@link CodeFunction} into a {@link FunctionLocation} by extracting required information.
     * @param function The function to be converted.
     * @return The location information for the specified function.
     */
    private @NonNull FunctionLocation determineFunctionLocation(@NonNull CodeFunction function) {
        String name = function.getName();
        File funcLocation = function.getSourceFile().getPath();
        Formula pc = function.getFunction().getPresenceCondition();
        // Check if the function is a function stub / dummy function without implementation
        boolean isStub = function.getFunction().getNestedElementCount() == 0;
        if (!isStub) {
            // Usually there should be an (empty) compound statement, even for empty functions
            ISyntaxElement body = function.getFunction().getNestedElement(0);
            isStub = body.getNestedElementCount() == 0;
        }
        FunctionLocation funcImpl = new FunctionLocation(name, funcLocation, pc, isStub);
        
        return funcImpl;
    }

    @Override
    public @NonNull String getResultName() {
        return "FunctionMap";
    }

}
