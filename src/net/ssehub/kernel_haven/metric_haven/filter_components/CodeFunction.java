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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;

import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Holds a function from the code model.
 *  
 * @author Adam
 */
public class CodeFunction {
    
    private @NonNull String name;
    
    private @NonNull Function function;

    private @NonNull SourceFile<ISyntaxElement> file;
    
    private @NonNull String id;

    /**
     * Creates a {@link CodeFunction}.
     * 
     * @param name The name of the function.
     * @param function The function that should be held by this object.
     * @param file The C-file containing the function directly or indirectly via an included H-file.
     */
    public CodeFunction(@NonNull String name, @NonNull Function function, @NonNull SourceFile<ISyntaxElement> file) {
        this.name = name;
        this.function = function;
        this.file = file;
        
        File functionSource = function.getSourceFile();
        File fileSource = file.getPath();
        if (!functionSource.equals(fileSource)) {
            id = fileSource.getPath() + ":" + functionSource.getPath();
        } else {
            id = notNull(fileSource.getPath());
        }
    }

    /**
     * Retrieves the name of this function.
     * 
     * @return The name of this function. Never <code>null</code>.
     */
    public @NonNull String getName() {
        return name;
    }

    /**
     * Retrieves the function held in this object.
     * 
     * @return The function. Never <code>null</code>.
     */
    public @NonNull Function getFunction() {
        return function;
    }
    
    /**
     * Returns the (main) source file containing the function. <br/>
     * <b>Please note:</b> This is not necessary identical to {@link ISyntaxElement#getSourceFile()} as this
     * points to the direct location of an (included) source file.
     * @return The location of the C-file (not of an included H-file of a macro).
     */
    public @NonNull SourceFile<ISyntaxElement> getSourceFile() {
        return file;
    }
    
    /**
     * Returns the full qualified name of the function, which considers the path of the C-file and included
     * H-files if existent.
     * @return The full qualified name of the function.
     */
    public @NonNull String getQualifiedName() {
        return id;
    }
}
