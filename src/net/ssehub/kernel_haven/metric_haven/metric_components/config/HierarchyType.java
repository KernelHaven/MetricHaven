package net.ssehub.kernel_haven.metric_haven.metric_components.config;

import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 *
 * Specification whether and how to incorporate hierarchy types of {@link VariabilityVariable}s.
 * @author El-Sharkawy
 *
 */
public enum HierarchyType {
    NO_HIERARCHY_MEASURING,
    HIERARCHY_WEIGHTS_BY_FILE,
    HIERARCHY_WEIGHTS_BY_LEVEL;

}
