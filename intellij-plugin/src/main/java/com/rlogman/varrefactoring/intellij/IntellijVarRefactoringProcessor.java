package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.core.VarEligibilityChecker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

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
     * @param editor The editor instance
     */
    public void processFile(@NotNull PsiJavaFile javaFile, @NotNull Editor editor) {
        // Check if Java version supports 'var'
        if (!isVarSupported(javaFile)) {
            return;
        }

        // Find all local variable declarations
        Collection<PsiLocalVariable> localVariables = PsiTreeUtil.findChildrenOfType(javaFile, PsiLocalVariable.class);

        // Process each local variable
        for (PsiLocalVariable variable : localVariables) {
            processVariable(variable);
        }

        // Process for loops if enabled in options
        if (options.isAllowForLoopVars()) {
            Collection<PsiForeachStatement> foreachStatements =
                PsiTreeUtil.findChildrenOfType(javaFile, PsiForeachStatement.class);

            for (PsiForeachStatement foreachStatement : foreachStatements) {
                processForEachVariable(foreachStatement);
            }
        }
    }

    /**
     * Process a local variable declaration.
     */
    private void processVariable(PsiLocalVariable variable) {
        // Skip if already using 'var'
        if (isVarType(variable.getType())) {
            return;
        }

        // Skip if no initializer is present
        PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
            return;
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
            var newVariable = factory.createVariableDeclarationStatement(
                "var",
                variable.getName(),
                initializer
            ).getFirstChild();

            // Replace the old variable with the new one
            variable.replace(newVariable);
        }
    }

    /**
     * Process a for-each loop variable.
     */
    private void processForEachVariable(PsiForeachStatement foreachStatement) {
        PsiParameter loopParameter = foreachStatement.getIterationParameter();

        // Skip if already using 'var'
        if (isVarType(loopParameter.getType())) {
            return;
        }

        // Get the type of the iteration parameter and the collection being iterated
        String parameterType = loopParameter.getType().getCanonicalText();

        PsiExpression iteratedValue = foreachStatement.getIteratedValue();
        if (iteratedValue == null || iteratedValue.getType() == null) {
            return;
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
        }
    }

    /**
     * Check if a variable is part of a for loop.
     */
    private boolean isVariableInForLoop(PsiLocalVariable variable) {
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
    private boolean isVarType(PsiType type) {
        return type.getCanonicalText().equals("var");
    }

    /**
     * Infer the element type from a collection type.
     */
    private String inferElementType(String collectionType) {
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
}
