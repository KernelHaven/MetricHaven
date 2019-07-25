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
package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.metric_haven.code_metrics.DLoC.LoFType;
import net.ssehub.kernel_haven.metric_haven.code_metrics.MetricFactory.MetricCreationParameters;
import net.ssehub.kernel_haven.metric_haven.filter_components.CodeFunction;
import net.ssehub.kernel_haven.metric_haven.metric_components.UnsupportedMetricVariationException;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FanInOutVisitor;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionCall;
import net.ssehub.kernel_haven.metric_haven.metric_components.visitors.FunctionMap.FunctionLocation;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.IVariableWeight;
import net.ssehub.kernel_haven.metric_haven.metric_components.weights.NoWeight;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * 
 * EigenVector Centrality metric variations (see <a href="https://doi.org/10.1145/2934466.2934467">
 * Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a>) based on
 * {@link FanInOut} metric variations.<p>
 * 
 * This metric accept exactly the same options than the {@link FanInOut} metric as it used this metric to compute some
 * kind of an adjacency matrix before it sums up the values of all neighbors.
 * 
 * <h2>Design Decisions</h2>
 * There exist multiple alternatives to implement this metric:
 * <ol>
 *   <li>Based on the {@link FunctionMap} the adjacency matrix could be computed and by means of
 *   <a href="http://commons.apache.org/proper/commons-math/">Apache Commons Math</a> we could compute the adjacency
 *   matrix and the eigenvectors.<p>
 *   <b>Pros:</b>
 *   <ul>
 *     <li>This implementation would close at the original definition of the eigenvector centrality metric as
 *   defined in <i>M. E. J. Newman: Networks An introduction</i>.</li>
 *   </ul>
 *   <b>Cons:</b>
 *   <ul>
 *     <li>The variability-aware metric defined by <a href="https://doi.org/10.1145/2934466.2934467">
 *     Do #ifdefs influence the occurrence of vulnerabilities? an empirical study of the Linux kernel paper</a> is
 *     defined the <i>weighted call graph</i>, which is not a pure adjacency matrix.</li>
 *     <li>This would only allow one variation and not all supported variations of the {@link FanInOut} metric.</li>
 *     <li>The original eigenvector centrality metric is problematic on directed graphs and may be undefined if no
 *   eigenvectors exist.</li>
 *   </ul>
 *   
 *   <li>We could compute first all Degree Centrality values in a preprocessing component and compute here the
 *   some kind of adjacency matrix which contains the DV values instead of ones. This follows the idea of
 *   <a href="http://djjr-courses.wikidot.com/soc180:eigenvector-centrality">Social Network Analysis Spring 2012:
 *   Lecture Notes: Eigenvector Centrality</a>.<p>
 *   <b>Pros:</b>
 *   <ul>
 *     <li>Supports all variations of {@link FanInOut} out of the box and has wont cause any matrix-related
 *   problems.</li>
 *     <li>Won't contain negative values.</li>
 *   </ul>
 *   <b>Cons:</b>
 *   <ul>
 *     <li>No real eigenvectors and thus also not normalized.</li>
 *     <li>We need to store all DC values for all metric variations which shall be supported by this metric.
 *     This would be very memory intensive.</li>
 *   </ul>
 *   </li>
 *   
 *   <li>We compute the DC values here and compute the values as described in the previous alternative.<p>
 *   <b>Pros:</b>
 *   <ul>
 *     <li>Supports all variations of {@link FanInOut} out of the box and has wont cause any matrix-related
 *   problems.</li>
 *     <li>Won't contain negative values.</li>
 *     <li>Only DC variations are computed that are required.</li>
 *   </ul>
 *   <b>Cons:</b>
 *   <ul>
 *     <li>No real eigenvectors and thus also not normalized.</li>
 *     <li>Architecture break since we need to compute first all DC values in constructor before the metric can
 *     compute the Eigenvector Centrality values for each function.</li>
 *     <li>Metrics may be computed multiple times, if {@link FanInOut} is also requested with same options.</li>
 *   </ul>
 *   </li>
 * </ol>
 * We choose the third option.
 * 
 * @author El-Sharkawy
 */
public class EigenVectorCentrality extends FanInOut {

    private Map<String, Double> dcValues;

