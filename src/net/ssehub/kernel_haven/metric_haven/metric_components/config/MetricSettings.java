/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.metric_haven.metric_components.config;

import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.ListSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.code_metrics.BlocksPerFunctionMetric.BlockMeasureType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.CyclomaticComplexity.CCType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.LoCMetric;
import net.ssehub.kernel_haven.metric_haven.code_metrics.FanInOut;
import net.ssehub.kernel_haven.metric_haven.code_metrics.NestingDepth;
import net.ssehub.kernel_haven.metric_haven.code_metrics.NestingDepth.NDType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.TanglingDegree.TDType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Definition of metric settings.
 * @author El-Sharkawy
 *
 */
public class MetricSettings {
    
    public static final @NonNull ListSetting<@NonNull String> FILTER_BY_FUNCTIONS = new ListSetting<>(
        "metrics.filter_results_by.functions", Type.STRING, false,
            "Specifies, the files and function names that the CodeFunctionByPathAndNameFilter should filter the code "
                + "functions for. Each element in the list should have a colon separated file path and function name "
                + "(e.g. kernel/kernel.c:init for all occurrences of a function with the name init in the file "
                + "kernel/kernel.c). File paths are relative to the source tree.");
    
    public static final @NonNull ListSetting<@NonNull String> LINE_NUMBER_SETTING
        = new ListSetting<>("analysis.code_function.lines", Type.STRING, false,
            "Specifies, the files and line numbers that the CodeFunctionByLineFilter should filter the code functions "
            + "for. Each element in the list should have a colon separated file path and line number (e.g. "
            + "kernel/kernel.c:51 for line 51 in the file kernel/kernel.c). File paths are relative to the source "
            + "tree. The filter will pass on only the functions that contain one of the file and line number pairs.");

    public static final @NonNull Setting<@NonNull Boolean> ALL_METRIC_VARIATIONS = new Setting<>(
            "metrics.function_measures.all_variations", Type.BOOLEAN, false, "true", "Specifies whether to run all"
                + "variations of a metric in parallel (true) or only a single variation (false). If not specified,"
                + " all variations will be measured by default.");
    
