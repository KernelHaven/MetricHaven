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
package net.ssehub.kernel_haven.metric_haven;

import java.io.File;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Represents the result of a metric execution.
 * 
 * @author Adam
 * @author Sascha El-Sharkawy
 */
@TableRow
public class MetricResult {
    
//    private static int instances = 0;

    private @NonNull String element;
    
    private double value;
    
    private @Nullable File sourceFile;
    private @Nullable File includedFile;
    private int lineNo;

    /**
     * Creates a new metric execution result.
     * 
     * @param sourceFile The main source file of containing the measured element (e.g., the C-file).
     *     May be <tt>null</tt>.
     * @param includedFile The (intermediate) source file of containing the measured element (e.g., the H-file).
     *     May be <tt>null</tt>.
     * @param lineNo The starting line number of the measured element, <tt>-1</tt> if unknown.
     * @param element The measured element (i.e. the thing that the metric was execute on) of the result. Must not be
     *      <code>null</code>.
     * @param value The value that the metric returned.
     */
    public MetricResult(@Nullable File sourceFile, @Nullable File includedFile, int lineNo,  @NonNull String element,
        double value) {
        
        this.sourceFile = sourceFile;
        this.includedFile = includedFile;
        this.lineNo = lineNo;
        this.element = element;
        this.value = value;
    }
    
    /**
     * Returns the main source file containing the measured element.
     * 
     * @return The path to the source file (e.g., a C-File). May be <code>null</code>.
     */
    @TableElement(name = "Source File", index = 0)
    public @Nullable File getSourceFile() {
        return sourceFile;
    }
    
    /**
     * Returns the (included) intermediated source file containing the measured element.
     * 
     * @return The path to the included file (e.g., a H-File). May be <code>null</code>.
     */
    @TableElement(name = "Include File", index = 1)
    public @Nullable File getIncludedFile() {
        return includedFile;
    }
    
    /**
     * Returns the beginning line number of the measured element.
     * 
     * @return The beginning line number of the measured element, maybe <tt>-1</tt> if it could not be determined.
     */
    @TableElement(name = "Line", index = 2)
    public int getLine() {
        return lineNo;
    }
    
    /**
     * Returns the measured element name, that is the thing that the metric was executed on.
     * For example, this could be the code function name.
     * 
     * @return The mesured element of this result. Never <code>null</code>.
     */
    @TableElement(name = "Element", index = 3)
    public @NonNull String getContext() {
        return element;
    }
    
    /**
     * Returns the value that the metric calculated.
     * 
     * @return The value of this result.
     */
    @TableElement(name = "Value", index = 4)
    public double getValue() {
        return value;
    }
    
    @Override
    public @NonNull String toString() {
        return element + ": " + value;
    }
    
//    @Override
//    protected void finalize() throws Throwable {
//        synchronized (MetricResult.class) {
//            instances--;
//            if (instances % 100000 == 0) {
//                Logger.get().logInfo2(instances, " Metric results");
//            }
//        }
//        super.finalize();
//    }

}
