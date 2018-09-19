package net.ssehub.kernel_haven.metric_haven.metric_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Super class for {@link AllFunctionMetrics}, {@link AllLineFilterableFunctionMetrics}, and
 * {@link AllNonLineFilterableFunctionMetrics} to share common functions.
 * @author El-Sharkawy
 *
 */
abstract class AbstractMultiFunctionMetrics extends PipelineAnalysis {

    /**
     * Creates this pipeline object.
     * 
     * @param config The global configuration.
     */
    AbstractMultiFunctionMetrics(@NonNull Configuration config) {
        super(config);
    }

    // CHECKSTYLE:OFF
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
    protected <MT> void addMetric(Class<? extends AbstractFunctionVisitorBasedMetric<?>> metric,
        @NonNull Setting<MT> setting,
        SplitComponent<CodeFunction> filteredFunctionSplitter,
        SplitComponent<ScatteringDegreeContainer> sdSplitter,
        List<@NonNull AnalysisComponent<MetricResult>> metrics,
        MT... settings) throws SetUpException {
        
        // Access constructor
        boolean useVariabilityWeights = (null != sdSplitter);
        Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor = null;
        try {
            metricConstructor = determineConstructor(metric, useVariabilityWeights);
        } catch (NoSuchMethodException e) {
            /* 
             * Fall back, if SD splitter is passed to this method,
             * but metric does not support variability weights.
             */
            try {
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class);
                useVariabilityWeights = false;
            } catch (NoSuchMethodException e1) {
                throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
            } catch (SecurityException e1) {
                throw new SetUpException("Was not allowed to create instance of " + metric.getName() + "-metric.", e);
            }
        }
        
        // Abort if constructor could not be accessed
        if (null == metricConstructor) {
            throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.");
        }
        
