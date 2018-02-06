package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Counts the number of  <tt>Number of internal/external configuration options</tt> as defined in
 * <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>.
 * @author El-Sharkawy
 *
 */
public class UsedVariabilityVarsVisitor extends AbstractFunctionVisitor {

    private Set<String> externalVars = new HashSet<>();
    private Set<String> internalVars = new HashSet<>();
    private Set<String> varModelVars;
    
    /**
     * Sole constructor.
     * @param varModel Optional, if not <tt>null</tt> for each variable used in a
     *     {@link net.ssehub.kernel_haven.code_model.ast.ISyntaxElement#getPresenceCondition()},
     *     whether it is defined in the variability model.
     */
    public UsedVariabilityVarsVisitor(VariabilityModel varModel) {
        super(varModel);
        varModelVars = (null != varModel) ? Collections.unmodifiableSet(varModel.getVariableMap().keySet()) : null;
    }

    @Override
    public void visitFunction(Function function) {
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(function.getPresenceCondition());
        for (int i = varFinder.getVariableNames().size() - 1; i >= 0; i--) {
            String symbolName = varFinder.getVariableNames().get(i);
            if (isVarModelVariable(symbolName)) {
                externalVars.add(symbolName);
            }
        }
        
        super.visitFunction(function);
    }
    
    @Override
    public void visitCppBlock(CppBlock block) {
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(block.getPresenceCondition());
        for (int i = varFinder.getVariableNames().size() - 1; i >= 0; i--) {
            String symbolName = varFinder.getVariableNames().get(i);
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
    private boolean isVarModelVariable(String symbolName) {
        return (null != varModelVars) ? varModelVars.contains(symbolName) : true;
    }
    
    /**
     * Returns the number of variables used in the presence condition to include the complete function.
     * @return The number of variables used in the presence condition to include the complete function (&gt; 0)
     */
    public int externalVarsSize() {
        return externalVars.size();
    }
    
    /**
     * Returns the number of variables used inside the function.
     * @return The number of variables used inside the function (&gt; 0)
     */
    public int internalVarsSize() {
        return externalVars.size();
    }
    
    
    /**
     * Returns the number of the union of the external and external variables.
     * @return The number of (external variables &cup; internal variables) (&gt; 0)
     */
    public int allVarsSize() {
        Set<String> superSet = new HashSet<>();
        superSet.addAll(internalVars);
        superSet.addAll(externalVars);
        
        return superSet.size();
    }
}
