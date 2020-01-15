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

import net.ssehub.kernel_haven.code_model.AbstractCodeElement;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
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
public class LoCVisitor extends AbstractLoCVisitor {
    
    private int nLoC = 0;
    private int nLoF = 0;
    private int nLoCComments = 0;
    private int nLoFComments = 0;
    private boolean inBlock;
       
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
        int nLines = getLines(function);
     
        // Check that it not negative (shouldn't be possible, but this is rather critical to the complete computation).
        if (nLines > 0) {
            nLoC += nLines;
        }
    }
        
    @Override
    public void visitCppBlock(@NonNull CppBlock cppBlock) {
        // Don't count nested blocks to avoid counting lines twice
        if (!inBlock) {
            // Count the whole block including #if and #endif -> +1
            int nLines = getLines(cppBlock);
            
            /*
             *  Check that it not negative (shouldn't be possible,
             *  but this is rather critical to the complete computation).
             */
            if (nLines > 0) {
                nLoF += nLines;
            }
        }
        
        boolean oldValue = inBlock;
        inBlock = true;
        super.visitCppBlock(cppBlock);
        inBlock = oldValue;
    }
    
    /**
     * Computes the Lines of Code spanned by the element.
     * @param element The element to count
     * @return <tt>begin - end + 1</tt>
     */
    private int getLines(AbstractCodeElement<ISyntaxElement> element) {
        return element.getLineEnd() - element.getLineStart() + 1;
    }

    @Override
    protected void countStatement() { }
    
    @Override
    protected void countComment() { }
    
    @Override
    public int getLoC() {
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
        nLoCComments = 0;
        nLoFComments = 0;
        inBlock = false;
    }

    @Override
    public int getLoCComments() {
        return nLoCComments;
    }

    @Override
    public int getLoFComments() {
        return nLoFComments;
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        int nLines = getLines(comment);
        
        // Check that it not negative (shouldn't be possible, but this is rather critical to the complete computation).
        if (nLines > 0) {
            nLoCComments += nLines;
            
            if (isInConditionalCode()) {
                nLoFComments += nLines;
            }
        }
    }
}
