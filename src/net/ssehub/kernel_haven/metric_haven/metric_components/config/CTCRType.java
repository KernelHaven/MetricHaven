/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
