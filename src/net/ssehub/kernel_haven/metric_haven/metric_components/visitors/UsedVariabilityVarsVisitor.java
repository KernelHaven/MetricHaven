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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.cpp_utils.non_boolean.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Counts the number of  <tt>Number of internal/external configuration options</tt> as defined in
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class UsedVariabilityVarsVisitor extends AbstractFunctionVisitor {
    
    private @NonNull Set<@NonNull String> externalVars = new HashSet<>();
    private @NonNull Set<@NonNull String> internalVars = new HashSet<>();
    private @Nullable Set<@NonNull String> varModelVars;
    private @Nullable BuildModel bm;
    
    /**
     * Default constructor, won't consider a {@link BuildModel}.
     * @param varModel Optional, if not <tt>null</tt> for each variable used in a
     *     {@link net.ssehub.kernel_haven.code_model.ast.ISyntaxElement#getPresenceCondition()},
     *     whether it is defined in the variability model.
     */
    public UsedVariabilityVarsVisitor(@Nullable VariabilityModel varModel) {
        this(varModel, null);
    }
    
    /**
     * Default constructor, considering a {@link BuildModel} for external variables.
     * @param varModel Optional, if not <tt>null</tt> for each variable used in a
     *     {@link net.ssehub.kernel_haven.code_model.ast.ISyntaxElement#getPresenceCondition()},
     *     whether it is defined in the variability model.
     * @param bm The {@link BuildModel}, if not <tt>null</tt> variables of the file presence condition will be
     *     considered for computing the number of external variables.
     */
    public UsedVariabilityVarsVisitor(@Nullable VariabilityModel varModel, @Nullable BuildModel bm) {
        super(varModel);
        varModelVars = (null != varModel) ? Collections.unmodifiableSet(varModel.getVariableMap().keySet()) : null;
        this.bm = bm;
    }

    @Override
    public void visitFunction(@NonNull Function function) {
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(function.getPresenceCondition());
        
        // Compute external variables
        for (String symbolName : varFinder.getVariableNames()) {
            String canditate = NullHelpers.notNull(symbolName);
            if (isVarModelVariable(canditate)) {
                externalVars.add(canditate);
            }
        }
        
        BuildModel bm = this.bm;
        if (null != bm && bm.containsFile(function.getSourceFile())) {
            Formula filePC = bm.getPc(function.getSourceFile());
            if (null != filePC) {
                varFinder.clear();
                varFinder.visit(filePC);
                
                for (String symbolName : varFinder.getVariableNames()) {
                    String canditate = NullHelpers.notNull(symbolName);
                    if (isVarModelVariable(canditate)) {
                        externalVars.add(canditate);
                    }
                }
            }
        }
        
        // Compute internal variables
        super.visitFunction(function);
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        Formula condition = block.getCondition();
        
        if (null != condition) {
            VariableFinder varFinder = new VariableFinder();
            varFinder.visit(condition);
            for (String symbolName : varFinder.getVariableNames()) {
                symbolName = notNull(symbolName);
                if (isVarModelVariable(symbolName)) {
                    internalVars.add(symbolName);
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
     * Returns the variables used in the presence condition to include the complete function.
     * 
     * @return The variables used in the presence condition to include the complete function
     */
    public Set<String> externalVars() {
        return externalVars;
    }
    
    /**
     * Returns the variables used inside the function.
     * 
     * @return The variables used inside the function
     */
    public Set<String> internalVars() {
        return internalVars;
    }
    
    
    /**
     * Returns the union of the external and external variables.
     * 
     * @return External variables &cup; internal variables
     */
    public Set<String> allVars() {
        Set<String> superSet = new HashSet<>();
        superSet.addAll(internalVars);
        superSet.addAll(externalVars);
        
        return superSet;
    }

    @Override
    public void reset() {
        super.reset();
        internalVars.clear();
        externalVars.clear();
    }
    
    /**
     * Ignore doubled code elements, since we need to analyze each code block only once.
     */
    @Override
    public void visitReference(@NonNull ReferenceElement referenceElement) { }
}
