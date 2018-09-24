package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.metric_haven.multi_results.MeasuredItem;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A processing unit for executing metrics.
 *  
 * @author Adam
 */
public class CodeMetricsRunner extends AnalysisComponent<MultiMetricResult> {
    
    public static final @NonNull Setting<@NonNull Integer> MAX_THREADS = new Setting<>("metrics.max_parallel_threads",
        Type.INTEGER, true, "0", "If greater than 0, a thread pool is used to limit the maximum number of threads "
            + "executed in parallel.");
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    
    private int nThreads;
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * @param sdComponent Scattering degree values, used to create the {@link ScatteringWeight}.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public CodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent,
        @NonNull AnalysisComponent<BuildModel> bmComponent,
        @NonNull AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config);
        
        this.codeFunctionComponent = codeFunctionComponent;
        this.varModelComponent = varModelComponent;
        this.bmComponent = bmComponent;
        this.sdComponent = sdComponent;
        
        try {
            config.registerSetting(MAX_THREADS);
            nThreads = config.getValue(MAX_THREADS);
        } catch (SetUpException exc) {
            LOGGER.logException("Could not load configuration setting " + MAX_THREADS.getKey(), exc);
        }
        
    }

    @Override
    protected void execute() {
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        BuildModel bm = (null != bmComponent) ? bmComponent.getNextResult() : null;
        ScatteringDegreeContainer sdContainer = (null != sdComponent) ? sdComponent.getNextResult() : null;
        
        List<@NonNull AbstractFunctionMetric<?>> allMetrics;
        try {
            if (varModel == null) {
                throw new SetUpException("VariabilityModel is null");
            }
            if (bm == null) {
                throw new SetUpException("BuildModel is null");
            }
            if (sdContainer == null) {
                throw new SetUpException("ScatteringDegreeContainer is null");
            }
            
            MetricCreationParameters params = new MetricCreationParameters(varModel, bm, sdContainer);
            
            allMetrics = MetricFactory.createAllVariations(params);
            
        } catch (SetUpException e) {
            LOGGER.logException("Could not create metric instances", e);
            return;
        }
        
        String[] metrics = new String[allMetrics.size()];
        int metricsIndex = 0;
        for (AbstractFunctionMetric<?> metric : allMetrics) {
            metrics[metricsIndex++] = metric.getResultName();
        }
        Double[] values = new Double[allMetrics.size()];
        
        CodeFunction function;
        while ((function = codeFunctionComponent.getNextResult()) != null) {
            LOGGER.logDebug2("Running for function ", function.getName(), " at ", function.getSourceFile(),
                   ":", function.getFunction().getLineStart());
            
            int valuesIndex = 0;
            for (AbstractFunctionMetric<?> metric : allMetrics) {
                values[valuesIndex++] = metric.compute(function).doubleValue();
            }
            
            MultiMetricResult result = new MultiMetricResult(
                    new MeasuredItem(notNull(function.getSourceFile().getPath().getPath()),
                            function.getFunction().getLineStart(), function.getName()),
                    metrics, values);
            
            addResult(result);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "AllCodeFunctions";
    }

}