    /**
     * Configuration of variability weight (for features): <b>Scattering Degree</b>.
     */
    public static final @NonNull Setting<@NonNull SDType> SCATTERING_DEGREE_USAGE_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_scattering_degree", SDType.class, true, 
            SDType.NO_SCATTERING, "Defines whether and how to incorporate scattering degree values"
                + "into measurement results.\n"
                + " - " + SDType.NO_SCATTERING.name() + ": Do not consider scattering degree (default).\n"
                + " - " + SDType.SD_VP.name() + ": Use variation point scattering.\n"
                + " - " + SDType.SD_FILE.name() + ": Use filet scattering.");

    /**
     * Configuration of variability weight (for features): <b>Cross-Tree Constraint Ratio</b>.
     */
    public static final @NonNull Setting<@NonNull CTCRType> CTCR_USAGE_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_ctcr", CTCRType.class, true, 
            CTCRType.NO_CTCR, "Defines whether and how to incorporate cross-tree constraint ratios from the variability"
                    + " model into measurement results.\n"
                    + " - " + CTCRType.NO_CTCR.name() + ": Do not consider any cross-tree constraint ratios (default)."
                    + "\n - " + CTCRType.INCOMIG_CONNECTIONS.name() + ": Count number of distinct variables, specifying"
                    + " a\n   constraint TO a measured/detected variable.\n"
                    + " - " + CTCRType.OUTGOING_CONNECTIONS.name() + ": Count number of distinct variables, referenced"
                    + " in\n   constraints defined by the measured/detected variable.\n"
                    + " - " + CTCRType.ALL_CTCR.name() + ": Count number of distinct variables in all constraints "
                    + "connected\n   with the measured/detected variable (intersection of "
                    + CTCRType.INCOMIG_CONNECTIONS.name() + "\n   and " + CTCRType.OUTGOING_CONNECTIONS.name() + ".");
    
    /**
     * Configuration of variability weight (for features): <b>Distance between definition and usage</b>.
     */
    public static final @NonNull Setting<@NonNull FeatureDistanceType> LOCATION_DISTANCE_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_feature_definition_distance", FeatureDistanceType.class,
            true, FeatureDistanceType.NO_DISTANCE, "Defines whether and how to incorporate distance between used "
                + "feature (location of measured code file) and definition of feature "
                + "(defining file of variability model):\n"
                + " - " + FeatureDistanceType.NO_DISTANCE.name() + ": Do not consider any distances (default)."
                + "\n - " + FeatureDistanceType.SHORTEST_DISTANCE.name() + ": Count the minimum number of required"
                + " folder changes to traverse to the defining file.");
    
    /**
     * Configuration of variability weight (for features): <b>Type of features</b>.
     */
    public static final @NonNull Setting<@NonNull VariabilityTypeMeasureType> TYPE_MEASURING_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_feature_types", VariabilityTypeMeasureType.class,
            true, VariabilityTypeMeasureType.NO_TYPE_MEASURING, "Defines whether and how to incorporate types of used "
                + "features:\n"
                + " - " + VariabilityTypeMeasureType.NO_TYPE_MEASURING.name() + ": Do not consider any types (default)."
                + "\n - " + VariabilityTypeMeasureType.TYPE_WEIGHTS_BY_FILE.name() + ": Weights are defined in the "
                + "configuration file.");
    
    /**
     * Configuration of variability weight (for features): Definitions of weights for {@link #TYPE_MEASURING_SETTING}.
     */
    public static final @NonNull ListSetting<@NonNull String> TYPE_WEIGHTS_SETTING
        = new ListSetting<>("metrics.function_measures.type_weight_definitions", Type.STRING,
            false, "Defines the weights to be used if " + TYPE_MEASURING_SETTING.getKey() + " is set to "
                + VariabilityTypeMeasureType.TYPE_WEIGHTS_BY_FILE.name() + "\n"
                + "Define the weights in form of (separated by a comma): type:weight");
    
    /**
     * Configuration of variability weight (for features): <b>Hierarchy level of features</b>.
     */
    public static final @NonNull Setting<@NonNull HierarchyType> HIERARCHY_TYPE_MEASURING_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_feature_hierarchies", HierarchyType.class,
            true, HierarchyType.NO_HIERARCHY_MEASURING, "Defines whether and how to incorporate hierarchies of used "
                + "features:\n"
                + " - " + HierarchyType.NO_HIERARCHY_MEASURING.name() + ": Do not consider any hierarchies (default).\n"
                + " - " + HierarchyType.HIERARCHY_WEIGHTS_BY_FILE.name() + ": Weights are defined in the configuration "
                + "file.\n"
                + " - " + HierarchyType.HIERARCHY_WEIGHTS_BY_LEVEL.name() + ": The hierarchy (level) is directly "
                + "used as weight.");
    
    /**
     * Configuration of variability weight (for features): Definitions of weights for
     * {@link #HIERARCHY_TYPE_MEASURING_SETTING}.
     */
    public static final @NonNull ListSetting<@NonNull String> HIERARCHY_WEIGHTS_SETTING
        = new ListSetting<>("metrics.function_measures.hierarchy_weight_definitions", Type.STRING,
            false, "Defines the weights to be used if " + HIERARCHY_TYPE_MEASURING_SETTING.getKey()
                + " is set to " + HierarchyType.HIERARCHY_WEIGHTS_BY_FILE.name() + "\n"
                + "Define the weights in form of (separated by a comma): hierarchy:weight");
    
    /**
     * Configuration of variability weight (for features): <b>Structural information of variability models</b>.
     */
    public static final @NonNull Setting<@NonNull StructuralType> STRUCTURE_MEASURING_SETTING
        = new EnumSetting<>("metrics.function_measures.consider_varmodel_structures", StructuralType.class,
            true, StructuralType.NO_STRUCTURAL_MEASUREMENT, "Defines whether and how to incorporate stuctural"
                + "information of used features:\n"
                + " - " + StructuralType.NO_STRUCTURAL_MEASUREMENT.name() + ": Do not consider any structures "
                + "(default).\n"
                + " - " + StructuralType.NUMBER_OF_CHILDREN.name() + ": Count number of children (inspired by RoV).\n"
                + " - " + StructuralType.COC.name() + ": Count all edges (inspired by CoC).");
    
    /**
     * Configuration of variability weight (for features): <b>Structural information of variability models</b>.
     */
    public static final @NonNull Setting<@NonNull FeatureSizeType> FEATURE_SIZE_MEASURING_SETTING
        = new EnumSetting<>("metrics.function_measures.feature_sizes", FeatureSizeType.class,
            true, FeatureSizeType.NO_FEATURE_SIZES, "Defines whether and how to incorporate feature"
                + "sizes (Lines of Code per Feature) of used features:\n"
                + " - " + FeatureSizeType.NO_FEATURE_SIZES.name() + ": Do not consider any feature sizes "
                + "(default).\n"
                + " - " + FeatureSizeType.POSITIVE_SIZES.name() + ": Count Lines of code, controlled by the "
                + "positive form of the feature.\n"
                + " - " + FeatureSizeType.TOTAL_SIZES.name() + ": Count Lines of code, controlled by the "
                + "the feature (positive or negated).");

    /*
     * Metric-specific settings
     */
    public static final @NonNull Setting<@NonNull BlockMeasureType> BLOCK_TYPE_SETTING
        = new EnumSetting<>("metrics.blocks_per_function.measured_block_type", BlockMeasureType.class, true, 
        BlockMeasureType.BLOCK_AS_ONE, "Defines whether partial blocks (#elif/#else) are also counted.");
    
    public static final @NonNull Setting<LoCMetric.LoCType>
            LOC_TYPE_SETTING = new EnumSetting<>("metrics.loc.measured_type", LoCMetric.LoCType.class, true, 
            LoCMetric.LoCType.SCOC,
            "Defines which lines of code should be counted for a function:\n"
            + " - " + LoCMetric.LoCType.SCOC.name()  + ": Counts all statements (Stament Count Of Code).\n"
            + " - " + LoCMetric.LoCType.SCOF.name()  + ": Counts all variable statements "
                                                     + "(Stament Count Of Feature Code).\n"
            + " - " + LoCMetric.LoCType.PSCOF.name() + ": SCOF / SCOC (0 if SCOF is 0).\n"
            + " - " + LoCMetric.LoCType.LOC.name()   + ": Counts all lines (Lines Of Code).\n"
            + " - " + LoCMetric.LoCType.LOF.name()   + ": Counts all variable lines (Lines Of Feature Code).\n"
            + " - " + LoCMetric.LoCType.PLOF.name()  + ": LOF / LOC (0 if LOF is 0).\n");
    
    public static final @NonNull Setting<FanInOut.@NonNull FanType> FAN_TYPE_SETTING
        = new EnumSetting<>("metrics.fan_in_out.type", FanInOut.FanType.class, true, 
            FanInOut.FanType.CLASSICAL_FAN_IN_GLOBALLY, "Defines which type of fan in/out should be counted for a"
            + " function.");
    
    public static final @NonNull Setting<@NonNull CCType> CC_VARIABLE_TYPE_SETTING
        = new EnumSetting<>("metrics.cyclomatic_complexity.measured_type", CCType.class, true, 
                CCType.MCCABE, "Defines which variables should be counted for a function.");
    
    public static final @NonNull Setting<@NonNull NDType> ND_TYPE_SETTING
        = new EnumSetting<>("metrics.nesting_depth.measured_type", NDType.class, true, 
        NDType.CLASSIC_ND_MAX, "Defines what should be counteded as the nesting depth:\n"
            + " - " + NDType.CLASSIC_ND_MAX.name() + ": Counts the max. nesting depth w.r.t classical\n"
            + "   control structures.\n"
            + " - " + NDType.CLASSIC_ND_AVG.name() + ": Counts the avg. nesting depth w.r.t classical\n"
            + "   control structures.\n"
            + " - " + NDType.VP_ND_MAX.name() + ": Counts the max. nesting depth w.r.t variation points\n"
            + "   (CPP-blocks).\n"
            + " - " + NDType.VP_ND_AVG.name() + ": Counts the avg. nesting depth w.r.t variation points\n"
            + "   (CPP-blocks).\n"
            + " - " + NDType.COMBINED_ND_MAX.name() + ": " + NDType.CLASSIC_ND_MAX.name() + " + "
                + NestingDepth.NDType.VP_ND_MAX.name() + "\n"
            + " - " + NDType.COMBINED_ND_AVG.name() + ": " + NDType.CLASSIC_ND_AVG.name() + " + "
            + NDType.VP_ND_AVG.name());
    
    public static final @NonNull Setting<@NonNull TDType> TD_TYPE_SETTING
        = new EnumSetting<>("metrics.tangling_degree.measured_type", TDType.class, true, 
            TDType.TD_ALL, "Defines what should be considered when computing nesting degree:\n"
                + " - " + TDType.TD_ALL.name() + ": Considers all variation points also those with invisible "
                + "expressions, i.e., else-blocks (default).\n"
                + " - " + TDType.TD_VISIBLE.name() + ": Considers only visible variation points, i.e., "
                + "no else-blocks");
    
    public static final @NonNull Setting<
        net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction.VarType> VARIABLE_TYPE_SETTING
            = new EnumSetting<>("metrics.variables_per_function.measured_variables_type",
            net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction.VarType.class, true, 
            net.ssehub.kernel_haven.metric_haven.code_metrics.VariablesPerFunction.VarType.ALL,
                    "Defines which variables should be counted for a function.");
    
}
