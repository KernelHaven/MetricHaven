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
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric.CCType;
import net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.metric_components.FanInOutMetric.FanType;
import net.ssehub.kernel_haven.metric_haven.metric_components.NestingDepthMetric.NDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.metric_haven.multi_results.MultiMetricResult;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics in parallel.
 * 
 * @author Adam
 */
public class AllFunctionMetrics extends AbstractMultiFunctionMetrics {
    
    /**
     * Whether this pipeline should add an {@link ObservableAnalysis} at the end or not.
     */
    private static boolean addObservable = false;
    
    /**
     * Whether this pipeline should add a {@link CodeFunctionByLineFilter} or not.
     */
    private static boolean addLineFilter = false;
    
    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    public AllFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    
    // CHECKSTYLE:OFF this method is too long, but splitting it up wouldn't make too much sense
    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
    // CHECKSTYLE:ON
        
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new CodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        SplitComponent<CodeFunction> filteredFunctionSplitter = functionSplitter;
        
        if (addLineFilter) {
            /*
             * One of the split outputs will get a filter and another split.
             * The resulting pipeline will look like this:
             * 
             * functionFilter -> Split -> CodeFunctionByLineFilter -> Split -> metric on filtered
             *                         -> metric on unfiltered              -> metric on filtered
             *                         -> metric on unfiltered              -> ....
             *                         -> ...
             */
            
            AnalysisComponent<CodeFunction> filteredFunctionFilter = functionSplitter.createOutputComponent();
            filteredFunctionFilter = new CodeFunctionByLineFilter(config, filteredFunctionFilter);
            filteredFunctionSplitter = new SplitComponent<>(config, filteredFunctionFilter);
        }

        // use functionSplitter.createOutputComponent() or filteredFunctionSplitter.createOutputComponent() to create
        // inputs for multiple metrics after the split
        
        config.registerSetting(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING);
        config.registerSetting(AbstractFunctionVisitorBasedMetric.CTCR_USAGE_SETTING);
        config.registerSetting(AbstractFunctionVisitorBasedMetric.LOCATION_DISTANCE_SETTING);
        AnalysisComponent<ScatteringDegreeContainer> sdAnalysis
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        SplitComponent<ScatteringDegreeContainer> sdSplitter = new SplitComponent<>(config, sdAnalysis);
        
        //VariabilityModel varModel = getVmComponent().getNextResult();
        
        
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics = new LinkedList<>();
        
        // All Cyclomatic complexity metrics
        addMetric(CyclomaticComplexityMetric.class, CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING,
            filteredFunctionSplitter, sdSplitter, metrics, CCType.values());

        // All Variables per Function metrics
        addMetric(VariablesPerFunctionMetric.class, VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING,
            filteredFunctionSplitter, sdSplitter, metrics, VarType.values());
        
        // Fan-in / Fan-out
        config.registerSetting(FanInOutMetric.FAN_TYPE_SETTING);
        addMetric(FanInOutMetric.class, FanInOutMetric.FAN_TYPE_SETTING, functionSplitter, sdSplitter,
            metrics, FanType.values());
        
        // All Nesting Depth metrics
        addMetric(NestingDepthMetric.class, NestingDepthMetric.ND_TYPE_SETTING, filteredFunctionSplitter, sdSplitter,
            metrics, NDType.values());
        
        // Disable variability weights
        config.setValue(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING, SDType.NO_SCATTERING);
        config.setValue(AbstractFunctionVisitorBasedMetric.CTCR_USAGE_SETTING, CTCRType.NO_CTCR);
        config.setValue(AbstractFunctionVisitorBasedMetric.LOCATION_DISTANCE_SETTING, FeatureDistanceType.NO_DISTANCE);
        
        // All dLoC per Function metrics
        addMetric(DLoC.class, DLoC.LOC_TYPE_SETTING, filteredFunctionSplitter, null, metrics, LoFType.values());
        
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
        AllFunctionMetrics.addObservable = addObservable;
    }
    
    /**
     * Whether this pipeline should add a {@link CodeFunctionByLineFilter} or not.
     * Default is <code>false</code>.
     * 
     * @param addLineFilter <code>true</code> if a {@link CodeFunctionByLineFilter} should be added
     */
    public static void setAddLineFilter(boolean addLineFilter) {
        AllFunctionMetrics.addLineFilter = addLineFilter;
    }

}
