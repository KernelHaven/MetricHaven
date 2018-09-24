package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.CachedWeightFactory;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
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
        
        public MetricCreationParameters(@NonNull VariabilityModel varModel, @NonNull BuildModel buildModel,
                @NonNull ScatteringDegreeContainer sdContainer) {
            this.varModel = varModel;
            this.buildModel = buildModel;
            this.sdContainer = sdContainer;
        }
        
        public @NonNull VariabilityModel getVarModel() {
            return varModel;
        }
        
        public @NonNull BuildModel getBuildModel() {
            return buildModel;
        }
        
        public @NonNull ScatteringDegreeContainer getSdContainer() {
            return sdContainer;
        }
        
        public void setWeight(IVariableWeight weight) {
            this.weight = weight;
        }
        
        public IVariableWeight getWeight() {
            return weight;
        }
        
        public void setMetricSpecificSettingValue(Object metricSpecificSettingValue) {
            this.metricSpecificSettingValue = metricSpecificSettingValue;
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getMetricSpecificSettingValue(Class<T> type) throws SetUpException {
            if (metricSpecificSettingValue == null || !type.isAssignableFrom(metricSpecificSettingValue.getClass())) {
                throw new SetUpException("Invalid metric specific type; expected " + type.getName() + " but got "
                        + (metricSpecificSettingValue == null ? "null" : metricSpecificSettingValue.getClass().getName()));
            }
            return (T) metricSpecificSettingValue;
        }
        
        public void setFunctionMap(FunctionMap functionMap) {
            this.functionMap = functionMap;
        }
        
        public FunctionMap getFunctionMap() {
            return functionMap;
        }
        
    }
    
    private static final List<@NonNull Class<? extends AbstractFunctionMetric<?>>> SUPPORTED_METRICS;
    
    static {
        List<@NonNull Class<? extends AbstractFunctionMetric<?>>> tmpList = new ArrayList<>();
        tmpList.add(DLoC.class);
        tmpList.add(VariablesPerFunctionMetric.class);
        tmpList.add(NestingDepth.class);
        tmpList.add(BlocksPerFunctionMetric.class);
        tmpList.add(TanglingDegree.class);
        
        SUPPORTED_METRICS = Collections.unmodifiableList(tmpList);
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
     * @param vm The variability model
     * @param bm The build model.
     * @param weight The weight to use.
     * @param individualSettingValue A enumeration literal defined by the metric class, may be <tt>null</tt>.
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
     * @param vm The variability model
     * @param bm The build model.
     * @param metricClass The metric to instantiate.
     * @param weight The weight to use.
     *
     * @return A list of all valid  metric combinations.
     * @throws SetUpException In case that at least one metric instance throws a SetUpException.
     */
    private static @NonNull List<@NonNull AbstractFunctionMetric<?>>
        createAllVariations(@NonNull MetricCreationParameters params,
        @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass) throws SetUpException {
        
        @NonNull List<@NonNull AbstractFunctionMetric<?>> result = new LinkedList<>();
        List<Class<?>> enumSettings = getSettings(metricClass);
        
        try {
            // Search for first constructor which is annotated with @PreferedConstructor
            Constructor<?> constructor = null;
            for (Constructor<?> constr : metricClass.getDeclaredConstructors()) {
                for (Annotation annotation : constr.getDeclaredAnnotations()) {
                    if (annotation instanceof PreferedConstructor) {
                        constructor = constr;
                        break;
                    }
                }
                if (null != constructor) {
                    break;
                }
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
    
}
