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

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the number of Lines of Code. This visitor supports
 * the following metrics:
 * <ul>
 *     <li>Lines of Code: {@link #getDLoC()}</li>
 *     <li>Lines of Feature code: {@link #getLoF()}</li>
 *     <li>Percentage of Lines of Feature code: {@link #getPLoF()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class LoCVisitor extends StatementCountLoCVisitor {
    
    private int nLoC = 0;
    private int nLoF = 0;
       
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one known variable of the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public LoCVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
    }
    
    @Override
    public void visitFunction(@NonNull Function function) {       
        super.visitFunction(function);
        
        // Count also the function signature
        int nLines = function.getLineEnd() - function.getLineStart() + 1;
     
        // Check that it not negative (shouldn't be possible, but this is rather critical to the complete computation).
        if (nLines > 0) {
            nLoC += nLines;
        }
    }
    
        
    @Override
    public void visitCppBlock(@NonNull CppBlock cppBlock) {
        // Count the whole block including #if and #endif -> +1
        int nLines = cppBlock.getLineEnd() - cppBlock.getLineStart() + 1;
        
        // Check that it not negative (shouldn't be possible, but this is rather critical to the complete computation).
        if (nLines > 0) {
            nLoF += nLines;
        }
        
        // Do not visit nested elements!
    }

    @Override
    protected void count() { }
    
    @Override
    public int getDLoC() {
        return nLoC;
    }
    
    @Override
    public int getLoF() {
        return nLoF;
    }
    
    @Override
    public void reset() {
        super.reset();
        nLoC = 0;
        nLoF = 0;
    }
}
