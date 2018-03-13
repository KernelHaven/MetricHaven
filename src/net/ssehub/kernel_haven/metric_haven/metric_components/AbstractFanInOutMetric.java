package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFanInOutVisitor;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Abstract super class for metrics, which count any function calls (outgoing or incoming).
 * @author El-Sharkawy
 *
 */
abstract class AbstractFanInOutMetric extends AnalysisComponent<MetricResult> {

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
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            functions.add(function);
        }
        // Looses threading for sub analyses, but should not be a big issue
        
        // Gather function calls for all functions
        AbstractFanInOutVisitor visitor = createVisitor(functions, varModel);
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
     * @return The visitor to compute the fan-in fan-out metric by the inherited metric analysis.
     */
    protected abstract @NonNull AbstractFanInOutVisitor createVisitor(@NonNull List<CodeFunction> functions,
        @Nullable VariabilityModel varModel);
}