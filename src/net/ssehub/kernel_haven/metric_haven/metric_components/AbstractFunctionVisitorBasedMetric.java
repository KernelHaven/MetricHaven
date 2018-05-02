package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFunctionVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.CtcrWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.MultiWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
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
        = new EnumSetting<>("metric.function_measures.consider_scattering_degree", SDType.class, true, 
            SDType.NO_SCATTERING, "Defines whether and how to incorporate scattering degree values"
                + "into measurement results.\n"
                + " - " + SDType.NO_SCATTERING.name() + ": Do not consider scattering degree (default).\n"
                + " - " + SDType.SD_VP.name() + ": Use variation point scattering.\n"
                + " - " + SDType.SD_FILE.name() + ": Use filet scattering.");
    
    public static final @NonNull Setting<@NonNull CTCRType> CTCR_USAGE_SETTING
        = new EnumSetting<>("metric.function_measures.consider_ctcr", CTCRType.class, true, 
            CTCRType.NO_CTCR, "Defines whether and how to incorporate cross-tree constraint ratios from the variability"
                    + " model into measurement results.\n"
                    + " - " + CTCRType.NO_CTCR.name() + ": Do not consider any cross-tree constraint ratios (default)."
                    + "\n - " + CTCRType.INCOMIG_CONNECTIONS.name() + ": Count number of distinct variables, specifying"
                    + " a\n   constraint TO a measured/detected variable.\n"
                    + " - " + CTCRType.OUTGOING_CONNECTIONS.name() + ": Count number of distinct variables, referenced"
                    + " in\n   constraints defined by the measured/detected variable.\n"
                    + " - " + CTCRType.ALL_CTCR.name() + ": Count number of distinct variables in all constraints "
                    + "connected\n   with the measured/detected variable (intersection of "
                    + CTCRType.INCOMIG_CONNECTIONS.name() + "\n   and " + CTCRType.OUTGOING_CONNECTIONS.name() + ".");
    
    private @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder;
    private @Nullable AnalysisComponent<VariabilityModel> varModelComponent;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable BuildModel bm;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    
    private @NonNull SDType sdType;
    private @NonNull CTCRType ctcrType;
    private IVariableWeight weighter;
    
    /**
     * Sole constructor for this class.
     * @param config The pipeline configuration.
     * @param codeFunctionFinder The component to get the code functions from
     *     (the metric will be computed for each function).
     * @param varModelComponent Optional: If a {@link VariabilityModel} is passed via this component, the
     *     {@link AbstractFunctionVisitor} may use this to identify variable parts depending on variables defined in
     *     the {@link VariabilityModel}.
     * @param bmComponent Optional: If a {@link BuildModel} is passed via this component, variables of the presence
     *     condition of the selected code file may be incorporated into the metric computation.
     * @param sdComponent Optional: If not <tt>null</tt> scattering degree of variables may be used to weight the
     *     results.
     *     
     * @throws SetUpException If {@link #SCATTERING_DEGREE_USAGE_SETTING} is used, but <tt>sdComponent</tt> is
     *     <tt>null</tt>.
     */
    AbstractFunctionVisitorBasedMetric(@NonNull Configuration config,
        @NonNull AnalysisComponent<CodeFunction> codeFunctionFinder,
        @Nullable AnalysisComponent<VariabilityModel> varModelComponent,
        @Nullable AnalysisComponent<BuildModel> bmComponent,
        @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent) throws SetUpException {
        
        super(config);
        this.codeFunctionFinder = codeFunctionFinder;
        this.varModelComponent = varModelComponent;
        this.sdComponent = sdComponent;
        
        config.registerSetting(SCATTERING_DEGREE_USAGE_SETTING);
        sdType = config.getValue(SCATTERING_DEGREE_USAGE_SETTING);
        if (sdType != SDType.NO_SCATTERING && null == sdComponent) {
            throw new SetUpException("Use of scattering degree was configured (" + sdType.name() + "), but no "
                + "SD analysis component was passed to " + this.getClass().getName());
        }
        
        config.registerSetting(CTCR_USAGE_SETTING);
        ctcrType = config.getValue(CTCR_USAGE_SETTING);
        if (ctcrType != CTCRType.NO_CTCR && null == varModelComponent) {
            throw new SetUpException("Use of cross-tree constraint ratio was configured (" + ctcrType.name() + "), but "
                + "no variability model was passed to " + this.getClass().getName());
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
    protected void execute() {
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        bm = (null != bmComponent) ? bmComponent.getNextResult() : null;
        
        createWeight(varModel);
        
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
     * Returns the {@link BuildModel} during the {@link #execute()}-method, if a <tt>bmComponent</tt> was passed to
     * the constructor.
     * @return The {@link BuildModel} or <tt>null</tt>.
     */
    protected final BuildModel getBuildModel() {
        return bm;
    }

    /**
     * Part of {@link #execute()}: Create the weight instance based on the given settings.
     * @param varModel The variability model (may be <tt>null</tt>).
     */
    protected void createWeight(@Nullable VariabilityModel varModel) {
        // Scattering degree
        List<IVariableWeight> weights = new ArrayList<>();
        if (sdType != SDType.NO_SCATTERING && null != sdComponent) {
            ScatteringDegreeContainer sdList = sdComponent.getNextResult();
            weights.add(new ScatteringWeight(sdList, sdType));
            
        }
        
        // Cross-tree constraint ratio
        if (ctcrType != CTCRType.NO_CTCR && null != varModel) {
            weights.add(new CtcrWeight(varModel, ctcrType)); 
        }
        
        // Create final weighting function with as less objects as necessary
        if (weights.isEmpty()) {
            weighter = NoWeight.INSTANCE;
        } else if (weights.size() == 1) {
            weighter = weights.get(0);
        } else {
            weighter = new MultiWeight(weights);
        }
    }
    
    /**
     * Returns the {@link #SCATTERING_DEGREE_USAGE_SETTING} setting.
     * @return The configured {@link SDType}.
     */
    protected @NonNull SDType getSDType() {
        return sdType;
    }
    
    /**
     * Returns the {@link #CTCR_USAGE_SETTING} setting.
     * @return The configured {@link CTCRType}.
     */
    protected @NonNull CTCRType getCTCRType() {
        return ctcrType;
    }
    
    /**
     * Returns a weighting function to be used for all detected variables.
     * @return The weighting function to be used for variables of the variability model (won't be <tt>null</tt>
     *     during the execution).
     */
    protected IVariableWeight getWeighter() {
        return weighter;
    }
    
    /**
     * Returns the name of the selected weights as part of the {@link #getResultName()}.
     * @return The name of the selected weights or an empty string if no weight was selected.
     */
    protected String getWeightsName() {
        String name;
        if (getSDType() != SDType.NO_SCATTERING || getCTCRType() != CTCRType.NO_CTCR) {
            name = " x " + getSDType().name() + " x " + getCTCRType().name();      
        } else {
            name = "";
        }
        
        return name;
    }
    
    /**
     * Returns whether this metric has at least one variability weight defined.
     * @return <tt>true</tt> if at least one variability weight is defined, <tt>false</tt> if no variability weight is
     *     defined.
     */
    protected final boolean hasVariabilityWeight() {
        return getSDType() != SDType.NO_SCATTERING || getCTCRType() != CTCRType.NO_CTCR;
    }
}
