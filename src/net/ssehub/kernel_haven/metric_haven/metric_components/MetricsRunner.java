package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.config.Setting.Type.STRING;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.JoinComponent;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.OrderedCodeFunctionFilter;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.VariabilityCounter;
import net.ssehub.kernel_haven.metric_haven.multi_results.MetricsAggregator;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Runs all variations of a single metric-analysis class.
 * @author El-Sharkawy
 *
 */
public class MetricsRunner extends AbstractMultiFunctionMetrics {
    
    public static final @NonNull Setting<@NonNull String> METRICS_CLASS
        = new Setting<>("analysis.metrics_runner.metrics_class", STRING, true, null,
            "The fully qualified class name of the metric that should be run.");
    private static final int MAX_METRICS_PER_AGGREGATOR = 1150;
    
    private Class<? extends AbstractFunctionVisitorBasedMetric<?>> metricClass;

    /**
     * Single constructor to instantiate and execute all variations of a single metric-analysis class.
     * @param config The global configuration.
     * @throws SetUpException In case of problems with the configuration of {@link #METRICS_CLASS}.
     */
    @SuppressWarnings("unchecked")
    public MetricsRunner(@NonNull Configuration config) throws SetUpException {
        super(config);
        config.registerSetting(METRICS_CLASS);
        String metricsClassName = config.getValue(METRICS_CLASS);
        try {
            metricClass = (Class<? extends AbstractFunctionVisitorBasedMetric<?>>)
                ClassLoader.getSystemClassLoader().loadClass(metricsClassName);
        } catch (ClassNotFoundException | ClassCastException exc) {
            LOGGER.logException("Could not load specified metric analysis class:", exc);
        }
    }

    @Override
    @SuppressWarnings("null")
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        AnalysisComponent<SourceFile> codeModel = getCmComponent();
        AnalysisComponent<CodeFunction> functionFilter = new OrderedCodeFunctionFilter(config, codeModel);
        
        // add a split component after the function filter
        SplitComponent<CodeFunction> functionSplitter = new SplitComponent<>(config, functionFilter);
        
        // Activate variability weights
        registerVariabilityWeights();
        AnalysisComponent<ScatteringDegreeContainer> sdAnalysis
            = new VariabilityCounter(config, getVmComponent(), getCmComponent());
        SplitComponent<ScatteringDegreeContainer> sdSplitter = new SplitComponent<>(config, sdAnalysis);
        
        // Determine individual settings of metric-analysis class
        List<@NonNull Setting<?>> settings = determineSettings(metricClass);

        // Start all metric variations
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics = new LinkedList<>();
        for (@NonNull Setting<?> setting : settings) {
            createAllVariations(setting, functionSplitter, sdSplitter, metrics);
        }
        
        // Disable variability weights
        unregisterVariabilityWeights();
        
        // Start and join all metrics into a single sheet
        @SuppressWarnings("unchecked")
        AnalysisComponent<MetricResult>[] metricComponents = metrics.toArray(new AnalysisComponent[metrics.size()]);
        AnalysisComponent<?> join;
        if (metricComponents.length > MAX_METRICS_PER_AGGREGATOR) {
            int nAggregators = (int) Math.ceil(((double) metricComponents.length) % MAX_METRICS_PER_AGGREGATOR);
            MetricsAggregator[] aggregators = new MetricsAggregator[nAggregators];
            int index = 0;
            for (int i = 0; i < metricComponents.length; i += MAX_METRICS_PER_AGGREGATOR) {
                AnalysisComponent<MetricResult>[] interval = new AnalysisComponent[MAX_METRICS_PER_AGGREGATOR];
                int nElements = Math.min(MAX_METRICS_PER_AGGREGATOR, metricComponents.length - i);
                System.arraycopy(metricComponents, i, interval, 0, nElements);
                aggregators[index] = new MetricsAggregator(config, metricClass.getSimpleName() + index++, interval);
            }
            config.setValue(DefaultSettings.ANALYSIS_SPLITCOMPONENT_MAX_THREADS, 1);
            join = new JoinComponent(config, aggregators);
        } else {
            join = new MetricsAggregator(config, metricClass.getSimpleName(), metricComponents);
        }
        
        return join;
    }
    
    /**
     * Creates and instantiates (but not starts) all variations of the metric class ({@link #metricClass}.
     * @param setting An individual settings defined in the metrics class to iterate through.
     * @param functionSplitter Needed to clone the code model in order to executed multiple metrics in parallel.
     * @param sdSplitter Optional if scattering degree should be considered in metric.
     * @param metrics A list containing all scheduled metric instances (will be changed as side-effect).
     * @param <MT> The individual <b>M</b>etrics settings <b>T</b>ype
     */
    @SuppressWarnings("unchecked")
    private <MT> void createAllVariations(@NonNull Setting<?> setting, SplitComponent<CodeFunction> functionSplitter,
        @NonNull SplitComponent<ScatteringDegreeContainer> sdSplitter,
        @NonNull List<@NonNull AnalysisComponent<MetricResult>> metrics) {
        
        // Determine specific enumeration class
        Class<? extends Enum<?>> enumClass = null;
        Enum<?>[] values = null;
        if (setting instanceof EnumSetting<?>) {
            EnumSetting<?> enumSetting = (EnumSetting<?>) setting;
            enumClass = enumSetting.getEnumClass();
            values = enumClass.getEnumConstants();
        }
        
        if (null != enumClass && values != null) {
            // Perform (dirty) casts to fulfill requirements of "generic" method
            MT[] metricTypeSettings = (MT[]) Array.newInstance(enumClass, values.length);
            System.arraycopy(values, 0, metricTypeSettings, 0, values.length);
            Setting<MT> castedSetting = (Setting<MT>) setting;
            
            try {
                // Execute all variations of metric class
                addMetric(metricClass, castedSetting, functionSplitter, sdSplitter, metrics, metricTypeSettings);
            } catch (SetUpException exc) {
                LOGGER.logException("Could not start Metric " + metricClass.getName(), exc);
            }
        }
    }

    /**
     * Analyzes the given classes and extracts all {@link Setting}s specified in that class (not inherited settings).
     * @param analysisClass The analysis class to check for individual settings.
     * @return The individual settings specified in the given class, maybe <tt>null</tt> if the class does not specify
     *     any individual settings.
     */
    private static List<@NonNull Setting<?>> determineSettings(Class<?> analysisClass) {
        List<@NonNull Setting<?>> settings = new ArrayList<>();
        
        Field[] fields = analysisClass.getDeclaredFields();
        for (Field field : fields) {
            if (Setting.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    Setting<?> analysisSetting = (Setting<?>) field.get(null);
                    if (null != analysisSetting) {
                        settings.add(analysisSetting);
                    }
                } catch (ReflectiveOperationException exc) {
                    LOGGER.logException("Could not extract settings from " + analysisClass.getName(), exc);
                }
            }
        }
        
        return settings;
    }
}