package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

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
        Constructor<? extends AbstractFunctionVisitorBasedMetric<?>> metricConstructor = null;
        try {
            if (null == sdSplitter) {
                // Default constructor
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class);
            } else {
                // Constructor with VarModel, BuildModel, and Scattering Degree container
                metricConstructor = metric.getConstructor(Configuration.class, AnalysisComponent.class,
                    AnalysisComponent.class, AnalysisComponent.class, AnalysisComponent.class);
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
                        LOGGER.logWarning2("Could not create instance of ", metric.getName(), " with setting ",
                            settings[i]);
                    }
                } else {
                    // These metrics support scattering degree AND CTCR ratio AND Feature distance
                    for (FeatureDistanceType distanceType : FeatureDistanceType.values()) {
                        config.setValue(MetricSettings.LOCATION_DISTANCE_SETTING, distanceType);
                        for (CTCRType ctcrType : CTCRType.values()) {
                            config.setValue(MetricSettings.CTCR_USAGE_SETTING, ctcrType);
                            for (SDType sdType : SDType.values()) {
                                config.setValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING, sdType);
                                
                                try {
                                    metricInstance = metricConstructor.newInstance(config,
                                        filteredFunctionSplitter.createOutputComponent(),
                                        getVmComponent(),
                                        getBmComponent(),
                                        sdSplitter.createOutputComponent());
                                    
                                    // Add instance to list if instantiation was successful
                                    if (null != metricInstance) {
                                        metrics.add(metricInstance);
                                    } else {
                                        LOGGER.logWarning2("Could not create instance of ", metric.getName(),
                                            " with setting ", settings[i], " and ", sdType);
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
            } catch (ReflectiveOperationException e) {
                throw new SetUpException("Could not create instance of " + metric.getName() + "-metric.", e);
            }
        }
    }
    
    // CHECKSTYLE:ON
}
