package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
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
     * 
     * @param params The parameters for creating this metric.
     */
    @PreferedConstructor
    BlocksPerFunctionMetric(@NonNull MetricCreationParameters params) throws SetUpException {
        
        super(params);
        this.measuredBlocks = params.getMetricSpecificSettingValue(BlockMeasureType.class);
        
        // All weights are always supported
        
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
