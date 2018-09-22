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
