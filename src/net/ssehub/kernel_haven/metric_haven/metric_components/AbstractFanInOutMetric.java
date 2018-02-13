package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Abstract super class for metrics, which count any function calls (outgoing or incoming).
 * @author El-Sharkawy
 *
 */
public abstract class AbstractFanInOutMetric extends AnalysisComponent<MetricResult> {

    private @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    
    /**
     * Sole constructor for this class.
     * @param config The pipeline configuration.
     * @param codeFunctionFinder The component to get the code functions from
     *     (the metric will be computed for each function).
     * @param varModelComponent Optional: If a {@link VariabilityModel} is passed via this component, visitors may use
     *     this to identify variable parts depending on variables defined in the {@link VariabilityModel}.
     */
    AbstractFanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) {
        
        super(config);
        this.codeFunctionFinder = codeFunctionFinder;
        this.varModelComponent = varModelComponent;
    }
    
    @Override
    protected final void execute() {
        VariabilityModel varModel = null;
        if (null != varModelComponent) {
            varModel = varModelComponent.getNextResult();
        }
        
        CodeFunction function;
        List<CodeFunction> functions = new LinkedList<>();
        Set<String> allFunctionNames = new HashSet<>();
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            functions.add(function);
            allFunctionNames.add(function.getName());
        }
        
        // Looses threading for sub analyses, but should not be a big issue
        computeMetrics(functions, allFunctionNames, varModel);
    }

    /**
     * Computes Fan-In / Fan-Out Metrics.
     * @param functions All gathered functions.
     * @param allFunctionNames The names of all functions.
     * @param varModel Optional: the variability model.
     */
    protected abstract void computeMetrics(@NonNull List<CodeFunction> functions, @NonNull Set<String> allFunctionNames,
        @Nullable VariabilityModel varModel);
}