    /**
     * Creates a new {@link EigenVectorCentrality}-metric instance.
     * 
     * @param params The parameters for creating this metric.
     * 
     * @throws UnsupportedMetricVariationException In case that classical code should be measured but a different
     *     {@link IVariableWeight} than {@link NoWeight#INSTANCE} was specified.
     * @throws SetUpException In case the metric specific setting does not match the expected metric setting type,
     *     e.g., {@link LoFType} is used for {@link CyclomaticComplexity}.
     */
    EigenVectorCentrality(@NonNull MetricCreationParameters params)
        throws UnsupportedMetricVariationException, SetUpException {
        
        super(params);
        init();
    }
    
    @Override
    public void prepare() {
        computeDCValues();        
    }
    
    @Override
    public boolean needsPreparation() {
        return true;
    }
    
    /**
     * Computes the degree centrality values by means of {@link FanInOut}. Need to be executed by before this
     * metric starts.
     * TODO SE: This is a memory and more important an performance problem. Here we have massive computation task
     * during the initialization while no threads are created. This needs to be fixed.
     */
    private void computeDCValues() {
        // Default load factor is 0.75 -> if initial capacity is filled to 75% the map rehashes 
        int exptectedCapacity = (int) Math.ceil(1.34 * getFunctions().getAllFunctions().size());
        dcValues = new HashMap<>(exptectedCapacity);
        for (CodeFunction function : getFunctions().getAllFunctions()) {
            Number dcValue = super.computeResult(null, function);
            String id = toPathID(function);
            
            // TODO SE: Check if better solution is possible
            /*
             * There might exist multiple implementations. Here we can differentiate among them based on line numbers.
             * However, we cannot differentiate among them when they are called (there is no line number available
             * for the called function).
             * For this reason we sum all instances up.
             */
            Number oldValue = dcValues.get(id);
            if (null != oldValue) {
                dcValue = oldValue.doubleValue() + dcValue.doubleValue();
            }
            
            dcValues.put(id, dcValue.doubleValue());
        }
    }
    
    /**
     * Computes an ID for the {@link #dcValues} map.
     * @param function The code function for which the ID should be retrieved for.
     * @return A (unique) ID for the {@link #dcValues} map. However line numbers are ignored.
     */
    private String toPathID(CodeFunction function) {
        return toPathID(function.getSourceFile().getPath(), function.getName());
    }
    
    /**
     * Computes an ID for the {@link #dcValues} map.
     * @param funcLocation The function locations for which the ID should be retrieved for.
     * @return A (unique) ID for the {@link #dcValues} map. However line numbers are ignored.
     */
    private String toPathID(FunctionLocation funcLocation) {
        return toPathID(funcLocation.getFile(), funcLocation.getName());
    }
    
    /**
     * Computes an ID for the {@link #dcValues} map.
     * @param path The path where the function is defined in.
     * @param funcName The name of the function.
     * @return A (unique) ID for the {@link #dcValues} map. However line numbers are ignored.
     */
    private String toPathID(File path, String funcName) {
        return path.toString() + ":" + funcName;
    }

    @Override
    protected Number computeResult(@NonNull FanInOutVisitor functionVisitor, CodeFunction func) {
        /*
         * EV is undefined for functions that are not connected.
         * This should be in line with the idea and avoid problems during statistical analysis.
         */
        double result = 0;
        
        // Get DegreeCentrality values for all neighbors and sum the up to compute value for "func"
        String funcID = toPathID(func);
        List<FunctionCall> functionCalls = getFunctions().getFunctionCalls(func.getName());
        if (null != functionCalls) {
            // Avoid counting neighbors twice -> Store already counted neigbors in this set
            Set<String> alreadyProcessed = new HashSet<>();
            for (FunctionCall functionCall : functionCalls) {
                String sourceID = toPathID(functionCall.getSource());
                String targetID = toPathID(functionCall.getTarget());
                //getFanType().isOutMetric() && 
                if (!funcID.equals(sourceID) && !alreadyProcessed.contains(sourceID)) {
                    // Source is a neighbor that calls func as target -> Count value of source
                    result += dcValues.get(sourceID);
                    alreadyProcessed.add(sourceID);
                } else if (!funcID.equals(targetID) && !alreadyProcessed.contains(targetID)) {
                    // Target is a neighbor that is called by func -> Count value of target
                    result += dcValues.get(targetID);
                    alreadyProcessed.add(targetID);
                }
                // Else: Do not count recursive calls
            }
        }
        
        return result;
    }
    
    @Override
    public @NonNull String getMetricName() {
        return "EV " + super.getMetricName();
    }
}
