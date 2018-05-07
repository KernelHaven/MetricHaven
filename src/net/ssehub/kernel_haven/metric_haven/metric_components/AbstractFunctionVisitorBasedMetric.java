package net.ssehub.kernel_haven.metric_haven.metric_components;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.metric_haven.MetricResult;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.CTCRType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.FeatureDistanceType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.MetricSettings;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.SDType;
import net.ssehub.kernel_haven.metric_haven.metric_components.config.VariabilityTypeMeasureType;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.AbstractFunctionVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.CtcrWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.FeatureDistanceWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.MultiWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.ScatteringWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.TypeWeight;
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
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    private @Nullable BuildModel bm;
    private @Nullable AnalysisComponent<ScatteringDegreeContainer> sdComponent;
    
    private @NonNull SDType sdType;
    private @NonNull CTCRType ctcrType;
    private @NonNull FeatureDistanceType locationType;
    private @NonNull VariabilityTypeMeasureType varTypeWeightType;
    private @Nullable Map<String, Integer> typeWeights;
    private IVariableWeight weighter;
    private File currentCodefile;
    
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
        
        config.registerSetting(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING);
        sdType = config.getValue(MetricSettings.SCATTERING_DEGREE_USAGE_SETTING);
        if (sdType != SDType.NO_SCATTERING && null == sdComponent) {
            throw new SetUpException("Use of scattering degree was configured (" + sdType.name() + "), but no "
                + "SD analysis component was passed to " + this.getClass().getName());
        }
        
        config.registerSetting(MetricSettings.CTCR_USAGE_SETTING);
        ctcrType = config.getValue(MetricSettings.CTCR_USAGE_SETTING);
        if (ctcrType != CTCRType.NO_CTCR && null == varModelComponent) {
            throw new SetUpException("Use of cross-tree constraint ratio was configured (" + ctcrType.name() + "), but "
                + "no variability model was passed to " + this.getClass().getName());
        }
        
        config.registerSetting(MetricSettings.LOCATION_DISTANCE_SETTING);
        locationType = config.getValue(MetricSettings.LOCATION_DISTANCE_SETTING);
        if (locationType != FeatureDistanceType.NO_DISTANCE && null == varModelComponent) {
            throw new SetUpException("Use of feature distance was configured (" + locationType.name() + "), but "
                    + "no variability model was passed to " + this.getClass().getName());
        }
        
        config.registerSetting(MetricSettings.TYPE_MEASURING_SETTING);
        varTypeWeightType = config.getValue(MetricSettings.TYPE_MEASURING_SETTING);
        if (varTypeWeightType != VariabilityTypeMeasureType.NO_TYPE_MEASURING) {
            if (null == varModelComponent) {
                throw new SetUpException("Use of feature type weights was configured (" + varTypeWeightType.name()
                    + "), but no variability model was passed to " + this.getClass().getName());
            }
            config.registerSetting(MetricSettings.TYPE_WEIGHTS_SETTING);
            List<String> weights = config.getValue(MetricSettings.TYPE_WEIGHTS_SETTING);
            if (null == weights) {
                throw new SetUpException("Use of feature type weights was configured (" + varTypeWeightType.name()
                    + "), but no weights are defined via " + MetricSettings.TYPE_WEIGHTS_SETTING.getKey());
            }
            
            typeWeights = new HashMap<>();
            for (String weightDef : weights) {
                String[] setting = weightDef.split(":");
                if (setting.length != 2) {
                    throw new SetUpException("Weight definition in unexpected format: " + weightDef);
                }
                Integer weight = 0;
                try {
                    weight = Integer.valueOf(setting[1]);
                } catch (NumberFormatException exc) {
                    throw new SetUpException("Weight of " + setting[0] + " is not an Integer: " + setting[1]);
                }
                typeWeights.put(setting[0], weight);
            }
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
        long time = System.currentTimeMillis();
        
        VariabilityModel varModel = (null != varModelComponent) ? varModelComponent.getNextResult() : null;
        bm = (null != bmComponent) ? bmComponent.getNextResult() : null;
        
        createWeight(varModel);
        
        CodeFunction function;
        V visitor = createVisitor(varModel);
        while ((function = codeFunctionFinder.getNextResult()) != null)  {
            currentCodefile = function.getSourceFile().getPath();
            Function astRoot = function.getFunction();
            visitor.reset();
            astRoot.accept(visitor);
            
            double result = computeResult(visitor);
            if (Double.NaN == result) {
                return;
            }
            
            Function functionAST = function.getFunction();
            File includedFile = currentCodefile.equals(functionAST.getSourceFile())
                ? null : functionAST.getSourceFile();
            addResult(new MetricResult(currentCodefile, includedFile, functionAST.getLineStart(), function.getName(),
                result));
        }
        
        logDuration(System.currentTimeMillis() - time, "Analysis of ", getResultName(), " finished in ");
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
     * Returns the code file, which is currently measured.
     * @return The code file, which is currently measured, won't <tt>null</tt> during the analysis.
     */
    protected final File getCodeFile() {
        return currentCodefile;
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
        
        // Cross-tree constraint ratio
        if (locationType != FeatureDistanceType.NO_DISTANCE && null != varModel) {
            weights.add(new FeatureDistanceWeight(varModel)); 
        }
        
        // Weights for the type of a feature
        if (varTypeWeightType != VariabilityTypeMeasureType.NO_TYPE_MEASURING && null != varModel
            && null != typeWeights) {
            
            weights.add(new TypeWeight(varModel, typeWeights));
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
     * Returns the {@link MetricSettings#SCATTERING_DEGREE_USAGE_SETTING} setting.
     * @return The configured {@link SDType}.
     */
    protected @NonNull SDType getSDType() {
        return sdType;
    }
    
    /**
     * Returns the {@link MetricSettings#CTCR_USAGE_SETTING} setting.
     * @return The configured {@link CTCRType}.
     */
    protected @NonNull CTCRType getCTCRType() {
        return ctcrType;
    }
    
    /**
     * Returns the {@link MetricSettings#LOCATION_DISTANCE_SETTING} setting.
     * @return The configured {@link FeatureDistanceType}.
     */
    protected @NonNull FeatureDistanceType getDistanceType() {
        return locationType;
    }
    
    /**
     * Returns the {@link MetricSettings#TYPE_MEASURING_SETTING} setting.
     * @return The configured {@link VariabilityTypeMeasureType}.
     */
    protected @NonNull VariabilityTypeMeasureType getVarTypeWeightType() {
        return varTypeWeightType;
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
        StringBuffer weightsName = new StringBuffer();
        
        // Scattering Degree
        if (getSDType() != SDType.NO_SCATTERING) {
            weightsName.append(" x ");
            weightsName.append(getSDType().name());
        }
        
        // Cross-Tree Constraint Ratio
        if (getCTCRType() != CTCRType.NO_CTCR) {
            weightsName.append(" x ");
            weightsName.append(getCTCRType().name());
        }
        
        // Feature Distance
        if (getDistanceType() != FeatureDistanceType.NO_DISTANCE) {
            weightsName.append(" x ");
            weightsName.append(getDistanceType().name());
        }
        
        // Feature Types
        if (getVarTypeWeightType() != VariabilityTypeMeasureType.NO_TYPE_MEASURING) {
            weightsName.append(" x ");
            weightsName.append(getVarTypeWeightType().name());
        }
        
        return weightsName.toString();
    }
    
    /**
     * Returns whether this metric has at least one variability weight defined.
     * @return <tt>true</tt> if at least one variability weight is defined, <tt>false</tt> if no variability weight is
     *     defined.
     */
    protected final boolean hasVariabilityWeight() {
        return getSDType() != SDType.NO_SCATTERING || getCTCRType() != CTCRType.NO_CTCR
            || getDistanceType() != FeatureDistanceType.NO_DISTANCE
            || getVarTypeWeightType() != VariabilityTypeMeasureType.NO_TYPE_MEASURING;
    }
    
    /**
     * Logs the duration of a metrics analysis.
     * @param duration The duration in milliseconds.
     * @param text The description to log, will be appended as prefix. Avoid concatenations, this will be done here,
     *     iff the log is printed.
     */
    protected final void logDuration(long duration, @NonNull String... text) {
        // See: https://stackoverflow.com/a/14081915
        String formatedDuration = String.format("%02d:%02d", duration / 60000, duration / 1000 % 60);
        LOGGER.logInfo2((String[]) text, formatedDuration, " Min.");
    }
}
