package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Counts the shortest path to traversal from the code destination to the source location where the feature was defined
 * in the variability model.
 * @author El-Sharkawy
 *
 */
public class FeatureDistanceWeight implements IVariableWeight {

    private @NonNull Map<@NonNull String, VariabilityVariable> varMap;
    /**
     * Creates a new weight based on cross-tree constraint ratios.
     * @param varModel Must be a variability model, which has the ability to provide information about the location
     *     where a variable/feature was defined.
     */
    public FeatureDistanceWeight(@NonNull VariabilityModel varModel) {
        if (varModel.getDescriptor().hasAttribute(Attribute.SOURCE_LOCATIONS)) {
            varMap = varModel.getVariableMap();
        } else {
            throw new UnsupportedOperationException("FeatureDistanceWeight without an approriate "
                + "variability model created.");
        }
    }
    
    @Override
    public int getWeight(String variable) {
        throw new UnsupportedOperationException("FeatureDistanceWeight.getWeight(String variable) called, "
            + "but this method is not supported.");
    }

    @Override
    public int getWeight(String variable, File codeFile) {
        int result = -1;
        Path codefolder = codeFile.getAbsoluteFile().getParentFile().toPath();
        
        List<SourceLocation> srcLocations = varMap.get(variable).getSourceLocations();
        if (null != srcLocations) {
            for (SourceLocation location : srcLocations) {
                Path srcFolder = location.getSource().getAbsoluteFile().getParentFile().toPath();
                Path delta = codefolder.relativize(srcFolder);
                
                // if both folders are identical, delta will be empty but getNameCount() returns 1
                int distance = delta.toString().isEmpty() ? 0 : delta.getNameCount();
                if (distance < result || result == -1) {
                    result = distance;
                }
            }
        }
        
        // Return 0 if no source location is available, i.e., do not count undefined features.
        if (result == -1) {
            result = 0;
        }
        
        return result;
    }
}
