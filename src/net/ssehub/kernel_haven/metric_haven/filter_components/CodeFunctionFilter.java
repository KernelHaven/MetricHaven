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

import java.util.Deque;
import java.util.LinkedList;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.code_model.ast.BranchStatement;
import net.ssehub.kernel_haven.code_model.ast.CaseStatement;
import net.ssehub.kernel_haven.code_model.ast.Code;
import net.ssehub.kernel_haven.code_model.ast.CodeList;
import net.ssehub.kernel_haven.code_model.ast.Comment;
import net.ssehub.kernel_haven.code_model.ast.CompoundStatement;
import net.ssehub.kernel_haven.code_model.ast.CppBlock;
import net.ssehub.kernel_haven.code_model.ast.CppStatement;
import net.ssehub.kernel_haven.code_model.ast.ErrorElement;
import net.ssehub.kernel_haven.code_model.ast.File;
import net.ssehub.kernel_haven.code_model.ast.Function;
import net.ssehub.kernel_haven.code_model.ast.ICode;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElement;
import net.ssehub.kernel_haven.code_model.ast.ISyntaxElementVisitor;
import net.ssehub.kernel_haven.code_model.ast.Label;
import net.ssehub.kernel_haven.code_model.ast.LoopStatement;
import net.ssehub.kernel_haven.code_model.ast.ReferenceElement;
import net.ssehub.kernel_haven.code_model.ast.SingleStatement;
import net.ssehub.kernel_haven.code_model.ast.SwitchStatement;
import net.ssehub.kernel_haven.code_model.ast.TypeDefinition;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;


/**
 * Aggregates the code model to a function level. Passes along each function wrapped inside a {@link Function}
 * instance.
 *
 * @author Adam
 */
public class CodeFunctionFilter extends AnalysisComponent<CodeFunction> implements ISyntaxElementVisitor {

    public static final @NonNull Setting<@NonNull Boolean> SKIP_ERROR_FUNCTIONS_SETTING
            = new Setting<>("analysis.function_filter.skip_error_functions", Type.BOOLEAN, true, "false",
                    "If set to true, this setting causes the " + CodeFunctionFilter.class.getSimpleName()
                    + " to discard all functions that contain an ErrorElement.");
    
    private @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider;

    private boolean skipErrorFunctions;
    
    private int numSkipped;
    
    private SourceFile<ISyntaxElement> currentFile;
    
    /**
     * Indicates whether we are currently inside a function.
     */
    private boolean insideFunction;
    
    private boolean foundFunction;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param codeModelProvider The component to get the code model from.
     * 
     * @throws SetUpException If reading the configuration fails.
     */
    public CodeFunctionFilter(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> codeModelProvider) throws SetUpException {
        super(config);
        
        config.registerSetting(SKIP_ERROR_FUNCTIONS_SETTING);
        this.skipErrorFunctions = config.getValue(SKIP_ERROR_FUNCTIONS_SETTING);
        
        this.codeModelProvider = codeModelProvider;
    }

    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        SourceFile<?> file;
        while ((file = codeModelProvider.getNextResult()) != null) {
            currentFile = file.castTo(ISyntaxElement.class);
            for (ISyntaxElement b : currentFile) {
                b.accept(this);
            }
            
            progress.processedOne();
        }
        
        if (numSkipped > 0) {
            LOGGER.logInfo("Discarded " + numSkipped + " functions that contained an ErrorElement");
        }
        
