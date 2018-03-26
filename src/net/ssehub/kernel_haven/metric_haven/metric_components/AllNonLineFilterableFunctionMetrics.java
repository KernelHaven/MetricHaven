package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.ObservableAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionByLineFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.VariabilityCounter;
import net.ssehub.kernel_haven.metric_haven.metric_components.FanInOutMetric.FanType;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics that can not be filtered by a {@link CodeFunctionByLineFilter} in parallel.
 * 
 * @author Adam
 */
public class AllNonLineFilterableFunctionMetrics extends AbstractMultiFunctionMetrics {

    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     */
    private static boolean addObservable = false;
    
    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllNonLineFilterableFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        config.registerSetting(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING);
        config.registerSetting(AbstractFunctionVisitorBasedMetric.CTCR_USAGE_SETTING);
        AnalysisComponent<ScatteringDegreeContainer> sdAnalysis
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        SplitComponent<ScatteringDegreeContainer> sdSplitter = new SplitComponent<>(config, sdAnalysis);
        
        // use functionSplitter.createOutputComponent() to create inputs for multiple metrics after the split
        
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics = new LinkedList<>();
        
        // Fan-in / Fan-out
        config.registerSetting(FanInOutMetric.FAN_TYPE_SETTING);
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_IN_GLOBALLY);
        metrics.add(new FanInOutMetric(config, functionSplitter.createOutputComponent()));
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_IN_LOCALLY);
        metrics.add(new FanInOutMetric(config, functionSplitter.createOutputComponent()));
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_OUT_GLOBALLY);
        metrics.add(new FanInOutMetric(config, functionSplitter.createOutputComponent()));
        config.setValue(FanInOutMetric.FAN_TYPE_SETTING, FanType.CLASSICAL_FAN_OUT_LOCALLY);
        metrics.add(new FanInOutMetric(config, functionSplitter.createOutputComponent()));
        
        // All Variables per Function metrics
        addMetric(VariablesPerFunctionMetric.class, VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING,
            functionSplitter, sdSplitter, metrics, VarType.values());
        config.setValue(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING, SDType.NO_SCATTERING);
        config.setValue(AbstractFunctionVisitorBasedMetric.CTCR_USAGE_SETTING, CTCRType.NO_CTCR);
        
        // join the parallel metrics together
        @SuppressWarnings({ "null", "unchecked" })
        AnalysisComponent<MultiMetricResult> join = new MetricsAggregator(config, "All Function Metrics",
            metrics.toArray(new AnalysisComponent[metrics.size()]));
        
        if (addObservable) {
            join = new ObservableAnalysis<>(config, join);
        }
        
        return join;
    }
    
    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     * Default is <code>false</code>.
     * 
     * @param addObservable <code>true</code> if an {@link ObservableAnalysis} should be added.
     */
    public static void setAddObservable(boolean addObservable) {
        AllNonLineFilterableFunctionMetrics.addObservable = addObservable;
    }
    
}
