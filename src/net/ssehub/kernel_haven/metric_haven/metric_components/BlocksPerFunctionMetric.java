package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.code_metrics.BlocksPerFunctionMetric.BlockMeasureType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.BlockCounter;
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
public class BlocksPerFunctionMetric extends AbstractFunctionVisitorBasedMetric<BlockCounter> {

    public static final @NonNull Setting<@NonNull BlockMeasureType> BLOCK_TYPE_SETTING
        = new EnumSetting<>("metric.blocks_per_function.measured_block_type", BlockMeasureType.class, true, 
                BlockMeasureType.BLOCK_AS_ONE, "Defines whether partial blocks (#elif/#else) are also counted.");
    
    
    private @NonNull BlockMeasureType measuredBlocks;
    
    /**
     * Constructor with minimal parameters.
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException If an unsupported combination of options is selected inside the <tt>config</tt>
     */
    public BlocksPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null, null, null);
    }
    
    /**
     * Constructor with if a variability model is present, to filter for CPP-blocks, using at least one feature.
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     CPP-block is a feature-dependent block.
     * @throws SetUpException If an unsupported combination of options is selected inside the <tt>config</tt>
     */
    public BlocksPerFunctionMetric(@NonNull Configuration config,
            @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
            @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, null);
    }
    
    /**
     * Default constructor.
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     CPP-block is a feature-dependent block.
     * @param bmComponent Not used, required to be called from the AllFunctionMetric.
     * @param sdComponent Not used, required to be called from the AllFunctionMetric.
     * @throws SetUpException If an unsupported combination of options is selected inside the <tt>config</tt>
     */
    public BlocksPerFunctionMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);
        config.registerSetting(BLOCK_TYPE_SETTING);
        measuredBlocks = config.getValue(BLOCK_TYPE_SETTING);
    }

    @Override
    protected @NonNull BlockCounter createVisitor(@Nullable VariabilityModel varModel) {
        return new BlockCounter(measuredBlocks, varModel);
    }

    @Override
    protected double computeResult(@NonNull BlockCounter visitor) {
        return visitor.getNumberOfBlocks();
    }

    @Override
    public @NonNull String getResultName() {
        StringBuffer resultName = new StringBuffer("No. of int. ifdefs x ");
        resultName.append(measuredBlocks.name());
        resultName.append(getWeightsName());
        
        return NullHelpers.notNull(resultName.toString());
    }

}
