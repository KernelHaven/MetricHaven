/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.code_metrics.AbstractFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.BlocksPerFunctionMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.BlocksPerFunctionMetric.BlockMeasureType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.CyclomaticComplexity;
import net.ssehub.kernel_haven.metric_haven.code_metrics.CyclomaticComplexity.CCType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC;
import net.ssehub.kernel_haven.metric_haven.code_metrics.EigenVectorCentrality;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut.FanType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.code_metrics.NestingDepth;
import net.ssehub.kernel_haven.metric_haven.code_metrics.NestingDepth.NDType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.TanglingDegree;
import net.ssehub.kernel_haven.metric_haven.code_metrics.TanglingDegree.TDType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.UndisciplinedPreprocessorUsage;
import net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction;
import net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction.VarType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.feature_size.FeatureSizeContainer;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureSizeType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.HierarchyType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.StructuralType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.FeatureSizeWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.WeigthsCache;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Hard coded list of metrics to run.
 *
 * @author Sascha El-Sharkawy
 * @author Adam
 */
public class IndividualCodeMetricsRunner extends CodeMetricsRunner {

    /**
     * Specifies a metric selection without any weights, used for
     * {@link IndividualCodeMetricsRunner#createAllAtomicVariations(MetricCreationParameters)}.
     */
    private static class MetricSelection {
        
        private @NonNull Class<? extends AbstractFunctionMetric<?>> metricClass;
        private @Nullable Object individualSetting;
        private boolean isWeightable;
        
        /**
         * Specifies a metric selection without any weight.
         * @param metricClass The metric to execute.
         * @param individualSetting The version of the metric to execute, <tt>null</tt> if the metric does not specify
         *     individual settings.
         * @param isWeightable <tt>true</tt> if it may be combined with a {@link IVariableWeight}.
         */
        private MetricSelection(@NonNull Class<? extends AbstractFunctionMetric<?>> metricClass,
            @Nullable Object individualSetting, boolean isWeightable) {
            
            this.metricClass = metricClass;
            this.individualSetting = individualSetting;
            this.isWeightable = isWeightable;
        }
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent) throws SetUpException {
        
        super(config, codeFunctionComponent, null, null, null, null);
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        super(config, codeFunctionComponent, varModelComponent, null, null, null);
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @NonNull AnalysisComponent<VariabilityModel> varModelComponent,
        @NonNull AnalysisComponent<BuildModel> bmComponent) throws SetUpException {
        
        super(config, codeFunctionComponent, varModelComponent, bmComponent, null, null);
    }
    
    /**
     * Creates this processing unit.
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * @param sdComponent Scattering degree values, used to create the {@link ScatteringWeight}.
     * @param fmComponent {@link FunctionMap}, required to run {@link FanInOut}-metrics.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    //CHECKSTYLE:OFF // More than 5 parameters
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
    //CHECKSTYLE:ON
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent,
        @Nullable AnalysisComponent<FunctionMap> fmComponent) throws SetUpException {
        
        super(config, codeFunctionComponent, varModelComponent, bmComponent, sdComponent, fmComponent, null);
    }
    
    /**
     * Creates this processing unit (default constructor for {@link IndividualCodeMetricsRunner} to run all variations).
     * 
     * @param config The pipeline configuration.
     * @param codeFunctionComponent The component to get the {@link CodeFunction}s to run the metrics on.
     * @param varModelComponent The variability model, to filter for VPs and to create {@link IVariableWeight}s.
     * @param bmComponent The build model, used by the {@link VariablesPerFunctionMetric}.
     * @param sdComponent Scattering degree values, used to create the {@link ScatteringWeight}.
     * @param fmComponent {@link FunctionMap}, required to run {@link FanInOut}-metrics.
     * @param fsComponent {@link FeatureSizeContainer}, required to create the {@link FeatureSizeWeight}.
     * 
     * @throws SetUpException If creating the metric instances fails.
     */
    //CHECKSTYLE:OFF // More than 5 parameters
    public IndividualCodeMetricsRunner(@NonNull Configuration config,
    //CHECKSTYLE:ON
        @NonNull AnalysisComponent<CodeFunction> codeFunctionComponent,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent,
        @Nullable AnalysisComponent<FunctionMap> fmComponent,
        @Nullable AnalysisComponent<FeatureSizeContainer> fsComponent) throws SetUpException {
        
        super(config, codeFunctionComponent, varModelComponent, bmComponent, sdComponent, fmComponent, fsComponent);
    }
    
