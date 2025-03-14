package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.intellij.settings.VarRefactoringSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Action that triggers the 'Convert to var' refactoring in IntelliJ IDEA.
 */
public class VarRefactoringAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only for Java files
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        boolean enabled = project != null && file != null &&
                          "java".equals(file.getExtension());

        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        // Get settings from the IDE
        VarRefactoringSettings settings = VarRefactoringSettings.getInstance();
        RefactoringOptions options = convertToRefactoringOptions(settings);

        // Create the processor with IDE-specific implementation
        IntellijVarRefactoringProcessor processor =
            new IntellijVarRefactoringProcessor(project, options);

        // Run the refactoring inside a write command
        WriteCommandAction.runWriteCommandAction(project, "Convert to var", null,
            () -> processor.processFile((PsiJavaFile) psiFile, editor),
            psiFile);
    }

    /**
     * Convert IDE settings to core refactoring options.
     */
    private RefactoringOptions convertToRefactoringOptions(VarRefactoringSettings settings) {
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
