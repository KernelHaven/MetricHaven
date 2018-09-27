package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.MultiWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Creates one or multiple metric instances with their corresponding variations.
 *  
 * @author Adam
 * @author Sascha El-Sharkawy
 */
public class MetricFactory {
    
    /**
     * Holds all parameters necessary for metric creation. 
     */
    public static class MetricCreationParameters {
        
        private @NonNull VariabilityModel varModel;
        
        private @NonNull BuildModel buildModel;
        
        private @NonNull ScatteringDegreeContainer sdContainer;
        
        private IVariableWeight weight;
        
        private FunctionMap functionMap; // for FanInOutmetric
        
        private Object metricSpecificSettingValue;
        
        private @Nullable SDType sdValue;
        private @Nullable CTCRType ctcrValue;
        private @Nullable FeatureDistanceType distanceValue;
        private @Nullable VariabilityTypeMeasureType varTypeValue;
        private @Nullable HierarchyType hierarhcyValue;
        private @Nullable StructuralType structureValue;
        private boolean singleMetricExecution;
        
        
        /**
         * Creates configuration parameters to instantiate a metric via the {@link MetricFactory}.
         * @param varModel The {@link VariabilityModel}
         * @param buildModel The {@link BuildModel}, required for the {@link VariablesPerFunction}
         * @param sdContainer The {@link ScatteringDegreeContainer} as required by the {@link ScatteringWeight}.
         */
        public MetricCreationParameters(@NonNull VariabilityModel varModel, @NonNull BuildModel buildModel,
            @NonNull ScatteringDegreeContainer sdContainer) {
            
            this.varModel = varModel;
            this.buildModel = buildModel;
            this.sdContainer = sdContainer;
            singleMetricExecution = false;
        }
        
        /**
         * Returns the {@link VariabilityModel}.
         * @return The {@link VariabilityModel}.
         */
        public @NonNull VariabilityModel getVarModel() {
            return varModel;
        }
        
        /**
         * Returns the {@link BuildModel}.
         * @return The {@link BuildModel}.
         */
        public @NonNull BuildModel getBuildModel() {
            return buildModel;
        }
        
        /**
         * Returns the {@link ScatteringDegreeContainer} as required by the {@link ScatteringWeight}.
         * @return The {@link ScatteringDegreeContainer}.
         */
        public @NonNull ScatteringDegreeContainer getSdContainer() {
            return sdContainer;
        }
        
        /**
         * Specifies the {@link IVariableWeight} to use. If multiple weights shall be used, use {@link MultiWeight}, if
         * no weight shall be used, use {@link NoWeight#INSTANCE},
         * @param weight The {@link IVariableWeight} to use.
         */
        public void setWeight(IVariableWeight weight) {
            this.weight = weight;
        }
        
        /**
         * Returns the {@link IVariableWeight} to use, maybe {@link MultiWeight} or {@link NoWeight#INSTANCE}.
         * @return The {@link IVariableWeight} to use.
         */
        public IVariableWeight getWeight() {
            return weight;
        }
        
        /**
         * Specifies a setting value for a metric-specific setting.
         * @param metricSpecificSettingValue The metric-specific setting value to use.
         */
        public void setMetricSpecificSettingValue(Object metricSpecificSettingValue) {
            this.metricSpecificSettingValue = metricSpecificSettingValue;
        }
        
        /**
         * Reads a value, which is specific for a single {@link AbstractFunctionMetric}.
         * @param type The enumeration , which shall be retrieved.
         * @param <T> The enumeration class of the setting to retrieve.
         * @return A value of the individual metric setting.
         * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
         *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
         */
        @SuppressWarnings("unchecked")
        public <T> T getMetricSpecificSettingValue(Class<T> type) throws SetUpException {
            if (metricSpecificSettingValue == null || !type.isAssignableFrom(metricSpecificSettingValue.getClass())) {
                throw new SetUpException("Invalid metric specific type; expected " + type.getName() + " but got "
                    + (metricSpecificSettingValue == null ? "null" : metricSpecificSettingValue.getClass().getName()));
            }
            return (T) metricSpecificSettingValue;
        }
        
        /**
         * Sets a (reusable) function map, which is required by {@link FanInOut}-metrics.
         * @param functionMap A {@link FunctionMap} containing information about all existing functions
         * (even if only a subset shall be measured).
         */
        public void setFunctionMap(FunctionMap functionMap) {
            this.functionMap = functionMap;
        }
        
        /**
         * Returns the {@link FunctionMap} as required for measuring {@link FanInOut}.
         * @return The {@link FunctionMap} as required for measuring {@link FanInOut}.
         */
        public FunctionMap getFunctionMap() {
            return functionMap;
        }
        
        public void setScatteringDegree(SDType sdSpecification) {
            this.sdValue = sdSpecification;
        }
        
        public SDType getScatteringDegree() {
            return sdValue;
        }
        
