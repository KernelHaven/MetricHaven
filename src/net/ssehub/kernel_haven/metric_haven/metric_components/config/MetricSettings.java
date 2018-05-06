package net.ssehub.kernel_haven.metric_haven.metric_components.config;

import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Definition of metric settings.
 * @author El-Sharkawy
 *
 */
public class MetricSettings {
    
    /**
     * Configuration of variability weight (for features): <b>Scattering Degree</b>.
     */
    public static final @NonNull Setting<SDType> SCATTERING_DEGREE_USAGE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_scattering_degree", SDType.class, true, 
            SDType.NO_SCATTERING, "Defines whether and how to incorporate scattering degree values"
                + "into measurement results.\n"
                + " - " + SDType.NO_SCATTERING.name() + ": Do not consider scattering degree (default).\n"
                + " - " + SDType.SD_VP.name() + ": Use variation point scattering.\n"
                + " - " + SDType.SD_FILE.name() + ": Use filet scattering.");

    /**
     * Configuration of variability weight (for features): <b>Cross-Tree Constraint Ratio</b>.
     */
    public static final @NonNull Setting<CTCRType> CTCR_USAGE_SETTING
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
    public static final @NonNull Setting<FeatureDistanceType> LOCATION_DISTANCE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_feature_definition_distance", FeatureDistanceType.class,
            true, FeatureDistanceType.NO_DISTANCE, "Defines whether and how to incorporate distance between used "
                + "feature (location of measured code file) and definition of feature "
                + "(defining file of variability model):\n"
                + " - " + FeatureDistanceType.NO_DISTANCE.name() + ": Do not consider any distances (default)."
                + "\n - " + FeatureDistanceType.SHORTEST_DISTANCE.name() + ": Count the minimum number of required"
                + " folder changes to traverse to the defining file.\n");

}
