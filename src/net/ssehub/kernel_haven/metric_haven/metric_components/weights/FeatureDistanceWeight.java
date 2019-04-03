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
package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
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
     * Cache: Stores for a file the {@link Path} of the folder containing the file.
     */
    private Map<File, Path> folderCache;
    
    /**
     * Creates a new weight based on cross-tree constraint ratios.
     * @param varModel Must be a variability model, which has the ability to provide information about the location
     *     where a variable/feature was defined.
     *     
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public FeatureDistanceWeight(@NonNull VariabilityModel varModel) throws SetUpException {
        if (varModel.getDescriptor().hasAttribute(Attribute.SOURCE_LOCATIONS)) {
            varMap = varModel.getVariableMap();
        } else {
            throw new SetUpException("FeatureDistanceWeight without an approriate "
                + "variability model created.");
        }
        
        folderCache = new HashMap<>();
    }
    
    @Override
    public long getWeight(String variable) {
        throw new UnsupportedOperationException("FeatureDistanceWeight.getWeight(String variable) called, "
            + "but this method is not supported.");
    }

    @Override
    public long getWeight(String variable, File codeFile) {
        int result = -1;
        
        List<SourceLocation> srcLocations = varMap.get(variable).getSourceLocations();
        if (null != srcLocations && null != codeFile) {
            Path codefolder = determineFolder(codeFile);
            
            for (SourceLocation location : srcLocations) {
                Path srcFolder = determineFolder(location.getSource());
                Path delta = codefolder.relativize(srcFolder);
                
                // if both folders are identical, delta will be empty but getNameCount() returns 1
                int distance =  delta.toString().isEmpty() ? 0 : delta.getNameCount();
                if (distance < result || result == -1) {
                    result = distance;
                }
                
                if (distance <= 0) {
                    break;
                }
            }
        }
        
        // Return 0 if no source location is available, i.e., do not count undefined features.
        if (result == -1) {
            result = 0;
        }
        
        return result;
    }
    
    /**
     * Computes the absolute {@link Path} of a folder containing the specified file, uses a cache to minimize file
     * operations.
     * @param file A (code) file for which the {@link Path} of a folder should be retrieved.
     * @return The {@link Path} of a folder containing the specified file.
     */
    private @NonNull Path determineFolder(@NonNull File file) {
        Path folder = folderCache.get(file);
        if (null == folder) {
            folder = NullHelpers.notNull(file.getAbsoluteFile().getParentFile().toPath());
            folderCache.put(file, folder);
        }
        
        return folder;
    }
    
    @Override
    public String getName() {
        return "Feature distance";
    }
}
