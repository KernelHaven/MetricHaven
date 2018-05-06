package net.ssehub.kernel_haven.metric_haven.metric_components.config;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Specification whether and how to incorporate cross-tree constraint ratios.
 * @author El-Sharkawy
 *
 */
public enum CTCRType {
    NO_CTCR("No CTCR"),
    OUTGOING_CONNECTIONS("Outgoing CTCR"),
    INCOMIG_CONNECTIONS("Incomming CTCR"),
    ALL_CTCR("Full CTCR");
    
    private @NonNull String shortName;
    
    /**
     * Sole constructor.
     * @param shortName The short description to be used in output.
     */
    private CTCRType(@NonNull String shortName) {
        this.shortName = shortName;
    }
    
    /**
     * Returns the short name of the type.
     * @return A name to be used when creating results.
     */
    public @NonNull String getShortName() {
        return shortName;
    }
}
