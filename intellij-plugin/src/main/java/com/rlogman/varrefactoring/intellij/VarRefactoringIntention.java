package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.core.VarEligibilityChecker;
import com.rlogman.varrefactoring.intellij.settings.VarRefactoringSettings;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Intention action to convert a single variable to use 'var'.
 */
public class VarRefactoringIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    private final VarEligibilityChecker eligibilityChecker;

    public VarRefactoringIntention() {
        this.eligibilityChecker = new VarEligibilityChecker(
                convertToRefactoringOptions(VarRefactoringSettings.getInstance())
        );
        setText("Convert to 'var'");
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Java Var Refactoring";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // Check if we're in a Java file
        if (!(element.getContainingFile() instanceof PsiJavaFile)) {
            return false;
        }

        // Check if Java version supports 'var'
        LanguageLevel languageLevel = PsiUtil.getLanguageLevel(element);
        if (!languageLevel.isAtLeast(LanguageLevel.JDK_10)) {
            return false;
        }

        // Find the local variable declaration
        PsiLocalVariable variable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
        if (variable == null) {
            // Also check for foreach parameter
            PsiForeachStatement foreachStatement =
                PsiTreeUtil.getParentOfType(element, PsiForeachStatement.class);
            if (foreachStatement != null && foreachStatement.getIterationParameter() != null) {
                return isEligibleForEachParameter(foreachStatement.getIterationParameter());
            }
            return false;
        }

        return isEligibleVariable(variable);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {

        // Try to find a local variable first
        PsiLocalVariable variable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
        if (variable != null) {
            replaceVariableWithVar(project, variable);
            return;
        }

        // Check for foreach parameter
        PsiForeachStatement foreachStatement =
            PsiTreeUtil.getParentOfType(element, PsiForeachStatement.class);
        if (foreachStatement != null && foreachStatement.getIterationParameter() != null) {
            replaceForeachParameterWithVar(project, foreachStatement.getIterationParameter());
        }
    }

    /**
     * Replace a local variable type with 'var'.
     */
    private void replaceVariableWithVar(Project project, PsiLocalVariable variable) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Create a new declaration with 'var'
        String name = variable.getName();
        PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
            return;
        }

        var varType = PsiType.getTypeByName("var", project, variable.getResolveScope());
        var newVariable = factory.createVariableDeclarationStatement(
                variable.getName(),
                varType,
                initializer
        ).getFirstChild();

        variable.replace(newVariable);
    }

    /**
     * Replace a foreach loop parameter type with 'var'.
     */
    private void replaceForeachParameterWithVar(Project project, PsiParameter parameter) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Create new parameter with 'var' type
        var newParameter = factory.createParameter(
            parameter.getName(),
            PsiType.getTypeByName("var", project, parameter.getResolveScope())
        );

        parameter.replace(newParameter);
    }

    /**
     * Check if a variable is eligible for var conversion.
     */
    private boolean isEligibleVariable(PsiLocalVariable variable) {
        // Check if already using 'var'
        if (isVarType(variable.getType())) {
            return false;
        }

        // Must have an initializer
        PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
            return false;
        }

        // Get types
        String declaredType = variable.getType().getCanonicalText();
        String initializerType = initializer.getType() != null ?
                                initializer.getType().getCanonicalText() :
                                "java.lang.Object";

        boolean isInForLoop = isVariableInForLoop(variable);

        return eligibilityChecker.isEligibleForVarReplacement(
            declaredType,
            initializerType,
            true,
            isInForLoop
        );
    }

    /**
     * Check if a foreach parameter is eligible for var conversion.
     */
    private boolean isEligibleForEachParameter(PsiParameter parameter) {
        // Must not already be using 'var'
        if (isVarType(parameter.getType())) {
            return false;
        }

        // Get parent foreach statement
        PsiForeachStatement foreachStatement =
            PsiTreeUtil.getParentOfType(parameter, PsiForeachStatement.class);
        if (foreachStatement == null) {
            return false;
        }

        // Get iterated value
        PsiExpression iteratedValue = foreachStatement.getIteratedValue();
        if (iteratedValue == null || iteratedValue.getType() == null) {
            return false;
        }

        // Get types
        String parameterType = parameter.getType().getCanonicalText();
        String iteratedValueType = iteratedValue.getType().getCanonicalText();
        String elementType = inferElementType(iteratedValueType);

        return eligibilityChecker.isEligibleForVarReplacement(
            parameterType,
            elementType,
            true,
            true
        );
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
            collectionType.startsWith("java.util.ArrayList<")) {
            return extractGenericType(collectionType);
        } else if (collectionType.startsWith("java.util.Set<") ||
                   collectionType.startsWith("java.util.HashSet<")) {
            return extractGenericType(collectionType);
        } else if (collectionType.endsWith("[]")) {
            // Array type
            return collectionType.substring(0, collectionType.length() - 2);
        }

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
     * Convert IDE settings to core refactoring options.
     */
    private static RefactoringOptions convertToRefactoringOptions(VarRefactoringSettings settings) {
        return new RefactoringOptions(
            settings.isAllowPrimitiveTypes(),
            settings.isAllowForLoopVars(),
            settings.isAllowDiamondOperator(),
            settings.isAllowDifferentTypes(),
            settings.isRefactorAnonymousClasses(),
            settings.isRefactorLambdaExpressions()
        );
    }
}
