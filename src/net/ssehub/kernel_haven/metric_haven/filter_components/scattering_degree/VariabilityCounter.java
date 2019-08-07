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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.metric_components.CodeMetricsRunner;
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
    
    private int nThreads;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param vmProvider The component to get the variability model from.
     * @param cmProvider The component to get the code model from.
     * @throws SetUpException 
     */
    public VariabilityCounter(@NonNull Configuration config, @NonNull AnalysisComponent<VariabilityModel> vmProvider,
            @NonNull AnalysisComponent<SourceFile<?>> cmProvider) throws SetUpException {
        super(config);
        
        this.vmProvider = vmProvider;
        this.cmProvider = cmProvider;
        this.countedVariables = new CountedVariables();
        
        config.registerSetting(CodeMetricsRunner.MAX_THREADS);
        nThreads = config.getValue(CodeMetricsRunner.MAX_THREADS);
        if (nThreads <= 0) {
            throw new SetUpException("Need at least one thread specified in " + CodeMetricsRunner.MAX_THREADS.getKey()
                + " (got " + nThreads + ")");
        }
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
        
        if (1 == nThreads) {
            SourceFile<?> file;
            ScatteringVisitor visitor = new ScatteringVisitor(countedVariables);
            while ((file = cmProvider.getNextResult()) != null) {
                
                for (ISyntaxElement element : file.castTo(ISyntaxElement.class)) {
                    element.accept(visitor);
                }
                visitor.reset();
                
                progress.processedOne();
            }
        } else {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);
            SourceFile<?> file;
            while ((file = cmProvider.getNextResult()) != null) {
                
                final SourceFile<ISyntaxElement> astFile = file.castTo(ISyntaxElement.class);
                Runnable runnable = () -> {
                    ScatteringVisitor visitor = new ScatteringVisitor(countedVariables);
                    for (ISyntaxElement element : astFile) {
                        element.accept(visitor);
                    }
                    
                    progress.processedOne();
                };
                
                executor.execute(runnable);
            }
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                LOGGER.logError("Threads did not finish in time, could not compute Scattering Degree values.");
                progress.close();
                return;
            }
        }
        
        addResult(new ScatteringDegreeContainer(countedVariables.getResults()));
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Counted Variability Variables";
    }
}
