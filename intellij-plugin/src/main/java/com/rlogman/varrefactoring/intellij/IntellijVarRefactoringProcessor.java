package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.core.VarEligibilityChecker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.util.PsiUtil;

import java.util.Collection;

/**
 * IntelliJ-specific implementation of the var refactoring processor.
 */
public class IntellijVarRefactoringProcessor {
    private final Project project;
    private final RefactoringOptions options;
    private final VarEligibilityChecker eligibilityChecker;

    public IntellijVarRefactoringProcessor(Project project, RefactoringOptions options) {
        this.project = project;
        this.options = options;
        this.eligibilityChecker = new VarEligibilityChecker(options);
    }

    /**
     * Process a Java file for var refactoring.
     *
     * @param javaFile The PSI Java file
     * @param editor The editor instance (can be null for batch processing)
     * @return true if the file was modified, false otherwise
     */
    public boolean processFile(@NotNull PsiJavaFile javaFile, @Nullable Editor editor) {
        boolean wasModified = false;
        // Check if Java version supports 'var'
        if (!isVarSupported(javaFile)) {
            return wasModified;
        }

        // Find all local variable declarations
        Collection<PsiLocalVariable> localVariables = PsiTreeUtil.findChildrenOfType(javaFile, PsiLocalVariable.class);

        // Process each local variable
        for (PsiLocalVariable variable : localVariables) {
            boolean modified = processVariable(variable);
            wasModified = wasModified || modified;
        }

        // Process for loops if enabled in options
        if (options.isAllowForLoopVars()) {
            Collection<PsiForeachStatement> foreachStatements =
                PsiTreeUtil.findChildrenOfType(javaFile, PsiForeachStatement.class);

            for (PsiForeachStatement foreachStatement : foreachStatements) {
                boolean modified = processForEachVariable(foreachStatement);
                wasModified = wasModified || modified;
            }
        }
        
        return wasModified;
    }

    /**
     * Process a local variable declaration.
     * 
     * @param variable The variable to process
     * @return true if the variable was modified
     */
    private boolean processVariable(PsiLocalVariable variable) {
        // Skip if already using 'var'
        if (isVarType(variable.getType())) {
            return false;
        }

        // Skip if no initializer is present
        PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
            return false;
        }

        // Get the declared type and initializer type
        String declaredTypeName = variable.getType().getCanonicalText();
        String initializerTypeName = initializer.getType() != null ?
                                    initializer.getType().getCanonicalText() :
                                    "java.lang.Object";

        // Check if variable is eligible for replacement
        boolean isInForLoop = isVariableInForLoop(variable);

        if (eligibilityChecker.isEligibleForVarReplacement(
                declaredTypeName,
                initializerTypeName,
                true,
                isInForLoop)) {

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            // Create new variable with 'var' type
            var varType = PsiType.getTypeByName("var", project, variable.getResolveScope());
            var newVariable = factory.createVariableDeclarationStatement(
                    variable.getName(),
                    varType,
                    initializer
            ).getFirstChild();

            // Replace the old variable with the new one
            variable.replace(newVariable);
            return true;
        }
        
        return false;
    }

    /**
     * Process a for-each loop variable.
     * 
     * @param foreachStatement The foreach statement to process
     * @return true if the variable was modified
     */
    private boolean processForEachVariable(PsiForeachStatement foreachStatement) {
        PsiParameter loopParameter = foreachStatement.getIterationParameter();

        // Skip if already using 'var'
        if (isVarType(loopParameter.getType())) {
            return false;
        }

        // Get the type of the iteration parameter and the collection being iterated
        String parameterType = loopParameter.getType().getCanonicalText();

        PsiExpression iteratedValue = foreachStatement.getIteratedValue();
        if (iteratedValue == null || iteratedValue.getType() == null) {
            return false;
        }

        // Try to infer the element type from the collection
        String iteratedValueType = iteratedValue.getType().getCanonicalText();
        String elementType = inferElementType(iteratedValueType);

        if (eligibilityChecker.isEligibleForVarReplacement(
                parameterType,
                elementType,
                true,
                true)) {

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            // Create new parameter with 'var' type
            var newParameter = factory.createParameter(
                loopParameter.getName(),
                PsiType.getTypeByName("var", project, loopParameter.getResolveScope())
            );

            // Replace the old parameter with the new one
            loopParameter.replace(newParameter);
            return true;
        }
        
        return false;
    }

    /**
     * Check if a variable is part of a for loop.
     */
    public boolean isVariableInForLoop(PsiLocalVariable variable) {
        PsiElement parent = variable.getParent();
        while (parent != null) {
            if (parent instanceof PsiForStatement || parent instanceof PsiForeachStatement) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Check if a type is the 'var' type.
     */
    public boolean isVarType(PsiType type) {
        return type.getCanonicalText().equals("var");
    }

    /**
     * Infer the element type from a collection type.
     */
    public String inferElementType(String collectionType) {
        // Simple inference for common collection types
        if (collectionType.startsWith("java.util.List<") ||
            collectionType.startsWith("java.util.ArrayList<") ||
            collectionType.startsWith("java.util.LinkedList<")) {
            return extractGenericType(collectionType);
        } else if (collectionType.startsWith("java.util.Set<") ||
                   collectionType.startsWith("java.util.HashSet<")) {
            return extractGenericType(collectionType);
        } else if (collectionType.startsWith("java.util.Map<")) {
            // For maps, we'd need to determine which generic param (K or V)
            // is being used in the for-each loop
            return "java.lang.Object";
        } else if (collectionType.endsWith("[]")) {
            // Array type
            return collectionType.substring(0, collectionType.length() - 2);
        }

        // Default
        return "java.lang.Object";
    }

    /**
     * Extract the generic type from a generic collection type.
     */
    private String extractGenericType(String genericType) {
        int startIndex = genericType.indexOf('<');
        int endIndex = genericType.lastIndexOf('>');

        if (startIndex != -1 && endIndex != -1) {
            return genericType.substring(startIndex + 1, endIndex);
        }

        return "java.lang.Object";
    }

    /**
     * Check if the current Java file uses a Java version that supports 'var'.
     */
    private boolean isVarSupported(PsiJavaFile javaFile) {
        PsiElement context = javaFile;
        LanguageLevel languageLevel = PsiUtil.getLanguageLevel(context);

        // 'var' was introduced in Java 10
        return languageLevel.isAtLeast(LanguageLevel.JDK_10);
    }
    
    /**
     * Get the refactoring options.
     * 
     * @return The current options
     */
    public RefactoringOptions getOptions() {
        return options;
    }
    
    /**
     * Check if a variable is eligible for replacement with 'var'.
     * 
     * @param declaredType The declared type
     * @param initializerType The initializer type
     * @param isLocal Whether the variable is local
     * @param isInForLoop Whether the variable is in a for loop
     * @return true if eligible for replacement
     */
    public boolean isEligibleForReplacement(String declaredType, String initializerType, 
                                           boolean isLocal, boolean isInForLoop) {
        return eligibilityChecker.isEligibleForVarReplacement(
            declaredType, initializerType, isLocal, isInForLoop);
    }
}
