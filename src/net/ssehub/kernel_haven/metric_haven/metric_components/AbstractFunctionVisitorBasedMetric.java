package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFunctionVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Class to simplify {@link AbstractFunctionVisitor}-based function-metrics.
 * @author El-Sharkawy
 *
 * @param <V> The {@link AbstractFunctionVisitor}-<b>visitor</b>
 *     which is used for all (derivations) of the metric to compute.
 */
abstract class AbstractFunctionVisitorBasedMetric<V extends AbstractFunctionVisitor>
    extends AnalysisComponent<MetricResult> {

    private @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    
    /**
     * Sole constructor for this class.
     * @param config The pipeline configuration.
     * @param codeFunctionFinder The component to get the code functions from
     *     (the metric will be computed for each function).
     * @param varModelComponent Optional: If a {@link VariabilityModel} is passed via this component, the
     *     {@link AbstractFunctionVisitor} may use this to identify variable parts depending on variables defined in
     *     the {@link VariabilityModel}.
     */
    AbstractFunctionVisitorBasedMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent) {
        
        super(config);
        this.codeFunctionFinder = codeFunctionFinder;
        this.varModelComponent = varModelComponent;
    }
    
    /**
     * Creates the visitor at the start of the computation.
     * @param varModel The variability model (retrieved from the <tt>varModelComponent</tt>), may be <tt>null</tt>.
     * @return The visitor to use for each of the functions, will be reseted after each computation via
     *     {@link AbstractFunctionVisitor#reset()}.
     */
    protected abstract @NonNull V createVisitor(@Nullable VariabilityModel varModel);
    
    /**
     * This method will compute the result based on the results of the visitor created by
     *     {@link #createVisitor(VariabilityModel)}.
     * @param visitor The visitor after visitation of a single function.
     * @return The computed value or {@link Double#NaN} if no result could be computed an this metric should not
     *     mention the result.
     */
    protected abstract double computeResult(@NonNull V visitor);
    
    @Override
    protected final void execute() {
        VariabilityModel varModel = null;
        if (null != varModelComponent) {
            varModel = varModelComponent.getNextResult();
        }
        
        CodeFunction function;
        V visitor = createVisitor(varModel);
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            Function astRoot = function.getFunction();
            visitor.reset();
            astRoot.accept(visitor);
            
            double result = computeResult(visitor);
            if (Double.NaN == result) {
                return;
            }
            
            Function functionAST = function.getFunction();
            File cFile = function.getSourceFile().getPath();
            File includedFile = cFile.equals(functionAST.getSourceFile()) ? null : functionAST.getSourceFile();
            addResult(new MetricResult(cFile, includedFile, functionAST.getLineStart(), function.getName(), result));
        }
    }

}
