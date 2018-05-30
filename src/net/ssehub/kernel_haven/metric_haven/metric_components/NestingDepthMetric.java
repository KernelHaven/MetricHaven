package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.NestingDepthVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A metric, which measures the maximum/average nesting depth. These metrics are based on:
 * <ul>
 *     <ul>Classical Nesting Depth as specified by <a href="https://dl.acm.org/citation.cfm?id=42168">[Conte et al.
 *         1986]</a></ul>
 *     <ul>Nesting Depth of Variation Points (e.g., used/specified by
 *         <a href="https://ieeexplore.ieee.org/abstract/document/6062078/">[Liebig et al. 2010]</a>, 
 *         <a href="https://link.springer.com/article/10.1007/s10664-015-9360-1">[Hunsen et al. 2016]</a>)</ul>
 * </ul>
 * However, we did a small variation as we count the nested statements instead of the branching elements itself. This is
 * done to count the average for nested elements inside a function and also to avoid counting of nesting structures,
 * which do not have at least one nested element. 
 * @author El-Sharkawy
 *
 */
public class NestingDepthMetric extends AbstractFunctionVisitorBasedMetric<NestingDepthVisitor> {
    
    /**
     * Specification which kind of LoC-metric shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum NDType {
        CLASSIC_ND_MAX(false), CLASSIC_ND_AVG(false),
        VP_ND_MAX(true), VP_ND_AVG(true),
        COMBINED_ND_MAX(true), COMBINED_ND_AVG(true);
        
        private boolean isVariabilityMetric;
        
        /**
         * Private constructor to specify whether this enums denotes a metric operating on variability information.
         * @param isVariabilityMetric <tt>true</tt> operates on variability, <tt>false</tt> classical code metric
         */
        private NDType(Boolean isVariabilityMetric) {
            this.isVariabilityMetric = isVariabilityMetric;
        }
    }
    
    public static final @NonNull Setting<@NonNull NDType> ND_TYPE_SETTING
        = new EnumSetting<>("metric.nesting_depth.measured_type", NDType.class, true, 
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
                    + NDType.VP_ND_MAX.name() + "\n"
                + " - " + NDType.COMBINED_ND_AVG.name() + ": " + NDType.CLASSIC_ND_AVG.name() + " + "
                    + NDType.VP_ND_AVG.name());
    
    private @NonNull NDType type;
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException if {@link #ND_TYPE_SETTING} is misconfigured.
     */
    public NestingDepthMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Creates this metric (won't be able to use scattering degree).
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @throws SetUpException if {@link #ND_TYPE_SETTING} is misconfigured.
     */
    public NestingDepthMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, null);
    }
    
    /**
     * Constructor to use scattering degree to weight the results.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #ND_TYPE_SETTING} was defined with an invalid option.
     */
    public NestingDepthMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        this(config, codeFunctionFinder, varModelComponent, null, sdComponent);
    }
    
    /**
     * Constructor for the automatic instantiation inside the {@link AllFunctionMetrics} component..
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param bmComponent Optional: Allows to incorporate variables of the file presence conditions into the
     *     external variables list.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #ND_TYPE_SETTING} was defined with an invalid option.
     */
    public NestingDepthMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);
        
        config.registerSetting(ND_TYPE_SETTING);
        type = config.getValue(ND_TYPE_SETTING);
        
        checkVariabilityWeights(type.isVariabilityMetric, type);
    }
    
    @Override
    protected NestingDepthVisitor createVisitor(@Nullable VariabilityModel varModel) {
        return hasVariabilityWeight() ? new NestingDepthVisitor(varModel, getWeighter())
            : new NestingDepthVisitor(varModel);
    }
    
    @Override
    protected double computeResult(@NonNull NestingDepthVisitor visitor) {
        double result;
        switch(type) {
        case CLASSIC_ND_MAX:
            result = visitor.getClassicalNestingDepth(true);
            break;
        case CLASSIC_ND_AVG:
            result = visitor.getClassicalNestingDepth(false);
            break;
        case VP_ND_MAX:
            result = visitor.getVariationPointNestingDepth(true);
            break;
        case VP_ND_AVG:
            result = visitor.getVariationPointNestingDepth(false);
            break;
        case COMBINED_ND_MAX:
            result = visitor.getClassicalNestingDepth(true) + visitor.getVariationPointNestingDepth(true);
            break;
        case COMBINED_ND_AVG:
            result = visitor.getClassicalNestingDepth(false) + visitor.getVariationPointNestingDepth(false);
            break;
        default:
            LOGGER.logError2("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                ND_TYPE_SETTING.getKey(), "=" + type.name());
            result = Double.NaN;
            break;
        }
        
        return result;
    }

    @Override
    public @NonNull String getResultName() {
        String resultName;
        switch(type) {
        case CLASSIC_ND_MAX:
            resultName = "Classic ND_Max";
            break;
        case CLASSIC_ND_AVG:
            resultName = "Classic ND_Avg";
            break;
        case VP_ND_MAX:
            resultName = "VP ND_Max";
            break;
        case VP_ND_AVG:
            resultName = "VP ND_Avg";
            break;
        case COMBINED_ND_MAX:
            resultName = "Combined ND_Max";
            break;
        case COMBINED_ND_AVG:
            resultName = "Combined ND_Avg";
            break;
        default:
            resultName = "Unsupported metric specified";
            LOGGER.logError2("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                ND_TYPE_SETTING.getKey(), "=", type.name());
            break;
        }
        
        resultName += getWeightsName();
        
        return resultName;
    }

}
