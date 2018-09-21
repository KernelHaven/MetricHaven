package net.ssehub.kernel_haven.metric_haven.metric_components;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.LoCVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * A metric, which measures the amount of delivered lines of code (dLoC) per function. More precisely, this metric
 * measures the number of statements within a function which should be a good approximation for dLoC.
 * @author El-Sharkawy
 *
 */
public class DLoC extends AbstractFunctionVisitorBasedMetric<LoCVisitor> {
    
    public static final @NonNull Setting<net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType>
        LOC_TYPE_SETTING = new EnumSetting<>("metric.loc.measured_type",
            net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.class, true, 
                net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.DLOC,
                "Defines which lines of code should be counted for a function:\n"
                + " - " + net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.DLOC.name()
                + ": Counts all statements, i.e., all delivered Lines of Code\n"
                + "   (dLoC).\n"
                + " - " + net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.LOF.name()
                + ": Counts all variable statements, sometimes refereed to as Lines\n"
                + "   of Feature code (LoF).\n"
                + " - " + net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType.PLOF.name()
                + ": Computes the fraction of LoF/dLoC (0 if LoF is 0).\n");
    
    private net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType type;
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException if {@link #LOC_TYPE_SETTING} is misconfigured.
     */
    public DLoC(@NonNull Configuration config, @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder)
        throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: The component to get the variability model from. If not <tt>null</tt>
     *     {@link LoFType#LOF} and {@link LoFType#PLOF} will check if at least one variable of the variability
     *     model is involved in {@link net.ssehub.kernel_haven.code_model.ast.CppBlock#getCondition()} expressions.
     * @throws SetUpException if {@link #LOC_TYPE_SETTING} is misconfigured.
     */
    public DLoC(@NonNull Configuration config, @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, null, null);
        config.registerSetting(LOC_TYPE_SETTING);
        type = config.getValue(LOC_TYPE_SETTING);
        
        checkVariabilityWeights(false, type);
    }
    
    @Override
    protected LoCVisitor createVisitor(@Nullable VariabilityModel varModel) {
        return new LoCVisitor(varModel);
    }
    
    @Override
    protected double computeResult(@NonNull LoCVisitor visitor) {
        double result;
        switch(type) {
        case DLOC:
            result = visitor.getDLoC();
            break;
        case LOF:
            result = visitor.getLoF();
            break;
        case PLOF:
            result = visitor.getPLoF();
            break;
        default:
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                LOC_TYPE_SETTING.getKey(), "=", type.name());
            result = Double.NaN;
            break;
        }
        
        return result;
    }

    @Override
    public @NonNull String getResultName() {
        String resultName;
        switch(type) {
        case DLOC:
            resultName = "LoC";
            break;
        case LOF:
            resultName = "LoF";
            break;
        case PLOF:
            resultName = "PLoF";
            break;
        default:
            resultName = "Unsupported metric specified";
            LOGGER.logError("Unsupported value setting for ", getClass().getName(), "-alysis: ",
                LOC_TYPE_SETTING.getKey(), "=", type.name());
            break;
        }
        
        return resultName;
    }

}
