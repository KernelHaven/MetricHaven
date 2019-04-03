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
package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.io.csv.CsvReader;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * An analysis component that reads the scattering degree for variables from a file. Produces the same kind of result
 * as {@link VariabilityCounter}.
 * 
 * @author Adam
 */
public class ScatteringDegreeReader extends AnalysisComponent<ScatteringDegreeContainer> {

    public static final @NonNull Setting<@Nullable File> SD_CACHE_FILE = new Setting<>("metrics.sd_cache.file",
            Type.PATH, false, null, "Defines the file that " + ScatteringDegreeReader.class.getName() + " and "
            + ScatteringDegreeWriter.class.getName() + " read and write the scattering degree cache to.");
    
    private @NonNull File cacheFile;
    
    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    
    /**
     * Creates this reader component.
     * 
     * @param config The pipeline configuration.
     * @param vmProvider The component to get the variability model from.
     * 
     * @throws SetUpException If the cache file setting is not valid.
     */
    public ScatteringDegreeReader(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariabilityModel> vmProvider) throws SetUpException {
        super(config);
        
        this.vmProvider = vmProvider;
        this.cacheFile = getSdCacheFileOrException(config);
    }

    @Override
    protected void execute() {
        VariabilityModel vm = vmProvider.getNextResult();
        
        if (vm != null) {

            try (CsvReader in = new CsvReader(new FileReader(cacheFile))) {
                
                // skip header row
                in.readNextRow();
                
                Set<ScatteringDegree> result = new HashSet<>();
                
                String[] row;
                while ((row = in.readNextRow()) != null) {
                    try {
                        VariabilityVariable var = vm.getVariableMap().get(row[0]);
                        if (var != null) {
                            result.add(new ScatteringDegree(var, Integer.parseInt(row[1]), Integer.parseInt(row[2])));
                            
                        } else {
                            LOGGER.logWarning("Couldn't find variable " + row[0] + " in VM");
                        }
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                        LOGGER.logException("Invalid row in line " + in.getLineNumber() + " in " + cacheFile, e);
                    }
                    
                }
                
                addResult(new ScatteringDegreeContainer(result));
                
            } catch (IOException e) {
                LOGGER.logException("Can't read ScatteringDegree cache file " + cacheFile, e);
            }
            
        } else {
            LOGGER.logError("Couldn't read ScatteringDegrees because no variability model is available");
        }
    }

    /**
     * Reads the {@link #SD_CACHE_FILE} setting from the given configuration. Throws a {@link SetUpException} if the
     * non-mandatory setting is not set. Registers the setting to the configuration
     * (see {@link Configuration#registerSetting(Setting)}).
     * 
     * @param config The configuration to read the setting from.
     *  
     * @return The value of the setting.
     * 
     * @throws SetUpException If the setting is not set or invalid.
     */
    static @NonNull File getSdCacheFileOrException(@NonNull Configuration config) throws SetUpException {
        config.registerSetting(SD_CACHE_FILE);
        File cacheFile = config.getValue(SD_CACHE_FILE);
        if (cacheFile == null) {
            throw new SetUpException("Component requires " + SD_CACHE_FILE.getKey() + " to be set");
        }
        
        return cacheFile;
    }
    
    @Override
    public @NonNull String getResultName() {
        return "Counted Variability Variables (read from file)";
    }

}