    /**
     * Creates a vector space basis, where always exactly one weight is set to 1 and all others to 0.  
     * @return The vector basis for creating elementary <b>hierarchy</b> weights.
     */
    private @NonNull List<@NonNull Map<String, Integer>> createHierarchyWeightVectorSpace() {
        @NonNull List<@NonNull Map<String, Integer>> result = new ArrayList<>();
        
        @NonNull Map<String, Integer> topHierarchyWeights = new HashMap<>();
        topHierarchyWeights.put("top", 1);
        topHierarchyWeights.put("intermediate", 0);
        topHierarchyWeights.put("leaf", 0);
        result.add(topHierarchyWeights);
        
        @NonNull Map<String, Integer> intermediateHierarchyWeights = new HashMap<>();
        intermediateHierarchyWeights.put("top", 0);
        intermediateHierarchyWeights.put("intermediate", 1);
        intermediateHierarchyWeights.put("leaf", 0);
        result.add(intermediateHierarchyWeights);
        
        @NonNull Map<String, Integer> leafHierarchyWeights = new HashMap<>();
        leafHierarchyWeights.put("top", 0);
        leafHierarchyWeights.put("intermediate", 0);
        leafHierarchyWeights.put("leaf", 1);
        result.add(leafHierarchyWeights);
        
        return result;
    }
    
    /**
     * Creates a vector space basis, where always exactly one weight is set to 1 and all others to 0.  
     * @return The vector basis for creating elementary <b>feature type</b> weights.
     */
    private @NonNull List<@NonNull Map<String, Integer>> createFeatureTypeVectorSpace() {
        @NonNull List<@NonNull Map<String, Integer>> result = new ArrayList<>();
        
        Map<String, Integer> boolWeights = new HashMap<>();
        boolWeights.put("bool", 1);
        boolWeights.put("tristate", 0);
        boolWeights.put("string", 0);
        boolWeights.put("int", 0);
        boolWeights.put("integer", 0);
        boolWeights.put("hex", 0);
        result.add(boolWeights);
        
        Map<String, Integer> tristateWeights = new HashMap<>();
        tristateWeights.put("bool", 0);
        tristateWeights.put("tristate", 1);
        tristateWeights.put("string", 0);
        tristateWeights.put("int", 0);
        tristateWeights.put("integer", 0);
        tristateWeights.put("hex", 0);
        result.add(tristateWeights);
        
        Map<String, Integer> stringWeights = new HashMap<>();
        stringWeights.put("bool", 0);
        stringWeights.put("tristate", 0);
        stringWeights.put("string", 1);
        stringWeights.put("int", 0);
        stringWeights.put("integer", 0);
        stringWeights.put("hex", 0);
        result.add(stringWeights);
        
        Map<String, Integer> intWeights = new HashMap<>();
        intWeights.put("bool", 0);
        intWeights.put("tristate", 0);
        intWeights.put("string", 0);
        intWeights.put("int", 1);
        intWeights.put("integer", 1);
        intWeights.put("hex", 0);
        result.add(intWeights);
        
        Map<String, Integer> hexWeights = new HashMap<>();
        hexWeights.put("bool", 0);
        hexWeights.put("tristate", 0);
        hexWeights.put("string", 0);
        hexWeights.put("int", 0);
        hexWeights.put("integer", 0);
        hexWeights.put("hex", 1);
        result.add(hexWeights);
        
        return result;
    }
    
    /**
     * Logs the currently created metrics.
     * @param metrics The list of currently created metric instances.
     */
    private void logCreatedMetrics(List<@NonNull AbstractFunctionMetric<?>> metrics) {
        StringBuffer tmpMsg = new StringBuffer();
        tmpMsg.append(metrics.size());
        tmpMsg.append(" new Metrics created:");
        for (AbstractFunctionMetric<?> metric : metrics) {
            tmpMsg.append("\n - ");
            tmpMsg.append(metric.getResultName());
        }
        LOGGER.logInfo2(tmpMsg);
    }
    
