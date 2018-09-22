package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.BlockCounter;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Implements the <tt>Number of internal ifdefs</tt> metric from
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class BlocksPerFunctionMetric extends AbstractFunctionMetric<BlockCounter> {

    /**
     * Specification how to count CPP blocks.
     * @author El-Sharkawy
     *
     */
    public static enum BlockMeasureType {
        BLOCK_AS_ONE, SEPARATE_PARTIAL_BLOCKS;
    }
    
    private @NonNull BlockMeasureType measuredBlocks;
    
    /**
     * Creates a new internal block metric, which can also be created by the {@link MetricFactory}.
     * @param varModel Used for the weights, but also to filter for {@link CppBlock}s based on features
     *     from the variability model
     * @param buildModel May be <tt>null</tt> as it is not used by this metric.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * @param measuredBlocks Specifies how to treat <tt>else</tt> and <tt>elif</tt> parts, are these extra blocks or
     *     do they form together with the <tt>if</tt> one block?
     */
    @PreferedConstructor
    BlocksPerFunctionMetric(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight, @NonNull BlockMeasureType measuredBlocks) {
        
        super(varModel, buildModel, weight);
        this.measuredBlocks = measuredBlocks;
        // Always all weights are supported

        init();
    }

    @Override
    protected @NonNull BlockCounter createVisitor(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight) {
        
        return new BlockCounter(measuredBlocks, varModel);
    }

    @Override
    protected Number computeResult(@NonNull BlockCounter functionVisitor, CodeFunction func) {
        return functionVisitor.getNumberOfBlocks();
    }

    @Override
    public @NonNull String getMetricName() {
        StringBuffer resultName = new StringBuffer("No. int. blocks x ");
        resultName.append(measuredBlocks.name());
        
        return NullHelpers.notNull(resultName.toString());
    }

}