        // Register setting and create instances
        config.registerSetting(setting);
        for (int i = 0; i < settings.length; i++) {
            MT individualSetting = settings[i];
            config.setValue(setting, individualSetting);
            try {
                // Instantiate metric
                if (!useVariabilityWeights) {
                    AbstractFunctionVisitorBasedMetric<?> metricInstance = metricConstructor.newInstance(config,
                        filteredFunctionSplitter.createOutputComponent());
                    
                    // Add instance to list if instantiation was successful
                    if (null != metricInstance) {
                        metrics.add(metricInstance);
                    } else {
                        LOGGER.logWarning2("Could not create instance of ", metric.getName(), " with setting ",
                            individualSetting);
                    }
                } else {
                    // These metrics support: Scattering Degree, CTCR, Feature distance, feature type, hierarchy,
                    // and structural weights
                    metricVariationsLoop(metricConstructor, metric.getName(), individualSetting.toString(),
                        filteredFunctionSplitter, sdSplitter, metrics);
                }
            } catch (ReflectiveOperationException e) {
                throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
            }
        }
    }

    /**
     * Adds multiple instances of the specified metric to the metric list, which is to be executed in parallel.
     * @param metric The metric to executed (in different variations).
     * @param functionSplitter Needed to clone the code model in order to executed multiple metrics in parallel.
     * @param sdSplitter Optional if scattering degree should be considered in metric.
     * @param metrics The list to where the metrics shall be added to (modified as side effect).
     * @throws SetUpException If the metric could not be instantiated with the specified settings.
     */
    protected void addMetric(Class<? extends AbstractFunctionVisitorBasedMetric<?>> metric,
        SplitComponent<CodeFunction> functionSplitter,
        SplitComponent<ScatteringDegreeContainer> sdSplitter,
        List<@NonNull AnalysisComponent<MetricResult>> metrics) throws SetUpException {
        
        // Access constructor
        boolean useVariabilityWeights = (null != sdSplitter);
        Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor = null;
        try {
            metricConstructor = determineConstructor(metric, useVariabilityWeights);
        } catch (NoSuchMethodException e) {
            /* 
             * Fall back, if SD splitter is passed to this method,
             * but metric does not support variability weights.
             */
            try {
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class);
                useVariabilityWeights = false;
            } catch (NoSuchMethodException e1) {
                throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
            } catch (SecurityException e1) {
                throw new SetUpException("Was not allowed to create instance of " + metric.getName() + "-metric.", e);
            }
        }
        
        // Abort if constructor could not be accessed
        if (null == metricConstructor) {
            throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.");
        }
        
        // Run all metric instances with correct constructor
        try {
            // Instantiate metric
            if (!useVariabilityWeights) {
                AbstractFunctionVisitorBasedMetric<?> metricInstance = metricConstructor.newInstance(config,
                    functionSplitter.createOutputComponent());
                
                // Add instance to list if instantiation was successful
                if (null != metricInstance) {
                    metrics.add(metricInstance);
                } else {
                    LOGGER.logWarning2("Could not create instance of ", metric.getName());
                }
            } else {
                // These metrics support: Scattering Degree, CTCR, Feature distance, feature type, hierarchy,
                // and structural weights
                metricVariationsLoop(metricConstructor, metric.getName(), null, functionSplitter, sdSplitter, metrics);
            }
        } catch (ReflectiveOperationException e) {
            throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
        }
    }
    
    /**
     * Uses reflection to find an appropriate constructor of the metric class to use.
     * @param metric The metric class for which a constructor shall be retrieved.
     * @param useVariabilityWeights If <tt>true</tt> the constructor is used allowing to specify components required
     *     for variability weights, otherwise the constructor using a {@link Configuration} and one processing unit for
     *     {@link CodeFunction}s is used.
     * 
     * @return A constructor to use, won't be <tt>null</tt>.
     * @throws SetUpException If the constructor could not be created.
     * @throws NoSuchMethodException If the preferred constructor could not be created, please use the constructor
     *     without variability weights.
     */
    private Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> determineConstructor(
        Class<? extends AbstractFunctionVisitorBasedMetric<?>> metric, boolean useVariabilityWeights)
        throws SetUpException, NoSuchMethodException {
        
        Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor = null;
        try {
            if (!useVariabilityWeights) {
                // Default constructor
                try {
                    metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class);
                } catch (NoSuchMethodException e) {
                    throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
                }
            } else {
                // Constructor with VarModel, BuildModel, and Scattering Degree container
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class,
                    AnalysisComponent.class, AnalysisComponent.class, AnalysisComponent.class);
            }
        } catch (SecurityException e) {
            throw new SetUpException("Was not allowed to create instance of " + metric.getName() + "-metric.", e);
        }
        return metricConstructor;
    }

    /**
     * Runs a metric with all valid variations of default metric weights
     * (as implemented in {@link AbstractFunctionVisitorBasedMetric}). <br/>
     * These metrics support: Scattering Degree, CTCR, Feature distance, feature type, hierarchy, and structural weights
     * @param metricConstructor A constructor of the inherited metric with the parameters of
     *     {@link AbstractFunctionVisitorBasedMetric#AbstractFunctionVisitorBasedMetric(Configuration,
     *     AnalysisComponent, AnalysisComponent, AnalysisComponent, AnalysisComponent)}
     * @param metricName The name of the metric, for logging purpose only.
     * @param individualSettingValue The name of the current value implemented by the inherited class
     *     (for logging purpose only), the setting needs to be set before calling this method.
     * @param functionSplitter The processing unit providing the code functions to measure.
     * @param sdSplitter The component providing scattering degree values.
     * @param resultList The list to store the results (as a side effect).
     * @throws ReflectiveOperationException If the metric could not be instantiated because of wrong parameters.
     *     In this case the metric probably does not provide the right constructor.
     */
    private void metricVariationsLoop(
            Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor,
            String metricName,
            @Nullable String individualSettingValue,
            SplitComponent<CodeFunction> functionSplitter,
            SplitComponent<ScatteringDegreeContainer> sdSplitter,
            List<@NonNull AnalysisComponent<MetricResult>> resultList)
            throws InstantiationException, IllegalAccessException, AssertionError {
        
        AbstractFunctionVisitorBasedMetric<?> metricInstance;
        for (StructuralType structuralType : StructuralType.values()) {
            config.setValue(MetricSettings.STRUCTURE_MEASURING_SETTING, structuralType);
            for (HierarchyType hierarchyType : HierarchyType.values()) {
                config.setValue(MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING, hierarchyType);
                for (VariabilityTypeMeasureType weightType : VariabilityTypeMeasureType.values()) {
                    config.setValue(MetricSettings.TYPE_MEASURING_SETTING, weightType);
                    for (FeatureDistanceType distanceType : FeatureDistanceType.values()) {
                        config.setValue(MetricSettings.LOCATION_DISTANCE_SETTING, distanceType);
                        for (CTCRType ctcrType : CTCRType.values()) {
                            config.setValue(MetricSettings.CTCR_USAGE_SETTING, ctcrType);
                            for (SDType sdType : SDType.values()) {
                                config.setValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING, sdType);
                                
                                try {
                                    metricInstance = metricConstructor.newInstance(config,
                                        functionSplitter.createOutputComponent(),
                                        getVmComponent(),
                                        getBmComponent(),
                                        notNull(sdSplitter).createOutputComponent());
                                    
                                    // Add instance to list if instantiation was successful
                                    if (null != metricInstance) {
                                        resultList.add(metricInstance);
                                    } else {
                                        individualSettingValue = (null == individualSettingValue) ? ""
                                            : (individualSettingValue + " and ");
                                        
                                        LOGGER.logWarning2("Could not create instance of ", metricName,
                                            " with setting ", individualSettingValue, sdType, " and ", ctcrType,
                                            " and ", distanceType, " and ", weightType);
                                    }
                                } catch (InvocationTargetException exc) {
                                    Throwable orignException = exc.getTargetException();
                                    if (orignException instanceof UnsupportedMetricVariationException) {
                                        
                                        // Drop silently illegal combinations
                                        LOGGER.logDebug2("Discarded metric:\n", orignException.getMessage());
                                    } else {
                                        LOGGER.logException("Metric could not be instantiated.", orignException);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // CHECKSTYLE:ON
    



    /**
     * Registers all settings, which are used to realize variability weights in more than only in one specific
     * metric-class.
     * 
     * @throws SetUpException If the constraints for a setting are not satisfied,
     *     or a different setting with a same key was already registered.
     */
    protected void registerVariabilityWeights() throws SetUpException {
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
    }

    /**
     * Disables the values of {@link #registerVariabilityWeights()} to disable weights. Should be done for metrics,
     * which do not support the variability weights.
     */
    protected void unregisterVariabilityWeights() {
        config.setValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING, SDType.NO_SCATTERING);
        config.setValue(MetricSettings.CTCR_USAGE_SETTING, CTCRType.NO_CTCR);
        config.setValue(MetricSettings.LOCATION_DISTANCE_SETTING, FeatureDistanceType.NO_DISTANCE);
        config.setValue(MetricSettings.TYPE_MEASURING_SETTING, VariabilityTypeMeasureType.NO_TYPE_MEASURING);
        config.setValue(MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING, HierarchyType.NO_HIERARCHY_MEASURING);
        config.setValue(MetricSettings.STRUCTURE_MEASURING_SETTING, StructuralType.NO_STRUCTURAL_MEASUREMENT);
    }
}
