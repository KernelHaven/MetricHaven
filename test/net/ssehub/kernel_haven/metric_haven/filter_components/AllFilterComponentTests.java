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
package net.ssehub.kernel_haven.metric_haven.filter_components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeReaderTest;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.ScatteringDegreeWriterTest;
import net.ssehub.kernel_haven.metric_haven.filter_components.scattering_degree.VariabilityCounterTest;

/**
 * All test classes for filter components.
 * 
 * @author Adam
 */
@RunWith(Suite.class)
@SuiteClasses({
    VariabilityCounterTest.class,
    ScatteringDegreeReaderTest.class,
    ScatteringDegreeWriterTest.class,
    })
public class AllFilterComponentTests {

}
