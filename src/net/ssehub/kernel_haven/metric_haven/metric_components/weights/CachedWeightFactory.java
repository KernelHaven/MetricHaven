package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSize;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureSizeType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;

/**
 * Creates {@link IVariableWeight}s and uses the {@link WeigthsCache} to reuse instances if possible.
 * @author El-Sharkawy
 *
 */
public class CachedWeightFactory {
    
    /**
     * Creates a {@link ScatteringWeight} with the given {@link ScatteringDegreeContainer}.
     * @param sdType Defines what kind of scattering shall be used.
     * @param sdContainer The scattering values to be used as weight, should not be <tt>null</tt>
     *     unless {@link SDType#NO_SCATTERING} is passed to <tt>sdType</tt>.
     * @return The {@link ScatteringWeight} or <tt>null</tt> in case that {@link SDType#NO_SCATTERING} was specified.
     * 
     * @throws SetUpException If scattering degree container is required, but null.
     */
    public static @Nullable ScatteringWeight createSdWeight(@NonNull SDType sdType,
        @Nullable ScatteringDegreeContainer sdContainer) throws SetUpException {
        
        // Uncached weight (doesn't make sense to cache this)
        return (sdType != SDType.NO_SCATTERING) ? new ScatteringWeight(sdContainer, sdType) : null;
    }
    