        progress.close();
    }

    @Override
    public void visitFunction(Function function) {
        boolean oldInsideFunction = this.insideFunction;
        
        this.insideFunction = true;
        this.foundFunction = false;
        ISyntaxElementVisitor.super.visitFunction(function);
        
        Function result = function;
        
        if (this.foundFunction) {
            LOGGER.logDebug(function.getName() + " contains nested functions; fixing...");
            // we found nested functions inside this function
            // copy this function (without the nested functions)
            // the nested functions already added themselves as a result, so we don't need to consider them
            FunctionCopyMachine copier = new FunctionCopyMachine(function);
            result = copier.getResult();
        }
        
        if (skipErrorFunctions && result.containsErrorElement()) {
            numSkipped++;
        } else {
            addResult(new CodeFunction(result.getName(), result, notNull(currentFile)));
        }
        
        this.insideFunction = oldInsideFunction;
        this.foundFunction = true;
    }
    
    /**
     * A class for copying an existing {@link Function} inside an AST. This also skips all nested functions. It creates
     * a deep copy on the main nesting structure, while {@link ICode} elements are usually not copied.
     */
    private static class FunctionCopyMachine implements ISyntaxElementVisitor {
        
        private @NonNull Deque<@NonNull ISyntaxElement> parentStack;
        
        private @NonNull Function oldFunction;
        
        /**
         * Creates a function copy machine for the given {@link Function}.
         * 
         * @param function The function to copy.
         */
        public FunctionCopyMachine(@NonNull Function function) {
            this.parentStack = new LinkedList<>();
            this.oldFunction = function;
            
            Function newFunction = new Function(oldFunction.getPresenceCondition(), oldFunction.getName(),
                    oldFunction.getHeader());
            copyUsualStuff(oldFunction, newFunction);
            
            parentStack.push(newFunction);
        }
        
        /**
         * Performs the deep copy.
         * 
         * @return The copy of the original function.
         */
        public @NonNull Function getResult() {
            for (ISyntaxElement oldNested : oldFunction) {
                oldNested.accept(this);
            }
            
            if (parentStack.size() != 1 || !(parentStack.peek() instanceof Function)) {
                throw new RuntimeException("Last stack element is unexpectedly not a function; stack corrupt?");
            }
            return (Function) parentStack.peek();
        }

        /**
         * Copies the attributes common for all {@link ISyntaxElement}s.
         * 
         * @param oldElement The old element to copy from.
         * @param newElement The new element to copy to.
         */
        private void copyUsualStuff(@NonNull ISyntaxElement oldElement, @NonNull ISyntaxElement newElement) {
            newElement.setSourceFile(oldElement.getSourceFile());
            newElement.setCondition(oldElement.getCondition());
            newElement.setLineStart(oldElement.getLineStart());
            newElement.setLineEnd(oldElement.getLineEnd());
            newElement.setContainsErrorElement(oldElement.containsErrorElement());
        }
        
        /**
         * Adds the new element to the stack of parents and recurses through the nested elements.
         * 
         * @param oldElement The old element that is copied from.
         * @param newElement The new element that is copied to.
         */
        private void addAndRecurse(@NonNull ISyntaxElement oldElement, @NonNull ISyntaxElement newElement) {
            notNull(parentStack.peek()).addNestedElement(newElement);
            
            parentStack.push(newElement);
            for (ISyntaxElement nested : oldElement) {
                nested.accept(this);
            }
            parentStack.pop();
        }
        
        @Override
        public void visitBranchStatement(@NonNull BranchStatement old) {
            BranchStatement newElement = new BranchStatement(old.getPresenceCondition(),
                    old.getType(), old.getIfCondition());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCaseStatement(@NonNull CaseStatement old) {
            CaseStatement newElement = new CaseStatement(old.getPresenceCondition(),
                    old.getCaseCondition(), old.getType(), old.getSwitchStatement());
            // TODO: reference to switch?
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCode(@NonNull Code old) {
            Code newElement = new Code(old.getPresenceCondition(), old.getText());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCodeList(@NonNull CodeList old) {
            CodeList newElement = new CodeList(old.getPresenceCondition());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitComment(@NonNull Comment old) {
            Comment newElement = new Comment(old.getPresenceCondition(), old.getComment());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCompoundStatement(@NonNull CompoundStatement old) {
            CompoundStatement newElement = new CompoundStatement(old.getPresenceCondition());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCppBlock(@NonNull CppBlock old) {
            CppBlock newElement = new CppBlock(old.getPresenceCondition(), old.getCondition(),
                old.getCurrentCondition(), old.getType());
            // TODO: siblings?
            for (int i = 0; i < old.getSiblingCount(); i++) {
                newElement.addSibling(old.getSibling(i));
            }
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitCppStatement(@NonNull CppStatement old) {
            CppStatement newElement = new CppStatement(old.getPresenceCondition(), old.getType(), old.getExpression());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitErrorElement(@NonNull ErrorElement old) {
            ErrorElement newElement = new ErrorElement(old.getPresenceCondition(), old.getErrorText());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitFile(@NonNull File old) {
            File newElement = new File(old.getPresenceCondition(), old.getPath());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitFunction(@NonNull Function old) {
            // ignore nested functions
        }
        
        @Override
        public void visitLabel(@NonNull Label old) {
            Label newElement = new Label(old.getPresenceCondition(), old.getCode());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitLoopStatement(@NonNull LoopStatement old) {
            LoopStatement newElement = new LoopStatement(old.getPresenceCondition(), old.getLoopCondition(),
                    old.getLoopType());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitSingleStatement(@NonNull SingleStatement old) {
            SingleStatement newElement = new SingleStatement(old.getPresenceCondition(), old.getCode(), old.getType());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitSwitchStatement(@NonNull SwitchStatement old) {
            SwitchStatement newElement = new SwitchStatement(old.getPresenceCondition(), old.getHeader());
            // TODO: children?
            for (int i = 0; i < old.getCasesCount(); i++) {
                newElement.addCase(old.getCase(i));
            }
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitTypeDefinition(@NonNull TypeDefinition old) {
            TypeDefinition newElement = new TypeDefinition(old.getPresenceCondition(), old.getDeclaration(),
                    old.getType());
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
        @Override
        public void visitReference(@NonNull ReferenceElement old) {
            ReferenceElement newElement = new ReferenceElement(old.getPresenceCondition(), old.getReferenced());
            // TODO: referenced element?
            
            copyUsualStuff(old, newElement);
            addAndRecurse(old, newElement);
        }
        
    }

    @Override
    public @NonNull String getResultName() {
        return "Code Functions";
    }

}
