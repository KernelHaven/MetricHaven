package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.metric_haven.filter_components.ScatteringDegreeContainer;
import net.ssehub.kernel_haven.metric_haven.metric_components.SDType;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Counts the number of  <tt>Number of internal/external configuration options</tt> as defined in
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class UsedVariabilityVarsVisitor extends AbstractFunctionVisitor {

    private @NonNull Set<@NonNull String> externalVars = new HashSet<>();
    private @NonNull Set<@NonNull String> internalVars = new HashSet<>();
    private @Nullable Set<@NonNull String> varModelVars;
    
    /**
     * Sole constructor.
     * @param varModel Optional, if not <tt>null</tt> for each variable used in a
     *     {@link net.ssehub.kernel_haven.code_model.ast.ISyntaxElement#getPresenceCondition()},
     *     whether it is defined in the variability model.
     */
    public UsedVariabilityVarsVisitor(@Nullable VariabilityModel varModel) {
        super(varModel);
        varModelVars = (null != varModel) ? Collections.unmodifiableSet(varModel.getVariableMap().keySet()) : null;
    }

    @Override
    public void visitFunction(@NonNull Function function) {
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(function.getPresenceCondition());
        for (int i = varFinder.getVariableNames().size() - 1; i >= 0; i--) {
            String symbolName = notNull(varFinder.getVariableNames().get(i));
            if (isVarModelVariable(symbolName)) {
                externalVars.add(symbolName);
            }
        }
        
        super.visitFunction(function);
    }
    
    @Override
    public void visitCppBlock(@NonNull CppBlock block) {
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(block.getPresenceCondition());
        for (int i = varFinder.getVariableNames().size() - 1; i >= 0; i--) {
            String symbolName = notNull(varFinder.getVariableNames().get(i));
            if (isVarModelVariable(symbolName)) {
                internalVars.add(symbolName);
            }
        }
        
        super.visitCppBlock(block);
    }
    
    /**
     * Returns whether the specified symbol name is known by the variability model.
     * @param symbolName The variable name to check.
     * @return <tt>true</tt> if the variable is known by the variability model or if no model was passed to the
     *     constructor, <tt>false</tt> otherwise.
     */
    private boolean isVarModelVariable(@NonNull String symbolName) {
        return (null != varModelVars) ? varModelVars.contains(symbolName) : true;
    }
    
    /**
     * Returns the number of variables used in the presence condition to include the complete function.
     * @param sdConsideration The configured SD option.
     * @param sdList Must not be <tt>null</tt> if SD shall be considered.
     * 
     * @return The number of variables used in the presence condition to include the complete function (&gt; 0)
     */
    public int externalVarsSize(@NonNull SDType sdConsideration, ScatteringDegreeContainer sdList) {
        return computeResult(externalVars, sdConsideration, sdList);
    }
    
    /**
     * Returns the number of variables used inside the function.
     * @param sdConsideration The configured SD option.
     * @param sdList Must not be <tt>null</tt> if SD shall be considered.
     * 
     * @return The number of variables used inside the function (&gt; 0)
     */
    public int internalVarsSize(@NonNull SDType sdConsideration, ScatteringDegreeContainer sdList) {
        return computeResult(internalVars, sdConsideration, sdList);
    }
    
    
    /**
     * Returns the number of the union of the external and external variables.
     * @param sdConsideration The configured SD option.
     * @param sdList Must not be <tt>null</tt> if SD shall be considered.
     * 
     * @return The number of (external variables &cup; internal variables) (&gt; 0)
     */
    public int allVarsSize(@NonNull SDType sdConsideration, ScatteringDegreeContainer sdList) {
        Set<String> superSet = new HashSet<>();
        superSet.addAll(internalVars);
        superSet.addAll(externalVars);
        
        return computeResult(superSet, sdConsideration, sdList);
    }
    
    /**
     * Computes the result depending on the selected scattering degree option.
     * @param collectedVars The collected variables by this visitor.
     * @param sdConsideration The configured SD option.
     * @param sdList Must not be <tt>null</tt> if SD shall be considered.
     * @return The number of counted variables multiplied with the selected SD option (will be &gt; 0).
     */
    private int computeResult(@NonNull Set<String> collectedVars, @NonNull SDType sdConsideration,
        ScatteringDegreeContainer sdList) {
        
        int result = 0;
        switch (sdConsideration) {
        case NO_SCATTERING:
            result = collectedVars.size();
            break;
        case SD_VP:
            for (String variable : collectedVars) {
                result += sdList.getSDVariationPoint(variable);
            }
            break;
        case SD_FILE:
            for (String variable : collectedVars) {
                result += sdList.getSDFile(variable);
            }
            break;
        default:
            result = collectedVars.size();
            Logger.get().logError("Unhandled scattering degree option passed to " + getClass().getSimpleName() + ": "
                + sdConsideration.name());
            break;
        }
        
        return result;
    }

    @Override
    public void reset() {
        super.reset();
        internalVars.clear();
        externalVars.clear();
    }
}