        public void setSingleMetricExecution(boolean singleMetricExecution) {
            this.singleMetricExecution = singleMetricExecution;
        }
        
        public boolean isSingleMetricExecution() {
            return singleMetricExecution;
        }
    }
    
    private static final List<@NonNull Class<? extends AbstractFunctionMetric<?>>> SUPPORTED_METRICS;
    private static final Map<@NonNull Class<? extends AbstractFunctionMetric<?>>, Setting<?>>
        METRIC_SPECIFIC_SETTINGS;
    
    static {
        List<@NonNull Class<? extends AbstractFunctionMetric<?>>> tmpList = new ArrayList<>();
        tmpList.add(DLoC.class);
        tmpList.add(VariablesPerFunction.class);
        tmpList.add(CyclomaticComplexity.class);
        tmpList.add(NestingDepth.class);
        tmpList.add(BlocksPerFunctionMetric.class);
        tmpList.add(TanglingDegree.class);
//        tmpList.add(FanInOut.class);
        SUPPORTED_METRICS = Collections.unmodifiableList(tmpList);
        
        Map<@NonNull Class<? extends AbstractFunctionMetric<?>>, Setting<?>> settings = new HashMap<>();
        settings.put(BlocksPerFunctionMetric.class, MetricSettings.BLOCK_TYPE_SETTING);
        settings.put(CyclomaticComplexity.class, MetricSettings.CC_VARIABLE_TYPE_SETTING);
        settings.put(DLoC.class, MetricSettings.LOC_TYPE_SETTING);
        settings.put(FanInOut.class, MetricSettings.FAN_TYPE_SETTING);
        settings.put(NestingDepth.class, MetricSettings.ND_TYPE_SETTING);
        // Tangling degree does not specify metric-specific settings
        settings.put(VariablesPerFunction.class, MetricSettings.VARIABLE_TYPE_SETTING);
        METRIC_SPECIFIC_SETTINGS = Collections.unmodifiableMap(settings);
    }
    
    /**
     * Detects enumerations declared in the specified metric class, these are used to create individual settings.
     * @param metricClass The metric to instantiate by the factory.
     * @return A list of all individual setting enumerations, probably 1, never <tt>null</tt>.
     */
    private static List<Class<?>> getSettings(@NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) {
        List<Class<?>> enumSettings = new ArrayList<>();
        
        for (Class<?> cls : metricClass.getDeclaredClasses()) {
            if (Enum.class.isAssignableFrom(cls)) {
                enumSettings.add(cls);
            }
        }
        
        return enumSettings;
    }
    
    /**
     * Creates a single metric instance. Illegal combinations will silently be dropped (method returns <tt>null</tt>).
     * @param constructor The constructor of the metric to use.
     * @param params The parameters for creating the class ({@link VariabilityModel}, {@link BuildModel},
     *     individual settings, ...)
     * 
     * @return The instantiated metric, may be <tt>null</tt> in case of an illegal combination of parameters.
     * @throws SetUpException In case the metric throws a SetUpException.
     */
    private static @Nullable AbstractFunctionMetric<?> createInstance(Constructor<?> constructor,
            @NonNull MetricCreationParameters params) throws SetUpException {
        
        AbstractFunctionMetric<?> metricInstance = null;
        try {
            Object instance = constructor.newInstance(params);
            metricInstance = (AbstractFunctionMetric<?>) instance;
       
        } catch (InvocationTargetException e) {
            Throwable orgException = e.getTargetException();
            
            if (!(orgException instanceof UnsupportedMetricVariationException)) {
                // Drop UnsupportedMetricVariationExceptions: These combinations may be created by the factory...
                if (orgException instanceof SetUpException) {
                    // Stop whole loop and do not create any instance
                    throw (SetUpException) orgException;
                } else {
                    // Try to create other instances
                    Logger.get().logException("Could not instantiate a metric with constructor: "
                        + constructor.toGenericString(), orgException);
                }
            }
        } catch (ReflectiveOperationException e) {
            // Try to create other instances
            Logger.get().logException("Could not instantiate a metric with constructor: "
                + constructor.toGenericString(), e);
        }
        
        return metricInstance;
    }
    
