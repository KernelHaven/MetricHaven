package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
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

    public static final @NonNull Setting<@NonNull SDType> SCATTERING_DEGREE_USAGE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_scattering_egree", SDType.class, true, 
            SDType.NO_SCATTERING, "Defines whether and how to incorporate scattering degree values"
                + "into measurement results.\n:"
                + " - " + SDType.NO_SCATTERING.name() + ": Do not consider scattering degree (default).\n"
                + " - " + SDType.SD_VP.name() + ": Use variation point scattering.\n"
                + " - " + SDType.SD_FILE.name() + ": Use filet scattering.");
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    
    private @NonNull SDType sdType;
    private @Nullable ScatteringDegreeContainer sdList;
    
    /**
     * Sole constructor for this class.
     * @param config The pipeline configuration.
     * @param codeFunctionFinder The component to get the code functions from
     *     (the metric will be computed for each function).
     * @param varModelComponent Optional: If a {@link VariabilityModel} is passed via this component, the
     *     {@link AbstractFunctionVisitor} may use this to identify variable parts depending on variables defined in
     *     the {@link VariabilityModel}.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     *     
     * @throws SetUpException If {@link #SCATTERING_DEGREE_USAGE_SETTING} is used, but <tt>sdComponent</tt> is
     *     <tt>null</tt>.
     */
    AbstractFunctionVisitorBasedMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config);
        this.codeFunctionFinder = codeFunctionFinder;
        this.varModelComponent = varModelComponent;
        this.sdComponent = sdComponent;
        
        config.registerSetting(SCATTERING_DEGREE_USAGE_SETTING);
        sdType = config.getValue(SCATTERING_DEGREE_USAGE_SETTING);
        if (sdType != SDType.NO_SCATTERING && null == sdComponent) {
            throw new SetUpException("Use of scatterind degree was configured (" + sdType.name() + "), but no "
                + "SD analysis component was passed to " + this.getClass().getName());
        }
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
        
        if (null != sdComponent) {
            sdList = sdComponent.getNextResult();
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
    
    /**
     * Returns information about all computed scattering degree values for all variables of the variability model.
     * @return The scattering degree values if available or <tt>null</tt>.
     */
    protected @Nullable ScatteringDegreeContainer getScatteringDegrees() {
        return sdList;
    }
    
    /**
     * Returns the {@link SCATTERING_DEGREE_USAGE_SETTING} setting.
     * @return The configured {@link SDType}.
     */
    protected @NonNull SDType getSDType() {
        return sdType;
    }
}
