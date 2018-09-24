package net.ssehub.kernel_haven.metric_haven.metric_components.weights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.util.Logger;
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
     */
    public static @Nullable ScatteringWeight createSdWeight(@NonNull SDType sdType,
        @Nullable ScatteringDegreeContainer sdContainer) {
        
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
     */
    public static @Nullable CtcrWeight createCtrcWeight(@NonNull CTCRType ctcrType,
        @Nullable VariabilityModel varModel) {
        
        // Uncached weight (doesn't make sense to cache this)
        return (ctcrType != CTCRType.NO_CTCR && null != varModel) ? new CtcrWeight(varModel, ctcrType) : null;
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
     */
    public static @Nullable FeatureDistanceWeight createFeatureDistanceWeight(@NonNull FeatureDistanceType distanceType,
        @Nullable VariabilityModel varModel) {
        
        // Uncachable weight
        return (distanceType != FeatureDistanceType.NO_DISTANCE && null != varModel)
            ? new FeatureDistanceWeight(varModel) : null;
    }
    
    public static @Nullable TypeWeight createTypeWeight(@NonNull VariabilityTypeMeasureType varTypeWeightType,
       @Nullable VariabilityModel varModel, @Nullable Map<String, Integer> typeWeights) {
        
        TypeWeight weight = null;
        if (varTypeWeightType != VariabilityTypeMeasureType.NO_TYPE_MEASURING && null != varModel
            && null != typeWeights) {
            
            weight = (TypeWeight) WeigthsCache.INSTANCE.getWeight(varTypeWeightType);
            if (null == weight) {
                weight = new TypeWeight(varModel, typeWeights);
                WeigthsCache.INSTANCE.add(varTypeWeightType, weight);
            }
        }
        
        return weight;
    }
    
    public static @Nullable HierarchyWeight createHierarchyWeight(@NonNull HierarchyType varHierarchyWeightType,
        @Nullable VariabilityModel varModel, @Nullable Map<String, Integer> hierarchyWeights) {
        
        // Weights for hierarchy level of a feature
        HierarchyWeight weight = null;
        if (varHierarchyWeightType != HierarchyType.NO_HIERARCHY_MEASURING) {
            weight = (HierarchyWeight) WeigthsCache.INSTANCE.getWeight(varHierarchyWeightType);
            if (null == weight) {
                if (null != varModel) {
                    if (varModel.getDescriptor().hasAttribute(Attribute.HIERARCHICAL)) {
                        weight = new HierarchyWeight(varModel, hierarchyWeights);
                        WeigthsCache.INSTANCE.add(varHierarchyWeightType, weight);
                    } else {
                        Logger.get().logError2("Hierarchy of features should be measured \"",
                            MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING, "=" + varHierarchyWeightType.name(),
                            "\", but the variability model does not store hierarchy information.");
                    }
                } else {
                    Logger.get().logError2("Hierarchy of features should be measured \"",
                        MetricSettings.HIERARCHY_TYPE_MEASURING_SETTING, "=" + varHierarchyWeightType.name(),
                        "\", but no variability model was passed to the analysis.");
                }
            }
        }
        
        return weight;
    }
    
    public static @Nullable StructuralWeight createStructuralWeight(@NonNull StructuralType structuralWeightType,
        @Nullable VariabilityModel varModel) {
            
        // Weights for the structure of the variability model
        StructuralWeight weight = null;
        if (structuralWeightType != StructuralType.NO_STRUCTURAL_MEASUREMENT  && null != varModel) {
            weight = (StructuralWeight) WeigthsCache.INSTANCE.getWeight(structuralWeightType);
            if (null == weight) {
                weight = new StructuralWeight(varModel, structuralWeightType);
                WeigthsCache.INSTANCE.add(structuralWeightType, weight);
            }
        }
        
        return weight;
    }
    
    /**
     * Creates a distinct list of all valid {@link IVariableWeight} combinations. Requires type and hierarchy weight
     * definitions.
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * @param typeWeights A 2-tuple in the form of (variable type; weight value).
     * @param hierarchyWeights A 2-tuple in the form of (hierarchy; weight value). This must contain hierarchy values
     *     for <tt>top</tt>, <tt>intermediate</tt>, and <tt>leaf</tt>.
     * 
     * @return A distinct list of all valid {@link IVariableWeight} combinations,
     *     contains also {@link NoWeight#INSTANCE}.
     */
    public static @NonNull List<@NonNull IVariableWeight> createAllCombinations(@NonNull VariabilityModel varModel,
        @NonNull ScatteringDegreeContainer sdContainer, @NonNull Map<String, Integer> typeWeights,
        @NonNull Map<String, Integer> hierarchyWeights) {
        
        List<@NonNull IVariableWeight> weightCombinations = new ArrayList<>();
        List<@NonNull IVariableWeight> tmpList = new ArrayList<>();
        
        for (SDType sdValue : SDType.values()) {
            for (CTCRType ctcrValue : CTCRType.values()) {
                for (FeatureDistanceType distanceValue : FeatureDistanceType.values()) {
                    for (VariabilityTypeMeasureType varTypeValue : VariabilityTypeMeasureType.values()) {
                        for (HierarchyType hierarhcyValue : HierarchyType.values()) {
                            for (StructuralType structureValue : StructuralType.values()) {
                                // Scattering Degree
                                IVariableWeight tmpWeight = createSdWeight(NullHelpers.notNull(sdValue), sdContainer);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Cross-Tree Constraint Ratio
                                tmpWeight = createCtrcWeight(NullHelpers.notNull(ctcrValue), varModel);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Feature Distance
                                tmpWeight = createFeatureDistanceWeight(NullHelpers.notNull(distanceValue), varModel);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Variability Types
                                tmpWeight = createTypeWeight(NullHelpers.notNull(varTypeValue), varModel, typeWeights);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Hierarchy Types
                                tmpWeight = createHierarchyWeight(NullHelpers.notNull(hierarhcyValue),
                                    varModel, hierarchyWeights);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Structure Types
                                tmpWeight = createStructuralWeight(NullHelpers.notNull(structureValue), varModel);
                                if (null != tmpWeight) {
                                    tmpList.add(tmpWeight);
                                }
                                
                                // Smart aggregation
                                if (tmpList.size() == 1) {
                                    weightCombinations.add(tmpList.get(0));
                                } else if (tmpList.size() > 1) {
                                    weightCombinations.add(new MultiWeight(tmpList));
                                } else {
                                    weightCombinations.add(NoWeight.INSTANCE);
                                }
                                tmpList.clear();
                            }
                        }
                    }
                }
            }
        }
        
        return weightCombinations;
    }
    
    /**
     * Creates a distinct list of all valid {@link IVariableWeight} combinations. Specifies default type and hierarchy
     * values, which may be used for analyses of Linux.
     * @param varModel The variability model.
     * @param sdContainer A scattering degree container, which shall be used for {@link ScatteringWeight}.
     * 
     * @return A distinct list of all valid {@link IVariableWeight} combinations,
     *     contains also {@link NoWeight#INSTANCE}.
     */
    public static @NonNull List<@NonNull IVariableWeight> createAllCombinations(@NonNull VariabilityModel varModel,
        @NonNull ScatteringDegreeContainer sdContainer) {
        
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
   
        return createAllCombinations(varModel, sdContainer, typeWeights, hierarchyWeights);
    }
}
