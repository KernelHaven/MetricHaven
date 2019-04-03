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

import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.CppBlock.Type;
import net.ssehub.kernel_haven.metric_haven.code_metrics.BlocksPerFunctionMetric.BlockMeasureType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Determines the maximum and average nesting depth within a function.
 * @author El-Sharkawy
 *
 */
public class BlockCounter extends AbstractFunctionVisitor {
    // Variability dependent code
    private int nBlocks;
    private @NonNull BlockMeasureType measuredBlocks;
    
    /**
     * Sole constructor for this class.
     * @param measuredBlocks Specifies whether partial blocks (elif, else) blocks shall also be counted.
     * @param varModel Optional, if not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions.
     */
    public BlockCounter(@NonNull BlockMeasureType measuredBlocks, @Nullable VariabilityModel varModel) {
        super(varModel);
        this.measuredBlocks = measuredBlocks;
        reset();
    }
    
    @Override
    public void visitComment(@NonNull Comment comment) {
        // Do not visit comments!
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        // Compute only once (in this child class)
        boolean isVariationPoint = isFeatureDependentBlock(block); 
        if (isVariationPoint) {
            if (measuredBlocks == BlockMeasureType.BLOCK_AS_ONE) {
                if (block.getType() == Type.IF || block.getType() == Type.IFDEF || block.getType() == Type.IFNDEF) {
                    count();
                }
            } else {
                count();
            }
        }
        
        super.visitCppBlock(block);
    }
    
    @Override
    public void reset() {
        super.reset();
        nBlocks = 0;
    }
    
    /**
     * Counts statements.
     */
    private void count() {
        if (isInFunction()) {
            nBlocks++;
        }
    }
    
    /**
     * Returns the number of identified blocks.
     * 
     * @return The number of counted blocks (&ge; 0).
     */
    public double getNumberOfBlocks() {
        return nBlocks;
    }
}
