package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.ClassicalFanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.ClassicalFanInOutVisitor.MeasurementType;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Measurement of various fan-in/fan-out metrics based on function calls.
 * @author El-Sharkawy
 *
 */
public class FanInOutMetric extends AbstractFanInOutMetric {

    /**
     * Specification which kind of variables shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum FanType {
        CLASSICAL_FAN_IN_GLOBALLY, CLASSICAL_FAN_IN_LOCALLY,
        CLASSICAL_FAN_OUT_GLOBALLY, CLASSICAL_FAN_OUT_LOCALLY;
    }
    
    public static final @NonNull Setting<@NonNull FanType> FAN_TYPE_SETTING
        = new EnumSetting<>("metric.fan_in_out.type", FanType.class, true, 
                FanType.CLASSICAL_FAN_IN_GLOBALLY, "Defines which type of fan in/out should be counted for a"
                    + " function.");
    
    private @NonNull FanType type;
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @throws SetUpException if {@link #ND_TYPE_SETTING} is misconfigured.
     */
    public FanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder) throws SetUpException {
        
        this(config, codeFunctionFinder, null);
    }
    
    /**
     * Creates this metric.
     * @param config The global configuration.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @throws SetUpException if {@link #FAN_TYPE_SETTING} is misconfigured.
     */
    public FanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent);
        config.registerSetting(FAN_TYPE_SETTING);
        type = config.getValue(FAN_TYPE_SETTING);
    }

    @Override
    protected double computeResult(@NonNull AbstractFanInOutVisitor visitor, CodeFunction function) {
        return visitor.getResult(function.getName());
    }

    @Override
    protected @NonNull AbstractFanInOutVisitor createVisitor(@NonNull List<CodeFunction> functions,
        @Nullable VariabilityModel varModel) {
        
        AbstractFanInOutVisitor visitor = null;
        switch (type) {
        case CLASSICAL_FAN_IN_GLOBALLY:
            visitor = new ClassicalFanInOutVisitor(functions, varModel, MeasurementType.FAN_IN_GLOBALLY);
            break;
        case CLASSICAL_FAN_IN_LOCALLY:
            visitor = new ClassicalFanInOutVisitor(functions, varModel, MeasurementType.FAN_IN_LOCALLY);
            break;
        case CLASSICAL_FAN_OUT_GLOBALLY:
            visitor = new ClassicalFanInOutVisitor(functions, varModel, MeasurementType.FAN_OUT_GLOBALLY);
            break;
        case CLASSICAL_FAN_OUT_LOCALLY:
            visitor = new ClassicalFanInOutVisitor(functions, varModel, MeasurementType.FAN_OUT_LOCALLY);
            break;
        default:
            LOGGER.logError("Unsupported metric variation " + type.name() + " for metric "
                + getClass().getName());
            break;
        }
        
        if (null == visitor) {
            LOGGER.logError("No strategy was instantiated for " + getClass().getName() + " loaded default one "
                + ClassicalFanInOutVisitor.class.getName());
            visitor = new ClassicalFanInOutVisitor(functions, varModel, MeasurementType.FAN_IN_GLOBALLY);
        }
        
        return visitor;
    }

    @Override
    public @NonNull String getResultName() {
        String resultName;
        switch (type) {
        case CLASSICAL_FAN_IN_GLOBALLY:
            resultName = "Classical Fan-In (globally)";
            break;
        case CLASSICAL_FAN_IN_LOCALLY:
            resultName = "Classical Fan-In (locally)";
            break;
        case CLASSICAL_FAN_OUT_GLOBALLY:
            resultName = "Classical Fan-Out (globally)";
            break;
        case CLASSICAL_FAN_OUT_LOCALLY:
            resultName = "Classical Fan-Out (locally)";
            break;
        default:
            resultName = "Unspecified Fan-In/Out metric";
            break;
        }
        
        return resultName;
    }
}
