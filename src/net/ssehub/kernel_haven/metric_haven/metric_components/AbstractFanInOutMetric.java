package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Abstract super class for metrics, which count any function calls (outgoing or incoming).
 * @author El-Sharkawy
 *
 */
abstract class AbstractFanInOutMetric extends AbstractFunctionVisitorBasedMetric<AbstractFanInOutVisitor> {

    private @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    
    /**
     * Simple constructor for this class, won't consider scattering degree.
     * @param config The pipeline configuration.
     * @param codeFunctionFinder The component to get the code functions from
     *     (the metric will be computed for each function).
     * @param varModelComponent Optional: If a {@link VariabilityModel} is passed via this component, visitors may use
     *     this to identify variable parts depending on variables defined in the {@link VariabilityModel}.
     * @throws SetUpException If {@link #SCATTERING_DEGREE_USAGE_SETTING} is used, but <tt>sdComponent</tt> is
     *     <tt>null</tt>.
     */
    @SuppressWarnings("null")
    AbstractFanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, null, null);
    }
    
    /**
     * Constructor for the automatic instantiation inside the {@link AllFunctionMetrics} component, provides
     * scattering degree.
     * 
     * @param config The complete user configuration for the pipeline. Must not be <code>null</code>.
     * @param codeFunctionFinder The component to get the code functions from.
     * @param varModelComponent Optional: If not <tt>null</tt> the varModel will be used the determine whether a
     *     constant of a CPP expression belongs to a variable of the variability model, otherwise all constants
     *     will be treated as feature constants.
     * @param bmComponent Will be ignored.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     * 
     * @throws SetUpException If {@link #VARIABLE_TYPE_SETTING} was defined with an invalid option.
     */
    @SuppressWarnings("null")
    public AbstractFanInOutMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config, codeFunctionFinder, varModelComponent, bmComponent, sdComponent);
    }
    
    @Override
    protected final void execute() {
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        createWeight(varModel);
        
        CodeFunction function;
        List<CodeFunction> functions = new LinkedList<>();
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            functions.add(function);
        }
        // Looses threading for sub analyses, but should not be a big issue
        
        // Gather function calls for all functions
        AbstractFanInOutVisitor visitor = createVisitor(functions, varModel, getWeighter());
        for (CodeFunction func : functions) {
            func.getFunction().accept(visitor);
        }
        
        // Compute and report all results
        for (CodeFunction func : functions) {
            double result = computeResult(visitor, func);
            if (Double.NaN == result) {
                return;
            }
            
            Function functionAST = func.getFunction();
            File cFile = func.getSourceFile().getPath();
            File includedFile = cFile.equals(functionAST.getSourceFile()) ? null : functionAST.getSourceFile();
            addResult(new MetricResult(cFile, includedFile, functionAST.getLineStart(), func.getName(), result));
        }
    }
    
    @Override
    protected final @NonNull AbstractFanInOutVisitor createVisitor(@Nullable VariabilityModel varModel) {
        throw new IllegalArgumentException("Wrong factory for visitor creation called: createVisitor(VariabilityModel),"
            + " but should be createVisitor(List<CodeFunction>, VariabilityModel, IVariableWeight)");
    }
    
    @Override
    protected final double computeResult(@NonNull AbstractFanInOutVisitor visitor) {
        throw new IllegalArgumentException("Wrong computation method called: computeResult(VariabilityModel),"
            + " but should be computeResult(List<CodeFunction>, VariabilityModel)");
    }
    
    /**
     * This method will compute the result based on the results of the visitor created by
     *     {@link #createVisitor(List, VariabilityModel)}.
     * @param visitor The visitor after visitation of a single function.
     * @param function The function for which we compute the metric result for.
     * @return The computed value or {@link Double#NaN} if no result could be computed an this metric should not
     *     mention the result.
     */
    protected abstract double computeResult(@NonNull AbstractFanInOutVisitor visitor, CodeFunction function);
    
    /**
     * Creates the visitor to be used by the metric.
     * @param functions All gathered functions.
     * @param varModel Optional, if not <tt>null</tt> this visitor should use the variability model to check if at least
     *     one variable of the variability model is involved in {@link CppBlock#getCondition()} expressions.
     * @param weight {@link #getWeighter()}
     * @return The visitor to compute the fan-in fan-out metric by the inherited metric analysis.
     */
    protected abstract @NonNull AbstractFanInOutVisitor createVisitor(@NonNull List<CodeFunction> functions,
        @Nullable VariabilityModel varModel, IVariableWeight weight);
}
