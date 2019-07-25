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

import net.ssehub.kernel_haven.code_model.ast.CodeList;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Detects <a href="https://www.infosun.fim.uni-passau.de/publications/docs/MRF+17.pdf">undisciplined preprocessor usage
 * </a> and sums up how often this appears in a function.
 * @author El-Sharkawy
 *
 */
public class UndisciplinedCPPUsageVisitor extends AbstractFunctionVisitor {

    private boolean insideStatement;
    private long result = 0;
    
    /**
     * Creates a new tangling visitor to measure tangling values of all CPP-blocks in a function.
     */
    public UndisciplinedCPPUsageVisitor() {
        super(null);
        reset();
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {
        super.visitFunction(function);
    }
    
    /**
     * Returns the number of &ldquo;undisciplined preprocessor usages&rdquo; of the measured code function.
     * @return The number of &ldquo;undisciplined preprocessor usages&rdquo; (&ge; 0).
     */
    public long getResult() {
        return result;
    }
    
    @Override
    public void reset() {
        super.reset();
        result = 0;
        insideStatement = false;
    }
    
    @Override
    public void visitCodeList(@NonNull CodeList code) {
        boolean oldState = insideStatement;
        insideStatement = true;
        
        for (int i = 0; i < code.getNestedElementCount(); i++) {
            code.getNestedElement(i).accept(this);
        }
        
        insideStatement = oldState;
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        if (insideStatement) {
            result++;
        }
        
        super.visitCppBlock(block);
    }
    
    @Override
    public void visitReference(@NonNull ReferenceElement referenceElement) {
    /*
     * Count the reference as it a "undisciplined preprocessor usage", but
     * ignore the doubled code elements, since we need to analyze each code block only once.
     * 
     */
        result++;
    }
}
