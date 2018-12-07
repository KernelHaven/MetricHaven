package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSize;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureSizeType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * {@link IVariableWeight} that uses the {@link FeatureSize} (Lines of Code per Feature).
 * @author El-Sharkawy
 *
 */
public class FeatureSizeWeight implements IVariableWeight {

    private FeatureSizeType type;
    private FeatureSizeContainer featureSizes;
    
    /**
     * Sole constructor.
     * @param featureSizes The Lines of Code controlled by features.
     * @param type What kind of {@link FeatureSize} should be used as weight, {@link FeatureSizeType#NO_FEATURE_SIZES}
     *     is not supported by this weight.
     * @throws SetUpException If an unsupported combination of parameters is given.
     */
    public FeatureSizeWeight(@NonNull FeatureSizeContainer featureSizes, @NonNull FeatureSizeType type)
        throws SetUpException {
        
        this.featureSizes = featureSizes;
        this.type = type;
        
        if (type == FeatureSizeType.NO_FEATURE_SIZES) {
            throw new SetUpException(type.name() + " is not supported by " + getClass().getName()
                + " but was selected.");
        }
    }
    
    @Override
    public long getWeight(String variable) {
        long result = 0;
        
        switch (type) {
        case POSITIVE_SIZES:
            result = featureSizes.getPositiveSize(variable);
            break;
        case TOTAL_SIZES:
            result = featureSizes.getTotalSize(variable);
            break;
        case NO_FEATURE_SIZES:
            LOGGER.logWarning2(getClass().getName(), " used an unsupported weight type ", type.name());
            break;
        default:
            LOGGER.logWarning2(getClass().getName(), " used an unsupported weight type ", type.name());
            break;
        }
        
        return result;
    }

    @Override
    public String getName() {
        return type.name();
    }

}
