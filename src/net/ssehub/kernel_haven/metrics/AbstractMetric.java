package net.ssehub.kernel_haven.metrics;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AbstractAnalysis;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Abstract template class for all metrics. Metrics are special kinds of analyzes. They run on the extractor data
 * and always produce floating point numbers.<br />
 * Concrete metrics do not directly inherit from this class. Instead, classes called "filters" inherit from this class.
 * These filters prepare the extractor data to the correct aggregation level required by the metric. The metric
 * then extend these classes (template pattern) to implement just the metric algorithm.
 * 
 * @author Adam
 */
public abstract class AbstractMetric extends AbstractAnalysis {

    private boolean logToFile;
    
    private boolean logToConsole;
    
    /**
     * Creates a new abstract metric.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     */
    public AbstractMetric(Configuration config) {
        super(config);
        logToFile = Boolean.parseBoolean(config.getProperty("analysis.log_result.file", "true"));
        logToConsole = Boolean.parseBoolean(config.getProperty("analysis.log_result.console", "true"));
        
        if (!(logToConsole || logToFile)) {
            LOGGER.logWarning("Neither analysis.log_result.file nor analysis.log_result.console are set to true");
        }
    }
    
    @Override
    public void run() {
        try {
            vmProvider.start();
            bmProvider.start();
            cmProvider.start();
            
            // vm and bm providers only return either one result or one exception
            // so we first query the exception; if it is null, then everything is fine an we proceeed with normal
            // execution. If the exception is not null, then simply stop here
            
            boolean noException = true;
            
            ExtractorException e = vmProvider.getException();
            if (e != null) {
                LOGGER.logException("VM extractor created an exception; not executing metric", e);
                noException = false;
            }
            
            e = bmProvider.getException();
            if (e != null) {
                LOGGER.logException("BM extractor created an exception; not executing metric", e);
                noException = false;
            }
            
            if (noException) {
                List<MetricResult> result = run(cmProvider.getResultQueue(), bmProvider.getResult(),
                        vmProvider.getResult());
                handleOutput(result);
            }
            
        } catch (SetUpException e) {
            LOGGER.logException("Exception while starting metric", e);
        }
    }
    
    /**
     * Handles the output generated by the metric.
     * 
     * @param result The output of the metric. Must not be <code>null</code>.
     */
    private void handleOutput(List<MetricResult> result) {
        LOGGER.logInfo("Writing output...");
        
        if (logToConsole) {
            String[] lines = new String[result.size() + 1];
            int i = 0;
            lines[i++] = "Metric result:";
            for (MetricResult r : result) {
                lines[i++] = "\t" + r.getContext() + ": " + r.getValue();
            }
            LOGGER.logInfo(lines);
        }
        
        if (logToFile) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            LocalDateTime now = LocalDateTime.now();
            String timestamp = dtf.format(now);
            
            PrintStream out = createResultStream(this.getClass().getSimpleName() + ".result_" + timestamp + ".csv");
            PrintStream err = createResultStream(this.getClass().getSimpleName() + ".errors_" + timestamp + ".csv");
            
            out.println("Context;Value");
            for (MetricResult r : result) {
                out.println(r.getContext() + ";" + r.getValue());
            }
            out.close();
            
            err.println("file;exception");
            ExtractorException error;
            while ((error = cmProvider.getNextException()) != null) {
                if (error instanceof CodeExtractorException) {
                    CodeExtractorException exc = (CodeExtractorException) error;
                    err.print(exc.getCausingFile().getPath() + ";");
                    if (exc.getCause() != null) {
                        err.println(exc.getCause().toString());
                    } else {
                        err.println(exc.toString());
                    }
                } else {
                    err.println(";" + error.toString());
                }
            }
            err.close();
        }
    }
    
    /**
     * Executes the metric on the given extractor data. This method is called once. The filter is supposed to aggregate
     * the given data and execute the metric based on this.<br />
     * Based on the aggregation level, metrics may be executed multiple times. Because of this, this method returns a 
     * list of results.
     * 
     * @param codeModel The code model extractor data.
     * @param buildModel The build model extractor data.
     * @param varModel The variability model extractor data.
     * 
     * @return The result of the metric execution. Must not be <code>null</code>.
     */
    protected abstract List<MetricResult> run(BlockingQueue<SourceFile> codeModel, BuildModel buildModel,
            VariabilityModel varModel);

}
