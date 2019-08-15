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
package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.metric_haven.code_metrics.TanglingDegree.TDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Sums up all tangling values of all CPP blocks in a function.
 * @author El-Sharkawy
 *
 */
public class TanglingVisitor extends AbstractFunctionVisitor {

    private @Nullable Set<@NonNull String> varModelVars;
    private @NonNull IVariableWeight weight;
    private @Nullable File codeFile;

    private @NonNull TDType type;
    
    private long result = 0;
    
    /**
     * Creates a new tangling visitor to measure tangling values of all CPP-blocks in a function.
     * @param varModel  Optional, if not <tt>null</tt> for each variable used in a
     *     {@link net.ssehub.kernel_haven.code_model.ast.ISyntaxElement#getPresenceCondition()},
     *     whether it is defined in the variability model.
     * @param weight How to weight all identified features, maybe
     *     {@link net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight} if no weight should be used.
     * @param type Specifies whether to count also else blocks or not.
     */
    public TanglingVisitor(@Nullable VariabilityModel varModel, @NonNull IVariableWeight weight, @NonNull TDType type) {
        super(varModel);
        varModelVars = (null != varModel) ? Collections.unmodifiableSet(varModel.getVariableMap().keySet()) : null;
        this.weight = weight;
        this.type = type;
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        codeFile = function.getSourceFile();
        super.visitFunction(function);
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        Formula condition = type == TDType.TD_ALL ? block.getCondition() : block.getCurrentCondition();
        
        if (null != condition) {
            VariableFinder varFinder = new VariableFinder();
            varFinder.visit(condition);
            for (String symbolName : varFinder.getVariableNames()) {
                symbolName = notNull(symbolName);
                if (isVarModelVariable(symbolName)) {
                    result += weight.getWeight(symbolName, codeFile);
                }
            }
        }
        
        super.visitCppBlock(block);
    }
    
    /**
     * Returns whether the specified symbol name is known by the variability model.
     * @param symbolName The variable name to check.
     * @return <tt>true</tt> if the variable is known by the variability model or if no model was passed to the
     *     constructor, <tt>false</tt> otherwise.
     */
    private boolean isVarModelVariable(@NonNull String symbolName) {
        return (null != varModelVars) ? varModelVars.contains(symbolName) : true;
    }

    /**
     * Returns the summed tangling values for all CPP blocks of the measured code function.
     * @return The summed tangling (&ge; 0).
     */
    public long getResult() {
        return result;
    }
    
    @Override
    public void reset() {
        super.reset();
        result = 0;
    }
    
    /**
     * Ignore doubled code elements, since we need to analyze each code block only once.
     */
    @Override
    public void visitReference(@NonNull ReferenceElement referenceElement) { }
}
