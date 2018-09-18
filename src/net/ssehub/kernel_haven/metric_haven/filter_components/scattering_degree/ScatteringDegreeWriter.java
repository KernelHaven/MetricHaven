package net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.io.csv.CsvWriter;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An analysis component that writes the scattering degree for variables to a file. Simply passes through the results
 * while writing also to the file.
 * 
 * @author Adam
 */
public class ScatteringDegreeWriter extends AnalysisComponent<ScatteringDegreeContainer> {

    private @NonNull File cacheFile;
    
    private @NonNull AnalysisComponent<ScatteringDegreeContainer> sdProvider;

    /**
     * Creates this reader component.
     * 
     * @param config The pipeline configuration.
     * @param sdProvider The component to get the scattering degree from.
     * 
     * @throws SetUpException If the cache file setting is not valid.
     */
    public ScatteringDegreeWriter(@NonNull Configuration config,
            @NonNull AnalysisComponent<ScatteringDegreeContainer> sdProvider) throws SetUpException {
        super(config);
        
        this.sdProvider = sdProvider;
        this.cacheFile = ScatteringDegreeReader.getSdCacheFileOrException(config);
    }

    @Override
    protected void execute() {
        ScatteringDegreeContainer container = sdProvider.getNextResult();
        
        if (container != null) {
            
            try (CsvWriter out = new CsvWriter(new FileWriter(cacheFile))) {
                
                for (Iterator<ScatteringDegree> it = container.getScatteringDegreeIterator(); it.hasNext();) {
                    ScatteringDegree sd = notNull(it.next());
                    
                    out.writeObject(sd);
                }
                
            } catch (IOException e) {
                LOGGER.logException("Can't write ScatteringDegree cache file " + cacheFile, e);
            }
            
            
            addResult(container);
            
        } else {
            LOGGER.logWarning("Got no ScatteringDegree result");
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Counted Variability Variables (writing to file)";
    }

}
