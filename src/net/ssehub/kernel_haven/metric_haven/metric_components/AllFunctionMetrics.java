package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.JoinComponent;
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
import net.ssehub.kernel_haven.metric_haven.metric_components.BlocksPerFunctionMetric.BlockMeasureType;
import net.ssehub.kernel_haven.metric_haven.metric_components.CyclomaticComplexityMetric.CCType;
import net.ssehub.kernel_haven.metric_haven.metric_components.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.metric_components.FanInOutMetric.FanType;
import net.ssehub.kernel_haven.metric_haven.metric_components.NestingDepthMetric.NDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.VariablesPerFunctionMetric.VarType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all function metrics in parallel.
 * 
 * @author Adam
 */
public class AllFunctionMetrics extends AbstractMultiFunctionMetrics {
    
    private static final boolean JOIN_INTO_SINGLE_SHEET = false;
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
    @SuppressWarnings("null")
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
        
        config.registerSetting(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING);
        config.registerSetting(MetricSettings.CTCR_USAGE_SETTING);
        config.registerSetting(MetricSettings.LOCATION_DISTANCE_SETTING);
        config.registerSetting(MetricSettings.TYPE_MEASURING_SETTING);
        config.registerSetting(MetricSettings.TYPE_WEIGHTS_SETTING);
        config.setValue(MetricSettings.TYPE_WEIGHTS_SETTING,
            Arrays.asList("bool:1", "tristate:10", "string:100", "int:100", "integer:100", "hex:100"));
        config.registerSetting(MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING);
        config.registerSetting(MetricSettings.HIERARCHY_WEIGHTS_SETTING);
        config.setValue(MetricSettings.HIERARCHY_WEIGHTS_SETTING,
                Arrays.asList("top:1", "intermediate:10", "leaf:100"));
        config.registerSetting(MetricSettings.STRUCTURE_MEASURING_SETTING);
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
        config.setValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING, SDType.NO_SCATTERING);
        config.setValue(MetricSettings.CTCR_USAGE_SETTING, CTCRType.NO_CTCR);
        config.setValue(MetricSettings.LOCATION_DISTANCE_SETTING, FeatureDistanceType.NO_DISTANCE);
        config.setValue(MetricSettings.TYPE_MEASURING_SETTING, VariabilityTypeMeasureType.NO_TYPE_MEASURING);
        config.setValue(MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING, HierarchyType.NO_HIERARCHY_MEASURING);
        config.setValue(MetricSettings.STRUCTURE_MEASURING_SETTING, StructuralType.NO_STRUCTURAL_MEASUREMENT);
        
        // All dLoC per Function metrics
        addMetric(DLoC.class, DLoC.LOC_TYPE_SETTING, filteredFunctionSplitter, null, metrics, LoFType.values());
        
        // All internal ifdef block metrics
        addMetric(BlocksPerFunctionMetric.class, BlocksPerFunctionMetric.BLOCK_TYPE_SETTING, filteredFunctionSplitter,
            null, metrics, BlockMeasureType.values());
        
        // join the parallel metrics together
        @SuppressWarnings({"unchecked"})
        AnalysisComponent<MetricResult>[] metricComponents = metrics.toArray(new AnalysisComponent[metrics.size()]);
        AnalysisComponent<?> join;
        if (JOIN_INTO_SINGLE_SHEET) {
            join = new MetricsAggregator(config, "All Function Metrics", metricComponents);           
        } else {
            join = new JoinComponent(config, metricComponents);
        }
        
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
