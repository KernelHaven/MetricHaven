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

import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Specification whether and how to incorporate types of {@link VariabilityVariable}s.
 * @author El-Sharkawy
 *
 */
public enum VariabilityTypeMeasureType {
    NO_TYPE_MEASURING,
    TYPE_WEIGHTS_BY_FILE;

}
