/*
 * Copyright 2017-2020 University of Hildesheim, Software Systems Engineering
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
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Visitor to measure for a given {@link Function} the number of Statements Count as derivation of LoC.
 * This visitor supports the following metrics:
 * <ul>
 *     <li>Statement Count of Code: {@link #getLoC()}</li>
 *     <li>Statement Count of Feature code: {@link #getLoF()}</li>
 *     <li>Percentage of Statement Count of Feature code: {@link #getPLoF()}</li>
 * </ul>
 * @author El-Sharkawy
 *
 */
public class StatementCountLoCVisitor extends AbstractLoCVisitor {
    
    private int nDLoC = 0;
    private int nLoF = 0;
    private int nLoCComments = 0;
    private int nLoFComments = 0;
    
    /**
     * Constructor to optionally specify a variability model. If a variability model is passed, only {@link CppBlock}s
     * will be treated as feature code ({@link #getLoF()} if their formula contains at least one known variable of the
     * variability model.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public StatementCountLoCVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
    }
    
    

    /**
     * Counts one line of code (only if currently a function is visited, does also consider c-preprocessor blocks).
     */
    protected void countStatement() {
        if (isInFunction()) {
            nDLoC++;
            
            if (isInConditionalCode()) {
                nLoF++;
            }
        }
    }
    
    @Override
    protected void countComment() {
        if (isInFunction()) {
            nLoCComments++;
            
            if (isInConditionalCode()) {
                nLoFComments++;
            }
        }
    }
    
    @Override
    public int getLoC() {
        return nDLoC;
    }
    
    @Override
    public int getLoF() {
        return nLoF;
    }
    
    @Override
    public double getLoCCommentRatio() {
        double basis = getLoC() + getLoCComments();
        return (basis != 0) ? getLoCComments() / basis : 0.0d;
    }
    
    @Override
    public double getLoFCommentRatio() {
        double basis = getLoF() + getLoFComments();
        return (basis != 0) ? getLoFComments() / basis : 0.0d;
    }
    
    @Override
    public void reset() {
        super.reset();
        nDLoC = 0;
        nLoF = 0;
        nLoCComments = 0;
        nLoFComments = 0;
    }



    @Override
    public int getLoCComments() {
        return nLoCComments;
    }



    @Override
    public int getLoFComments() {
        return nLoFComments;
    }
}
