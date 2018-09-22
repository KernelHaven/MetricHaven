package net.ssehub.kernel_haven.metric_haven.code_metrics;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.NestingDepthVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
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
public class NestingDepth extends AbstractFunctionMetric<NestingDepthVisitor> {

    /**
     * Specification which kind of nesting depth metric shall be measured.
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
        
        /**
         * Specifies whether the setting is used measure variability attributes and, thus, may be combined with a
         * variability weight.
         * @return <tt>true</tt> if it may be combined with a {@link IVariableWeight}, <tt>false</tt> otherwise.
         */
        public boolean isVariabilityMetric() {
            return isVariabilityMetric;
        }
    }

    private @NonNull NDType type;
    
    /**
     * Creates a new Nesting depth metric, which can also be created by the {@link MetricFactory}.
     * @param varModel I not <tt>null</tt> this visitor check if at least one variable of the variability
     *     model is involved in {@link CppBlock#getCondition()} expressions and supports also the usage of
     *     {@link IVariableWeight}s.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * @param buildModel May be <tt>null</tt> as it is not used by this metric.
     * @param type Specifies whether to measure only classical/variation point nesting depth, or the fraction of both.
     */
    @PreferedConstructor
    NestingDepth(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight, @NonNull NDType type) {
        
        super(varModel, buildModel, weight);
        this.type = type;
    }

    @Override
    protected @NonNull NestingDepthVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
        return type.isVariabilityMetric ? new NestingDepthVisitor(varModel, weight)
            : new NestingDepthVisitor(varModel);
    }

    @Override
    protected Number computeResult(@NonNull NestingDepthVisitor functionVisitor, CodeFunction func) {
        double result;
        switch(type) {
        case CLASSIC_ND_MAX:
            result = functionVisitor.getClassicalNestingDepth(true);
            break;
        case CLASSIC_ND_AVG:
            result = functionVisitor.getClassicalNestingDepth(false);
            break;
        case VP_ND_MAX:
            result = functionVisitor.getVariationPointNestingDepth(true);
            break;
        case VP_ND_AVG:
            result = functionVisitor.getVariationPointNestingDepth(false);
            break;
        case COMBINED_ND_MAX:
            result = functionVisitor.getClassicalNestingDepth(true)
                + functionVisitor.getVariationPointNestingDepth(true);
            break;
        case COMBINED_ND_AVG:
            result = functionVisitor.getClassicalNestingDepth(false)
                + functionVisitor.getVariationPointNestingDepth(false);
            break;
        default:
            LOGGER.logError2("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                type.getClass().getSimpleName(), "=", type.name());
            result = Double.NaN;
            break;
        }
        
        return result;
    }

    @Override
    public @NonNull String getMetricName() {
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
                type.getClass().getSimpleName(), "=", type.name());
            break;
        }
        
        return resultName;
    }

}