    /**
     * Creates all combinations for a given metric.
     * @param params The parameters for creating the class ({@link VariabilityModel}, {@link BuildModel},
     *     individual settings, ...)
     * @param metricClass The metric to instantiate.
     *
     * @return A list of all valid  metric combinations.
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    private static @NonNull List<@NonNull AbstractFunctionMetric<?>>
        createAllVariations(@NonNull MetricCreationParameters params,
        @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) throws SetUpException {
        
        @NonNull List<@NonNull AbstractFunctionMetric<?>> result = new LinkedList<>();
        List<Class<?>> enumSettings = getSettings(metricClass);
        
        try {
            Constructor<?> constructor = null;
            try {
                constructor = metricClass.getDeclaredConstructor(MetricCreationParameters.class);
            } catch (NoSuchMethodException exc) {
                Logger.get().logException("Could not load constructor for " + metricClass.getName(), exc);
            }
            
            // Create instances by instantiating all legal combinations
            if (null != constructor) {
                if (!enumSettings.isEmpty()) {
                    for (Class<?> setting : enumSettings) {
                        // TODO SE: Create Cartesian product if more than one enum exist.
                        for (Object value : setting.getEnumConstants()) {
                            params.setMetricSpecificSettingValue(value);
                            AbstractFunctionMetric<?> metric = createInstance(constructor, params);
                            if (null != metric) {
                                result.add(metric);
                            }
                        }
                    }
                } else {
                    params.setMetricSpecificSettingValue(null);
                    AbstractFunctionMetric<?> metric = createInstance(constructor, params);
                    if (null != metric) {
                        result.add(metric);
                    }
                }
            } else {
                Logger.get().logError2("Could not detect constructors of metric: ", metricClass.getName());
            }
        } catch (SecurityException e) {
            Logger.get().logError2("Was not allowed to instantiate metric: ", metricClass.getName());
        }
        
        
        return result;
    }
    
    /**
     * 
     * @param params The parameters for creating the class ({@link VariabilityModel}, {@link BuildModel},
     *     individual settings, ...)
     * @param metricClass The metric to instantiate.
     *
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    private static @Nullable AbstractFunctionMetric<?> createMetric(@NonNull MetricCreationParameters params,
        @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass)
        throws SetUpException {
        
        AbstractFunctionMetric<?> result = null;
        
        try {
            Constructor<?> constructor = null;
            try {
                constructor = metricClass.getDeclaredConstructor(MetricCreationParameters.class);
            } catch (NoSuchMethodException exc) {
                Logger.get().logException("Could not load constructor for " + metricClass.getName(), exc);
            }
            
            // Create instances by instantiating all legal combinations
            if (null != constructor) {
                result = createInstance(constructor, params);
            } else {
                Logger.get().logError2("Could not detect constructors of metric: ", metricClass.getName());
            }
        } catch (SecurityException e) {
            Logger.get().logError2("Was not allowed to instantiate metric: ", metricClass.getName());
        }
        
        
        return result;
    }
    
    /**
     * Creates all valid variations of all code function metrics, each variation will appear only once.
     * 
     * @param params The parameters for creating metrics.
     * 
     * @return All valid variations of all code function metrics
     * @throws SetUpException In case that at least one metric instance throws a SetUpException.
     */
    public static @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllVariations(
            @NonNull MetricCreationParameters params) throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> result = new ArrayList<>();
        List<IVariableWeight> weights
            = CachedWeightFactory.createAllCombinations(params.getVarModel(), params.getSdContainer());
        
        for (Class<? extends AbstractFunctionMetric<?>> metricClass : SUPPORTED_METRICS) {
            for (IVariableWeight weight : weights) {
                params.setWeight(weight);
                result.addAll(createAllVariations(params, metricClass));
            }
        }
        
        return result;
    }
    
    /**
     * Creates all valid variations of the given code function metric, each variation will appear only once.
     * 
     * @param metricClass The metric class to create all variations for.
     * @param params The parameters for creating metrics.
     * 
     * @return All valid variations of the given code function metric.
     * @throws SetUpException In case that at least one metric instance throws a SetUpException.
     */
    public static @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllVariations(
            @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass, @NonNull MetricCreationParameters params)
            throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> result = new ArrayList<>();
        
        if (params.isSingleMetricExecution()) {
            List<IVariableWeight> weights
                = CachedWeightFactory.createVariabilityWeight(params.getVarModel(), params.getSdContainer(), params);
            for (IVariableWeight weight : weights) {
                params.setWeight(weight);
                AbstractFunctionMetric<?> metric = createMetric(params, metricClass);
                if (null != metric) {
                    result.add(metric);
                }
            }
        } else {
            List<IVariableWeight> weights =
                CachedWeightFactory.createAllCombinations(params.getVarModel(), params.getSdContainer());
            for (IVariableWeight weight : weights) {
                params.setWeight(weight);
                result.addAll(createAllVariations(params, metricClass));
            }
        }
        
        
        return result;
        
    }
    
    public static @Nullable Object configureAndReadMetricSpecificSetting(Configuration config,
        @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) {
        
        Object result = null;
        Setting<?> setting = METRIC_SPECIFIC_SETTINGS.get(metricClass);
        if (null != setting) {
            try {
                config.registerSetting(setting);
                result = config.getValue(setting);
            } catch (SetUpException exc) {
                Logger.get().logException("Could not load metric-specific setting for metric: " + metricClass.getName(),
                    exc);
            }
        }
        
        return result;
    }
}