    /**
     * Resets the MetricCreationParameters.
     * @param params The MetricCreationParameters to reset (as side effect).
     */
    private void cleanWeightSettings(MetricCreationParameters params) {
        params.setScatteringDegree(SDType.NO_SCATTERING);
        params.setCTCR(CTCRType.NO_CTCR);
        params.setDistance(FeatureDistanceType.NO_DISTANCE);
        params.setHierarchyType(HierarchyType.NO_HIERARCHY_MEASURING);
        params.setFeatureTypes(VariabilityTypeMeasureType.NO_TYPE_MEASURING);
        params.setStructuralType(StructuralType.NO_STRUCTURAL_MEASUREMENT);
        params.setFeatureSizeType(FeatureSizeType.NO_FEATURE_SIZES);
    }
    
    /**
     * Create all metric instances for metrics operating on variability and allowing weights, but does not
     * use more than one weight.
     * @param params The parameters for creating metrics.
     * 
     * @return The selected metric instances.
     * @throws SetUpException If creation of specified metrics is not possible (wrong set-up)
     */
    //CHECKSTYLE:OFF // Hard coded list of metrics requires more than 70 lines
    private @NonNull List<@NonNull AbstractFunctionMetric<?>> createAllAtomicVariations(MetricCreationParameters params)
        throws SetUpException {
    //CHECKSTYLE:ON
        
        List<@NonNull AbstractFunctionMetric<?>> metrics = new ArrayList<>();
        MetricSelection[] selectedMetrics = allAtomicMetrics();
        
        params.setSingleMetricExecution(true);
        @NonNull List<@NonNull Map<String, Integer>> hierarchyWeightVectorSpace = createHierarchyWeightVectorSpace();
        @NonNull List<@NonNull Map<String, Integer>> typeWeightVectorSpace = createFeatureTypeVectorSpace();
        for (MetricSelection metricSelection : selectedMetrics) {
            params.setMetricSpecificSettingValue(metricSelection.individualSetting);
            
            // Blank without any weights
            cleanWeightSettings(params);
            metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
            
            if (metricSelection.isWeightable) {
                // Scattering
                cleanWeightSettings(params);
                for (SDType sd : SDType.values()) {
                    if (sd != SDType.NO_SCATTERING) {
                        params.setScatteringDegree(sd);
                        WeigthsCache.INSTANCE.clear();
                        metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                    }
                }
                
                // CTCR
                cleanWeightSettings(params);
                for (CTCRType ctcr : CTCRType.values()) {
                    if (ctcr != CTCRType.NO_CTCR) {
                        params.setCTCR(ctcr);
                        WeigthsCache.INSTANCE.clear();
                        metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                    }
                }
                
                // Feature Distance
                cleanWeightSettings(params);
                for (FeatureDistanceType distance : FeatureDistanceType.values()) {
                    if (distance != FeatureDistanceType.NO_DISTANCE) {
                        params.setDistance(distance);
                        WeigthsCache.INSTANCE.clear();
                        metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                    }
                }
                
                // Hierarchy Types
                cleanWeightSettings(params);
                params.setHierarchyType(HierarchyType.HIERARCHY_WEIGHTS_BY_FILE);
                for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
                    params.setHierarchyWeights(hierarchyWeights);
                    WeigthsCache.INSTANCE.clear();
                    metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                } 
                cleanWeightSettings(params);
                params.setHierarchyType(HierarchyType.HIERARCHY_WEIGHTS_BY_LEVEL);
                WeigthsCache.INSTANCE.clear();
                metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                
                // Variable Types
                cleanWeightSettings(params);
                params.setFeatureTypes(VariabilityTypeMeasureType.TYPE_WEIGHTS_BY_FILE);
                for (@NonNull Map<String, Integer> typeWeights : typeWeightVectorSpace) {
                    params.setTypeWeights(typeWeights);
                    WeigthsCache.INSTANCE.clear();
                    metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                }
                
                // VarModel Structure
                cleanWeightSettings(params);
                for (StructuralType structure : StructuralType.values()) {
                    if (structure != StructuralType.NO_STRUCTURAL_MEASUREMENT) {
                        params.setStructuralType(structure);
                        WeigthsCache.INSTANCE.clear();
                        metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                    }
                }
                
                // Feature Size
                cleanWeightSettings(params);
                for (FeatureSizeType size : FeatureSizeType.values()) {
                    if (size != FeatureSizeType.NO_FEATURE_SIZES) {
                        params.setFeatureSizeType(size);
                        WeigthsCache.INSTANCE.clear();
                        metrics.addAll(MetricFactory.createAllVariations(metricSelection.metricClass, params));
                    }
                }
            }
        }
        
