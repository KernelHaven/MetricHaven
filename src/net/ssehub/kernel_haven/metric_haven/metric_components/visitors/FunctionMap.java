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
package net.ssehub.kernel_haven.metric_haven.metric_components.visitors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Container that stores {@link FunctionCall}s for each function.
 *
 * @author Adam
 */
public class FunctionMap {

    /**
     * Represents a function and it's location.
     */
    public static class FunctionLocation {
        
        private @NonNull String name;
        
        private @NonNull File file;
        
        private @NonNull Formula presenceCondition;
        
        private boolean isStub;

        /**
         * Creates a new {@link FunctionLocation}.
         * 
         * @param name The name of the function.
         * @param file The source file of the function.
         * @param presenceCondition The presence condition of the function.
         * @param isStub <tt>true</tt> if this function is a function stub (function without a body), <tt>false</tt> if
         *     the function has a body implementation.
         */
        public FunctionLocation(@NonNull String name, @NonNull File file, @NonNull Formula presenceCondition,
            boolean isStub) {
            
            this.name = name;
            this.file = file;
            this.presenceCondition = presenceCondition;
            this.isStub = isStub;
        }

        /**
         * Returns the name of the function.
         * 
         * @return The name of the function.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the source file of the function.
         * 
         * @return The file of the function.
         */
        public File getFile() {
            return file;
        }

        /**
         * Returns the presence condition of the function.
         * 
         * @return The PC of the function.
         */
        public Formula getPresenceCondition() {
            return presenceCondition;
        }
        
        /**
         * Returns whether this function is a function stub that does not contain a body implementation.
         * @return <tt>true</tt> if this function location points to a function stub, <tt>false</tt> this function has
         *     a body.
         */
        public boolean isStub() {
            return isStub;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
    }
    
    /**
     * Represents a function call from a source function to a target function.
     */
    public static class FunctionCall {
        
        private @NonNull FunctionLocation source;
        
        private @NonNull FunctionLocation target;
        
        private @NonNull Formula callCondition;

        /**
         * Creates a new function call.
         * 
         * @param source The source function.
         * @param target The target function.
         * @param callCondition The surrounding presence condition around the function call
         *        (inside source, around target).
         */
        public FunctionCall(@NonNull FunctionLocation source, @NonNull FunctionLocation target,
            @NonNull Formula callCondition) {
            
            this.source = source;
            this.target = target;
            this.callCondition = callCondition;
        }
        
        /**
         * Returns the source function (i.e. the function that is calling).
         * 
         * @return The source function.
         */
        public FunctionLocation getSource() {
            return source;
        }
        
        /**
         * Returns the target function (i.e. the function that is called).
         * 
         * @return The target function.
         */
        public FunctionLocation getTarget() {
            return target;
        }
        
        /**
         * Returns the condition (not full presence condition) which surround the call of <tt>target</tt> inside
         * <tt>source</tt>.
         * @return The condition surrounding the target call.
         */
        public @NonNull Formula getCallCondition() {
            return callCondition;
        }
        
        @Override
        public String toString() {
            return source + " calls " + target;
        }
        
    }

    private @NonNull Map<String, List<@NonNull FunctionCall>> functionCalls;
    
    /**
     * Creates an empty {@link FunctionMap}.
     */
    public FunctionMap() {
        this.functionCalls = new HashMap<>();
    }
    
    /**
     * Adds a {@link FunctionCall} to this map.
     * 
     * @param call The function call to add.
     */
    public void addFunctionCall(@NonNull FunctionCall call) {
        this.functionCalls.putIfAbsent(call.getSource().getName(), new ArrayList<>());
        this.functionCalls.get(call.getSource().getName()).add(call);
        
        this.functionCalls.putIfAbsent(call.getTarget().getName(), new ArrayList<>());
        this.functionCalls.get(call.getTarget().getName()).add(call);
    }
    
    /**
     * Retrieves all {@link FunctionCall}s where the given function is either source or target.
     * 
     * @param functionName The name of the function to get calls for.
     * 
     * @return A list of {@link FunctionCall}s. <code>null</code> if the given name does not describe a function.
     */
    public @Nullable List<@NonNull FunctionCall> getFunctionCalls(@NonNull String functionName) {
        return this.functionCalls.get(functionName);
    }
    
}
