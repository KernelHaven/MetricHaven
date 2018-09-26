package net.ssehub.kernel_haven.metric_haven.metric_components.config;

import java.util.List;

import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Definition of metric settings.
 * @author El-Sharkawy
 *
 */
public class MetricSettings {
    
    public static final @NonNull Setting<@Nullable List<String>> FILTER_BY_FILES = new Setting<>(
        "metrics.filter_results_by.files", Type.STRING_LIST, false, null, "If defined, the results are filter so that "
            + "the final results will contain only results for the specified files (comma separated list)");
    
    public static final @NonNull Setting<@Nullable List<@NonNull String>> LINE_NUMBER_SETTING
        = new Setting<>("analysis.code_function.lines", Type.STRING_LIST, false, null,
            "Specifies, the files and line numbers that the CodeFunctionByLineFilter should filter the code functions "
            + "for. Each element in the list should have a colon separated file path and line number (e.g. "
            + "kernel/kernel.c:51 for line 51 in the file kernel/kernel.c). File paths are relative to the source "
            + "tree. The filter will pass on only the functions that contain one of the file and line number pairs.");
    
    /**
     * Configuration of variability weight (for features): <b>Scattering Degree</b>.
     */
    public static final @NonNull Setting<@NonNull SDType> SCATTERING_DEGREE_USAGE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_scattering_degree", SDType.class, true, 
            SDType.NO_SCATTERING, "Defines whether and how to incorporate scattering degree values"
                + "into measurement results.\n"
                + " - " + SDType.NO_SCATTERING.name() + ": Do not consider scattering degree (default).\n"
                + " - " + SDType.SD_VP.name() + ": Use variation point scattering.\n"
                + " - " + SDType.SD_FILE.name() + ": Use filet scattering.");

    /**
     * Configuration of variability weight (for features): <b>Cross-Tree Constraint Ratio</b>.
     */
    public static final @NonNull Setting<@NonNull CTCRType> CTCR_USAGE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_ctcr", CTCRType.class, true, 
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
        = new EnumSetting<>("metric.function_measures.consider_feature_definition_distance", FeatureDistanceType.class,
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
        = new EnumSetting<>("metric.function_measures.consider_feature_types", VariabilityTypeMeasureType.class,
            true, VariabilityTypeMeasureType.NO_TYPE_MEASURING, "Defines whether and how to incorporate types of used "
                + "features:\n"
                + " - " + VariabilityTypeMeasureType.NO_TYPE_MEASURING.name() + ": Do not consider any types (default)."
                + "\n - " + VariabilityTypeMeasureType.TYPE_WEIGHTS_BY_FILE.name() + ": Weights are defined in the "
                + "configuration file.");
    
    /**
     * Configuration of variability weight (for features): Definitions of weights for {@value #TYPE_MEASURING_SETTING}.
     */
    public static final @NonNull Setting<@Nullable List<@NonNull String>> TYPE_WEIGHTS_SETTING
        = new Setting<>("metric.function_measures.type_weight_definitions", Type.STRING_LIST,
            false, null, "Defines the weights to be used if " + TYPE_MEASURING_SETTING.getKey() + " is set to "
                + VariabilityTypeMeasureType.TYPE_WEIGHTS_BY_FILE.name() + "\n"
                + "Define the weights in form of (separated by a comma): type:weight");
    
    /**
     * Configuration of variability weight (for features): <b>Hierarchy level of features</b>.
     */
    public static final @NonNull Setting<@NonNull HierarchyType> HIERARCHY_TYPE_MEASURING_SETTING
        = new EnumSetting<>("metric.function_measures.consider_feature_hierarchies", HierarchyType.class,
            true, HierarchyType.NO_HIERARCHY_MEASURING, "Defines whether and how to incorporate hierarchies of used "
                + "features:\n"
                + " - " + HierarchyType.NO_HIERARCHY_MEASURING.name() + ": Do not consider any hierarchies (default).\n"
                + " - " + HierarchyType.HIERARCHY_WEIGHTS_BY_FILE.name() + ": Weights are defined in the configuration "
                + "file.\n"
                + " - " + HierarchyType.HIERARCHY_WEIGHTS_BY_LEVEL.name() + ": The hierarchy (level) is directly "
                + "used as weight.");
    
    /**
     * Configuration of variability weight (for features): Definitions of weights for
     * {@value #HIERARCHY_TYPE_MEASURING_SETTING}.
     */
    public static final @NonNull Setting<@Nullable List<@NonNull String>> HIERARCHY_WEIGHTS_SETTING
        = new Setting<>("metric.function_measures.hierarchy_weight_definitions", Type.STRING_LIST,
            false, null, "Defines the weights to be used if " + HIERARCHY_TYPE_MEASURING_SETTING.getKey()
                + " is set to " + HierarchyType.HIERARCHY_WEIGHTS_BY_FILE.name() + "\n"
                + "Define the weights in form of (separated by a comma): hierarchy:weight");
    
    /**
     * Configuration of variability weight (for features): <b>Structural information of variability models</b>.
     */
    public static final @NonNull Setting<@NonNull StructuralType> STRUCTURE_MEASURING_SETTING
        = new EnumSetting<>("metric.function_measures.consider_varmodel_structures", StructuralType.class,
            true, StructuralType.NO_STRUCTURAL_MEASUREMENT, "Defines whether and how to incorporate stuctural"
                + "information of used features:\n"
                + " - " + StructuralType.NO_STRUCTURAL_MEASUREMENT.name() + ": Do not consider any structures "
                + "(default).\n"
                + " - " + StructuralType.NUMBER_OF_CHILDREN.name() + ": Count number of children (inspired by RoV).\n"
                + " - " + StructuralType.COC.name() + ": Count all edges (inspired by CoC).");

    public static final @NonNull Setting<DLoC.LoFType>
            LOC_TYPE_SETTING = new EnumSetting<>("metric.loc.measured_type", DLoC.LoFType.class, true, 
            DLoC.LoFType.DLOC,
            "Defines which lines of code should be counted for a function:\n"
            + " - " + DLoC.LoFType.DLOC.name()
            + ": Counts all statements, i.e., all delivered Lines of Code\n"
            + "   (dLoC).\n"
            + " - " + DLoC.LoFType.LOF.name()
            + ": Counts all variable statements, sometimes refereed to as Lines\n"
            + "   of Feature code (LoF).\n"
            + " - " + DLoC.LoFType.PLOF.name()
            + ": Computes the fraction of LoF/dLoC (0 if LoF is 0).\n");
    
}