        return metrics;
    }

    /**
     * Creates a list of all (variability) metrics.
     * @return Variability metrics
     */
    //CHECKSTYLE:OFF // Hard coded list of metrics requires more than 70 lines
    private static MetricSelection[] allAtomicMetrics() {
        MetricSelection[] selectedMetrics = {
    //CHECKSTYLE:ON
            // VariablesPerFunction metric
            new MetricSelection(VariablesPerFunction.class, VarType.INTERNAL, true),
            new MetricSelection(VariablesPerFunction.class, VarType.EXTERNAL, true),
            new MetricSelection(VariablesPerFunction.class, VarType.ALL, true),
            new MetricSelection(VariablesPerFunction.class, VarType.EXTERNAL_WITH_BUILD_VARS, true),
            new MetricSelection(VariablesPerFunction.class, VarType.ALL_WITH_BUILD_VARS, true),
            
            // McCabe
            new MetricSelection(CyclomaticComplexity.class, CCType.VARIATION_POINTS, true),
            new MetricSelection(CyclomaticComplexity.class, CCType.ALL, true),
            
            // VP nesting
            new MetricSelection(NestingDepth.class, NDType.VP_ND_MAX, true),
            new MetricSelection(NestingDepth.class, NDType.VP_ND_AVG, true),
            new MetricSelection(NestingDepth.class, NDType.COMBINED_ND_MAX, true),
            new MetricSelection(NestingDepth.class, NDType.COMBINED_ND_AVG, true),
            
            // EigenVectorCentrality
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_IN_GLOBALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_IN_LOCALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_GLOBALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_LOCALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_NO_STUB_GLOBALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_NO_STUB_LOCALLY, true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_NO_EXTERNAL_VPS_GLOBALLY,
                true),
            new MetricSelection(EigenVectorCentrality.class, FanType.DEGREE_CENTRALITY_OUT_NO_EXTERNAL_VPS_LOCALLY,
                true),
            new MetricSelection(EigenVectorCentrality.class,
                FanType.DEGREE_CENTRALITY_OUT_NO_STUB_NO_EXTERNAL_VPS_LOCALLY, true),
            
            // Fan-In/Out
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_IN_GLOBALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_IN_LOCALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_GLOBALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_LOCALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_NO_STUB_GLOBALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_NO_STUB_LOCALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_NO_EXTERNAL_VPS_GLOBALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_NO_EXTERNAL_VPS_LOCALLY, true),
            new MetricSelection(FanInOut.class, FanType.DEGREE_CENTRALITY_OUT_NO_STUB_NO_EXTERNAL_VPS_LOCALLY, true),
            
            // Tangling Degree
            new MetricSelection(TanglingDegree.class, TDType.TD_ALL, true),
            new MetricSelection(TanglingDegree.class, TDType.TD_NO_ELSE, true),
            
            // Metrics which do not accept any variability metrics
            // Blocks per Function
            new MetricSelection(BlocksPerFunctionMetric.class, BlockMeasureType.BLOCK_AS_ONE, false),
            new MetricSelection(BlocksPerFunctionMetric.class, BlockMeasureType.SEPARATE_PARTIAL_BLOCKS, false),
            new MetricSelection(CyclomaticComplexity.class, CCType.MCCABE, false),
            new MetricSelection(DLoC.class, LoFType.DLOC, false),
            new MetricSelection(DLoC.class, LoFType.LOF, false),
            new MetricSelection(DLoC.class, LoFType.PLOF, false),
            new MetricSelection(NestingDepth.class, NDType.CLASSIC_ND_MAX, false),
            new MetricSelection(NestingDepth.class, NDType.CLASSIC_ND_AVG, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.CLASSICAL_FAN_IN_GLOBALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.CLASSICAL_FAN_IN_LOCALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.CLASSICAL_FAN_OUT_GLOBALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.CLASSICAL_FAN_OUT_LOCALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.VP_FAN_IN_GLOBALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.VP_FAN_IN_LOCALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.VP_FAN_OUT_GLOBALLY, false),
            new MetricSelection(EigenVectorCentrality.class, FanType.VP_FAN_OUT_LOCALLY, false),
            new MetricSelection(FanInOut.class, FanType.CLASSICAL_FAN_IN_GLOBALLY, false),
            new MetricSelection(FanInOut.class, FanType.CLASSICAL_FAN_IN_LOCALLY, false),
            new MetricSelection(FanInOut.class, FanType.CLASSICAL_FAN_OUT_GLOBALLY, false),
            new MetricSelection(FanInOut.class, FanType.CLASSICAL_FAN_OUT_LOCALLY, false),
            new MetricSelection(FanInOut.class, FanType.VP_FAN_IN_GLOBALLY, false),
            new MetricSelection(FanInOut.class, FanType.VP_FAN_IN_LOCALLY, false),
            new MetricSelection(FanInOut.class, FanType.VP_FAN_OUT_GLOBALLY, false),
            new MetricSelection(FanInOut.class, FanType.VP_FAN_OUT_LOCALLY, false),
            new MetricSelection(UndisciplinedPreprocessorUsage.class, null, false),
        };
        return selectedMetrics;
    }
    
    /**
     * Coded list of metrics to execute.
     * @param params The parameters for creating metrics.
     * 
     * @return The selected metric instances.
     * @throws SetUpException If creation of specified metrics is not possible (wrong set-up)
     */
    @SuppressWarnings("unused")
    private List<@NonNull AbstractFunctionMetric<?>> createMetrics(MetricCreationParameters params)
        throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> metrics = new ArrayList<>();
        params.setSingleMetricExecution(true);
        params.setHierarchyType(HierarchyType.HIERARCHY_WEIGHTS_BY_FILE);
        
        @NonNull List<@NonNull Map<String, Integer>> hierarchyWeightVectorSpace = createHierarchyWeightVectorSpace();
        
        // Variables per function metrics
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // External Variables
            params.setMetricSpecificSettingValue(VariablesPerFunction.VarType.EXTERNAL);
            metrics.addAll(MetricFactory.createAllVariations(VariablesPerFunction.class, params));
        }
        logCreatedMetrics(metrics);
        
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // All Variables
            params.setMetricSpecificSettingValue(VariablesPerFunction.VarType.ALL);
            metrics.addAll(MetricFactory.createAllVariations(VariablesPerFunction.class, params));
        }
        
        // Nesting Depth
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // VP NDmax
            params.setMetricSpecificSettingValue(NestingDepth.NDType.VP_ND_MAX);
            metrics.addAll(MetricFactory.createAllVariations(NestingDepth.class, params));
        }
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // Code + VP NDmax
            params.setMetricSpecificSettingValue(NestingDepth.NDType.COMBINED_ND_MAX);
            metrics.addAll(MetricFactory.createAllVariations(NestingDepth.class, params));
        }
        
        // DC Fan-In/Out
        for (@NonNull Map<String, Integer> hierarchyWeights : hierarchyWeightVectorSpace) {
            // Disable caching of weights, because all weighs use different values
            params.setHierarchyWeights(hierarchyWeights);
            WeigthsCache.INSTANCE.clear();
            
            // DC Fan-In (locally)
            params.setMetricSpecificSettingValue(FanInOut.FanType.DEGREE_CENTRALITY_IN_LOCALLY);
            metrics.addAll(MetricFactory.createAllVariations(FanInOut.class, params));
        }
        
        return metrics;
    }
    
    @Override
    // CHECKSTYLE:OFF // TODO: somehow checkstyle always adds an error for indentation here?
    protected @NonNull List<@NonNull AbstractFunctionMetric<?>> instantiateMetrics(
    // CHECKSTYLE:ON
        @NonNull MetricCreationParameters params) throws SetUpException {
        
        List<@NonNull AbstractFunctionMetric<?>> result = createAllAtomicVariations(params);

        logCreatedMetrics(result);
        return result;
    }
    
    @Override
    public @NonNull String getResultName() {
        return "SpecifiedCodeFunctions";
    }
    
}