    /**
     * Creates a {@link CtcrWeight} with the given {@link VariabilityModel}.
     * @param ctcrType Defines what kind of cross-tree-constraints shall be used.
     * @param varModel A variability model that contains informations about cross-tree constraints
     *     ({@link Attribute#CONSTRAINT_USAGE}). Should not be <tt>null</tt>
     *     unless {@link CTCRType#NO_CTCR} is passed to <tt>ctcrType</tt>.
     * 
     * @return The {@link CtcrWeight} or <tt>null</tt> in case that {@link CTCRType#NO_CTCR} was specified.
     * 
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public static @Nullable CtcrWeight createCtrcWeight(@NonNull CTCRType ctcrType,
        @Nullable VariabilityModel varModel) throws SetUpException {
        
        CtcrWeight weight = null;
        if (ctcrType != CTCRType.NO_CTCR && null != varModel) {
            weight = (CtcrWeight) WeigthsCache.INSTANCE.getWeight(ctcrType);
            if (null == weight) {
                weight = new CtcrWeight(varModel, ctcrType);
                WeigthsCache.INSTANCE.add(ctcrType, weight);
            }
        }
        return weight;
    }
    
    /**
     * Creates a {@link FeatureDistanceWeight} with the given {@link VariabilityModel}.
     * @param distanceType Defines what kind of feature distances shall be used.
     * @param varModel A variability model that contains locations where features a defined in the file system
     *     ({@link Attribute#SOURCE_LOCATIONS}). Should not be <tt>null</tt>
     *     unless {@link FeatureDistanceType#NO_DISTANCE} is passed to <tt>ctcrType</tt>.
     * 
     * @return The {@link FeatureDistanceWeight} or <tt>null</tt> in case that
     *     {@link SDType#NO_SCATTERING} was specified.
     *     
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public static @Nullable FeatureDistanceWeight createFeatureDistanceWeight(@NonNull FeatureDistanceType distanceType,
        @Nullable VariabilityModel varModel) throws SetUpException {
        
        FeatureDistanceWeight weight = null;
        if (distanceType != FeatureDistanceType.NO_DISTANCE && null != varModel) {
            weight = (FeatureDistanceWeight) WeigthsCache.INSTANCE.getWeight(distanceType);
            if (null == weight) {
                weight = new FeatureDistanceWeight(varModel);
                WeigthsCache.INSTANCE.add(distanceType, weight);
            }
        }
        
        return weight;
    }
    
    /**
     * Creates a {@link TypeWeight} with the given {@link VariabilityModel} and specified <tt>typeWeights</tt>.
     * @param varTypeWeightType Defines how to weight types.
     * @param varModel The variability model to use, should not be <tt>null</tt>
     *     unless {@link VariabilityTypeMeasureType#NO_TYPE_MEASURING} is passed to <tt>varTypeWeightType</tt>.
     * @param typeWeights A 2-tuple specifying weight values for each type used in the variability model.
     * 
     * @return The {@link TypeWeight} or <tt>null</tt> in case that {@link VariabilityTypeMeasureType#NO_TYPE_MEASURING}
     *     was specified.
     * 
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public static @Nullable TypeWeight createTypeWeight(@NonNull VariabilityTypeMeasureType varTypeWeightType,
        @Nullable VariabilityModel varModel, @Nullable Map<String, Integer> typeWeights) throws SetUpException {
        
        TypeWeight weight = null;
        if (varTypeWeightType != VariabilityTypeMeasureType.NO_TYPE_MEASURING && null != typeWeights) {
            
            if (varModel == null) {
                throw new SetUpException("TypeWeight requires a VariabilityModel");
            }
            
            weight = (TypeWeight) WeigthsCache.INSTANCE.getWeight(varTypeWeightType);
            if (null == weight) {
                weight = new TypeWeight(varModel, typeWeights);
                WeigthsCache.INSTANCE.add(varTypeWeightType, weight);
            }
        }
        
        return weight;
    }
    
    /**
     * Creates a {@link HierarchyWeight} with the given {@link VariabilityModel} and specified
     * <tt>hierarchyWeights</tt>.
     * @param varHierarchyWeightType Defines how to weight hierarchies.
     * @param varModel A variability model that contains information about feature hierarchies
     *     ({@link Attribute#HIERARCHICAL}). Should not be <tt>null</tt>
     *     unless {@link HierarchyType#NO_HIERARCHY_MEASURING} is passed to <tt>varHierarchyWeightType</tt>.
     * @param hierarchyWeights A 2-tuple specifying weight values for each hierarchy type used in the variability model.
     *      This must contain hierarchy values for <tt>top</tt>, <tt>intermediate</tt>, and <tt>leaf</tt>.
     * 
     * @return The {@link HierarchyWeight} or <tt>null</tt> in case that
     *     {@link HierarchyType#NO_HIERARCHY_MEASURING} was specified.
     *     
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public static @Nullable HierarchyWeight createHierarchyWeight(@NonNull HierarchyType varHierarchyWeightType,
        @Nullable VariabilityModel varModel, @Nullable Map<String, Integer> hierarchyWeights) throws SetUpException {
        
        // Weights for hierarchy level of a feature
        HierarchyWeight weight = null;
        if (varHierarchyWeightType != HierarchyType.NO_HIERARCHY_MEASURING) {
            weight = (HierarchyWeight) WeigthsCache.INSTANCE.getWeight(varHierarchyWeightType);
            if (null == weight) {
                if (null != varModel) {
                    if (varModel.getDescriptor().hasAttribute(Attribute.HIERARCHICAL)) {
                        weight = new HierarchyWeight(varModel,
                                (varHierarchyWeightType == HierarchyType.HIERARCHY_WEIGHTS_BY_LEVEL
                                        ? null : hierarchyWeights));
                        WeigthsCache.INSTANCE.add(varHierarchyWeightType, weight);
                    } else {
                        throw new SetUpException("Hierarchy of features should be measured \""
                            + MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING + "=" + varHierarchyWeightType.name()
                            + "\", but the variability model does not store hierarchy information.");
                    }
                } else {
                    throw new SetUpException("Hierarchy of features should be measured \""
                        + MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING + "=" + varHierarchyWeightType.name()
                        + "\", but no variability model was passed to the analysis.");
                }
            }
        }
        
        return weight;
    }
    
    /**
     * Creates a {@link StructuralWeight} with the specified {@link VariabilityModel}.
     * @param structuralWeightType Defines which structures shall be used as weight.
     * @param varModel The variability model to use.
     * 
     * @return The {@link StructuralWeight} or <tt>null</tt> in case that
     *     {@link StructuralType#NO_STRUCTURAL_MEASUREMENT} was specified.
     * 
     * @throws SetUpException If the var model does not fit the configuration.
     */
    public static @Nullable StructuralWeight createStructuralWeight(@NonNull StructuralType structuralWeightType,
        @Nullable VariabilityModel varModel) throws SetUpException {
            
        // Weights for the structure of the variability model
        StructuralWeight weight = null;
        if (structuralWeightType != StructuralType.NO_STRUCTURAL_MEASUREMENT) {
            if (varModel == null || !varModel.getDescriptor().hasAttribute(Attribute.HIERARCHICAL)) {
                throw new SetUpException("StructuralWeight requires a VariabilityModel with hierarchy information");
            }
            
            weight = (StructuralWeight) WeigthsCache.INSTANCE.getWeight(structuralWeightType);
            if (null == weight) {
                weight = new StructuralWeight(varModel, structuralWeightType);
                WeigthsCache.INSTANCE.add(structuralWeightType, weight);
            }
        }
        
        return weight;
    }

