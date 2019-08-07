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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Counts in how many &#35;ifdefs and files a {@link VariabilityVariable} is used in.
 * 
 * @author Adam
 * @author El-Sharkawy
 */
public class VariabilityCounter extends AnalysisComponent<ScatteringDegreeContainer> {

    private @NonNull AnalysisComponent<VariabilityModel> vmProvider;
    
    private @NonNull AnalysisComponent<SourceFile<?>> cmProvider;
    
    private @NonNull CountedVariables countedVariables;
    
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param vmProvider The component to get the variability model from.
     * @param cmProvider The component to get the code model from.
     */
    public VariabilityCounter(@NonNull Configuration config, @NonNull AnalysisComponent<VariabilityModel> vmProvider,
            @NonNull AnalysisComponent<SourceFile<?>> cmProvider) {
        super(config);
        
        this.vmProvider = vmProvider;
        this.cmProvider = cmProvider;
        this.countedVariables = new CountedVariables();
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = vmProvider.getNextResult();
        if (varModel == null) {
            LOGGER.logError("Did not get a variability model", "Can't create any results");
            return;
        }
        
        countedVariables.init(varModel);
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        ScatteringVisitor visitor = new ScatteringVisitor(countedVariables);
        while ((file = cmProvider.getNextResult()) != null) {
            
            for (ISyntaxElement element : file.castTo(ISyntaxElement.class)) {
                element.accept(visitor);
            }
            visitor.reset();
            
            progress.processedOne();
        }
        
        addResult(new ScatteringDegreeContainer(countedVariables.getResults()));
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Counted Variability Variables";
    }
}
