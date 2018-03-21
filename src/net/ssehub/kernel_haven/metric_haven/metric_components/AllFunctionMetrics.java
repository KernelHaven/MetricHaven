package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.ObservableAnalysis;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
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
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Runs all function metrics in parallel.
 * 
 * @author Adam
 */
public class AllFunctionMetrics extends PipelineAnalysis {
    
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
        AnalysisComponent<ScatteringDegreeContainer> sdAnalysis
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        SplitComponent<ScatteringDegreeContainer> sdSplitter = new SplitComponent<>(config, sdAnalysis);
        
        //VariabilityModel varModel = getVmComponent().getNextResult();
        
        
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics = new LinkedList<>();
        
        // All Cyclomatic complexity metrics
        addMetric(CyclomaticComplexityMetric.class, CyclomaticComplexityMetric.VARIABLE_TYPE_SETTING,
            filteredFunctionSplitter, null, metrics, CCType.values());

        // All Variables per Function metrics
        addMetric(VariablesPerFunctionMetric.class, VariablesPerFunctionMetric.VARIABLE_TYPE_SETTING,
            filteredFunctionSplitter, sdSplitter, metrics, VarType.values());
        config.setValue(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING, SDType.NO_SCATTERING);
        
        // All dLoC per Function metrics
        addMetric(DLoC.class, DLoC.LOC_TYPE_SETTING, filteredFunctionSplitter, null, metrics, LoFType.values());
        
        // All Nesting Depth metrics
        addMetric(NestingDepthMetric.class, NestingDepthMetric.ND_TYPE_SETTING, filteredFunctionSplitter, null, metrics,
            NDType.values());
        
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
     * Adds multiple instances of the specified metric to the metric list, which is to be executed in parallel.
     * @param metric The metric to executed (in different variations).
     * @param setting The setting to be varied.
     * @param filteredFunctionSplitter Needed to clone the code model in order to executed multiple metrics in parallel.
     * @param sdSplitter Optional if scattering degree should be considered in metric.
     * @param metrics The list to where the metrics shall be added to (modified as side effect).
     * @param settings The different setting values to be used (for each of this settings,
     *     a metric instance will be created).
     * @param <MT> The <b>M</b>etrics settings <b>T</b>ype
     * @throws SetUpException If the metric could not be instantiated with the specified settings.
     */
    @SuppressWarnings({"unchecked"})
    // CHECKSTYLE:OFF
    private <MT> void addMetric(Class<? extends AbstractFunctionVisitorBasedMetric<?>> metric,
        @NonNull Setting<MT> setting,
        SplitComponent<CodeFunction> filteredFunctionSplitter,
        SplitComponent<ScatteringDegreeContainer> sdSplitter,
        List<@NonNull AnalysisComponent<MetricResult>> metrics,
        MT... settings) throws SetUpException {
     // CHECKSTYLE:ON
        
        // Access constructor
        Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor = null;
        try {
            if (null == sdSplitter) {
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class);
            } else {
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class,
                    AnalysisComponent.class, AnalysisComponent.class);
            }
        } catch (ReflectiveOperationException e) {
            throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
        } catch (SecurityException e) {
            throw new SetUpException("Was not allowed to create instance of " + metric.getName() + "-metric.", e);
        }
        
        // Abort if constructor could not be accessed
        if (null == metricConstructor) {
            throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.");
        }
        
        // Register setting and create instances
        config.registerSetting(setting);
        for (int i = 0; i < settings.length; i++) {
            config.setValue(setting, settings[i]);
            try {
                // Instantiate metric
                AbstractFunctionVisitorBasedMetric<?> metricInstance = null;
                if (null == sdSplitter) {
                    metricInstance = metricConstructor.newInstance(config,
                        filteredFunctionSplitter.createOutputComponent());
                    
                    // Add instance to list if instantiation was successful
                    if (null != metricInstance) {
                        metrics.add(metricInstance);
                    } else {
                        LOGGER.logWarning("Could not create instance of " + metric.getName() + " with setting "
                            + settings[i]);
                    }
                } else {
                    for (SDType sdType : SDType.values()) {
                        config.setValue(AbstractFunctionVisitorBasedMetric.SCATTERING_DEGREE_USAGE_SETTING, sdType);
                        metricInstance = metricConstructor.newInstance(config,
                            filteredFunctionSplitter.createOutputComponent(),
                            getVmComponent(),
                            sdSplitter.createOutputComponent());
                        
                        // Add instance to list if instantiation was successful
                        if (null != metricInstance) {
                            metrics.add(metricInstance);
                        } else {
                            LOGGER.logWarning("Could not create instance of " + metric.getName() + " with setting "
                                + settings[i] + " and " + sdType);
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
            }
        }
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
