package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.util.Set;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.UsedVariabilityVarsVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Implements the <tt>Number of internal/external configuration options</tt> metric from
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class VariablesPerFunctionMetric extends AbstractFunctionMetric<UsedVariabilityVarsVisitor> {

    /**
     * Specification which kind of variables shall be measured.
     * @author El-Sharkawy
     *
     */
    public static enum VarType {
        INTERNAL,
        EXTERNAL, EXTERNAL_WITH_BUILD_VARS,
        ALL, ALL_WITH_BUILD_VARS;
    }
    
    private @NonNull VarType measuredVars;

    /**
     * Instantiates {@link VariabilityModel}-metric. 
     * @param varModel The variability model to identify features.
     * @param buildModel The build model if in case of external features also features from the build model
     *     should be measured.
     * @param weight A {@link IVariableWeight}to weight/measure the configuration complexity of variation points.
     * @param measuredVars Specifies what kind of features should be measured.
     */
    @PreferedConstructor
    VariablesPerFunctionMetric(@Nullable VariabilityModel varModel, @Nullable BuildModel buildModel,
        @NonNull IVariableWeight weight, @NonNull VarType measuredVars) {
        
        super(varModel, buildModel, weight);
        this.measuredVars = measuredVars;
        // Always all weights are supported

        init();
    }

    @Override
    protected @NonNull UsedVariabilityVarsVisitor createVisitor(@Nullable VariabilityModel varModel,
        @Nullable BuildModel buildModel, @NonNull IVariableWeight weight) {
        
        UsedVariabilityVarsVisitor visitor;
        if (measuredVars == VarType.INTERNAL || measuredVars == VarType.EXTERNAL || measuredVars == VarType.ALL) {
            visitor = new UsedVariabilityVarsVisitor(varModel);
        } else {
            visitor = new UsedVariabilityVarsVisitor(varModel, buildModel);
        }
        return visitor;
    }

    @Override
    protected Number computeResult(@NonNull UsedVariabilityVarsVisitor functionVisitor, CodeFunction func) {
        Set<String> variables;
        switch (measuredVars) {
        case INTERNAL:
            variables = functionVisitor.internalVars();
            break;
        case EXTERNAL_WITH_BUILD_VARS:
            // Falls through
        case EXTERNAL:
            variables = functionVisitor.externalVars();
            break;
        case ALL_WITH_BUILD_VARS:
            // Falls through
        case ALL:
            variables = functionVisitor.allVars();
            break;
        default:
            variables = functionVisitor.allVars();
            throw new IllegalArgumentException("Unsupported " + VarType.class.getSimpleName()
                + " property = " + measuredVars.name());
        }
        
        int result = 0;
        for (String variable : variables) {
            result += getWeight().getWeight(variable, func.getSourceFile().getPath());
        }
        
        return result;
    }

    @Override
    public @NonNull String getMetricName() {
        // TODO Auto-generated method stub
        return null;
    }


}