    /**
     * Creates a new {@link FeatureSizeWeight} with the already computed {@link FeatureSizeContainer}.
     * @param type Specifies whether and how to use {@link FeatureSize}s.
     * @param sizeContainer The already computed feature sizes.
     * @return The {@link FeatureSizeWeight} or <tt>null</tt> if {@link FeatureSizeType#NO_FEATURE_SIZES} was specified.
     * @throws SetUpException If {@link FeatureSizeContainer} is <tt>null</tt> but a feature size weight was specified
     */
    public static @Nullable FeatureSizeWeight createFeatureSizeWeight(@NonNull FeatureSizeType type,
        @Nullable FeatureSizeContainer sizeContainer) throws SetUpException {
        
        FeatureSizeWeight weight = null;
        if (type != FeatureSizeType.NO_FEATURE_SIZES && null == sizeContainer) {
            throw new SetUpException("Feature Size weight specified (" + type.name() + ") but no FeatureSizes "
                    + "computed before.");
        } else if (type != FeatureSizeType.NO_FEATURE_SIZES) {
            // Caching not necessary as all values are already computed.
            weight = new FeatureSizeWeight(NullHelpers.notNull(sizeContainer), type);
        }
        
        return weight;
    }
    
    /**
     * Creates a distinct list of all valid {@link IVariableWeight} combinations. Requires type and hierarchy weight
     * definitions.
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * @param fsContainer A feature size container, which shall be used for {@link FeatureSizeWeight}.
     * @param typeWeights A 2-tuple in the form of (variable type; weight value).
     * @param hierarchyWeights A 2-tuple in the form of (hierarchy; weight value). This must contain hierarchy values
     *     for <tt>top</tt>, <tt>intermediate</tt>, and <tt>leaf</tt>.
     * 
     * @return A distinct list of all valid {@link IVariableWeight} combinations,
     *     contains also {@link NoWeight#INSTANCE}.
     *     
     * @throws SetUpException If there is a misconfiguartion.
     */
    public static @NonNull List<@NonNull IVariableWeight> createAllCombinations(@Nullable VariabilityModel varModel,
        @Nullable ScatteringDegreeContainer sdContainer, @Nullable FeatureSizeContainer fsContainer,
        @NonNull Map<String, Integer> typeWeights, @NonNull Map<String, Integer> hierarchyWeights)
        throws SetUpException {
        
        List<@NonNull IVariableWeight> weightCombinations = new ArrayList<>();
        List<@NonNull IVariableWeight> tmpList = new ArrayList<>();
        
        // Iterate through all existing combinations
        for (SDType sdValue : SDType.values()) {
            for (CTCRType ctcrValue : CTCRType.values()) {
                for (FeatureDistanceType distanceValue : FeatureDistanceType.values()) {
                    for (VariabilityTypeMeasureType varTypeValue : VariabilityTypeMeasureType.values()) {
                        for (HierarchyType hierarhcyValue : HierarchyType.values()) {
                            for (StructuralType structureValue : StructuralType.values()) {
                                for (FeatureSizeType featureSizeValue : FeatureSizeType.values()) {
                                    
                                    // Create the weight combination
                                    weightCombinations.add(createVariabilityWeight(tmpList, varModel, sdContainer,
                                        fsContainer, sdValue, ctcrValue, distanceValue, varTypeValue, hierarhcyValue,
                                        structureValue, featureSizeValue, typeWeights, hierarchyWeights));
                                    
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return weightCombinations;
    }
    
    /**
     * Creates a (compound) {@link IVariableWeight} based on the passed enumeration values. <tt>null</tt> elements will
     * be treated as deselection.
     * @param tmpList An empty list which will be used to aggregate {@link IVariableWeight}. This list will be cleared
     *     as a side effect. This allows to re-use allocated resources.
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * @param sdValue Defines what kind of scattering shall be used.
     * @param ctcrValue Defines what kind of cross-tree-constraints shall be used.
     * @param distanceValue Defines what kind of feature distances shall be used.
     * @param varTypeValue Defines how to weight types.
     * @param hierarhcyValue Defines how to weight hierarchies.
     * @param structureValue Defines which structures shall be used as weight.
     * @param typeWeights A 2-tuple specifying weight values for each type used in the variability model.
     * @param hierarchyWeights A 2-tuple in the form of (hierarchy; weight value). This must contain hierarchy values
     *     for <tt>top</tt>, <tt>intermediate</tt>, and <tt>leaf</tt>.
     * 
     * @return A variability weight instance either, maybe {@link NoWeight#INSTANCE} if no weight was specified, or
     *     {@link MultiWeight} if multiple individual weights are selected.    
     * 
     * @throws SetUpException If there is a misconfiguartion.
     */
    // CHECKSTYLE:OFF
    private static @NonNull IVariableWeight createVariabilityWeight(List<@NonNull IVariableWeight> tmpList,
        @Nullable VariabilityModel varModel, @Nullable ScatteringDegreeContainer sdContainer,
        @Nullable FeatureSizeContainer fsContainer,
        
        // Specification of weights
        @Nullable SDType sdValue,
        @Nullable CTCRType ctcrValue,
        @Nullable FeatureDistanceType distanceValue,
        @Nullable VariabilityTypeMeasureType varTypeValue,
        @Nullable HierarchyType hierarhcyValue,
        @Nullable StructuralType structureValue,
        @Nullable FeatureSizeType fsType,
        
        // Value configuration of weights
        @Nullable Map<String, Integer> typeWeights,
        @Nullable Map<String, Integer> hierarchyWeights)
    
        throws SetUpException {
    // CHECKSTYLE:ON
        
        
        IVariableWeight tmpWeight = null;
        
        // Scattering Degree
        if (null != sdValue) {
            tmpWeight = createSdWeight(sdValue, sdContainer);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Cross-Tree Constraint Ratio
        if (null != ctcrValue) {
            tmpWeight = createCtrcWeight(ctcrValue, varModel);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Feature Distance
        if (null != distanceValue) {
            tmpWeight = createFeatureDistanceWeight(distanceValue, varModel);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Variability Types
        if (null != varTypeValue) {
            tmpWeight = createTypeWeight(varTypeValue, varModel, typeWeights);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Hierarchy Types
        if (null != hierarhcyValue) {
            tmpWeight = createHierarchyWeight(hierarhcyValue, varModel, hierarchyWeights);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Structure Types
        if (null != structureValue) {
            tmpWeight = createStructuralWeight(structureValue, varModel);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Feature Sizes
        if (null != fsType) {
            tmpWeight = createFeatureSizeWeight(fsType, fsContainer);
            if (null != tmpWeight) {
                tmpList.add(tmpWeight);
            }
        }
        
        // Smart aggregation
        if (tmpList.size() == 1) {
            tmpWeight = NullHelpers.notNull(tmpList.get(0));
        } else if (tmpList.size() > 1) {
            tmpWeight = new MultiWeight(tmpList);
        } else {
            tmpWeight = NoWeight.INSTANCE;
        }

        tmpList.clear();
        return tmpWeight;
    }
    
    /**
     * Creates a distinct list of all valid {@link IVariableWeight} combinations. Specifies default type and hierarchy
     * values, which may be used for analyses of Linux. May also be used if a specifics weight is desired (if
     * {@link MetricCreationParameters#isSingleMetricExecution()} is selected).
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * @param fsContainer A feature size container, which shall be used for {@link FeatureSizeWeight}.
     * @param params Specifies whether a single {@link IVariableWeight} shall be created or all valid combinations.
     * @return A distinct list of all valid {@link IVariableWeight} combinations,
     *     contains also {@link NoWeight#INSTANCE}.
     * 
     * @throws SetUpException If there is a misconfiguartion.
     */
    public static @NonNull List<@NonNull IVariableWeight> createVariabilityWeight(@Nullable VariabilityModel varModel,
        @Nullable ScatteringDegreeContainer sdContainer, @Nullable FeatureSizeContainer fsContainer,
        @NonNull MetricCreationParameters params) throws SetUpException {
        
        @NonNull List<@NonNull IVariableWeight> result;
        if (params.isSingleMetricExecution()) {
            result = new ArrayList<>();
            result.add(createVariabilityWeight(new ArrayList<>(7), varModel, sdContainer, fsContainer,
                
                params.getScatteringDegree(),
                params.getCTCR(),
                params.getDistance(),
                params.getFeatureTypes(),
                params.getHierarchyType(),
                params.getStructuralType(),
                params.getFeatureSizeType(),
                
                params.getTypeWeights(), params.getHierarchyWeights()));
        } else {
            result = createAllCombinations(varModel, sdContainer, fsContainer);
        }
        
        return result;
    }
    
    /**
     * Creates a distinct list of all valid {@link IVariableWeight} combinations. Specifies default type and hierarchy
     * values, which may be used for analyses of Linux.
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * @param fsContainer A feature size container, which shall be used for {@link FeatureSizeWeight}.
     * 
     * @return A distinct list of all valid {@link IVariableWeight} combinations,
     *     contains also {@link NoWeight#INSTANCE}.
     * 
     * @throws SetUpException If there is a misconfiguartion.
     */
    public static @NonNull List<@NonNull IVariableWeight> createAllCombinations(@Nullable VariabilityModel varModel,
        @Nullable ScatteringDegreeContainer sdContainer, @Nullable FeatureSizeContainer fsContainer)
        throws SetUpException {
        
        Map<String, Integer> typeWeights = new HashMap<>();
        typeWeights.put("bool", 1);
        typeWeights.put("tristate", 10);
        typeWeights.put("string", 100);
        typeWeights.put("int", 100);
        typeWeights.put("integer", 100);
        typeWeights.put("hex", 100);
        
        Map<String, Integer> hierarchyWeights = new HashMap<>();
        hierarchyWeights.put("top", 1);
        hierarchyWeights.put("intermediate", 10);
        hierarchyWeights.put("leaf", 100);
   
        return createAllCombinations(varModel, sdContainer, fsContainer, typeWeights, hierarchyWeights);
    }
}
