package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.intellij.settings.VarRefactoringSettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Action that triggers the 'Convert to var' refactoring in IntelliJ IDEA.
 */
public class VarRefactoringAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action for Java files, packages, or directories that might contain Java files
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Get the selection from the data context
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null || files.length == 0) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Check if any of the selected items are Java files, directories, or packages
        boolean hasJavaContent = false;
        for (VirtualFile file : files) {
            if (file.isDirectory() || "java".equals(file.getExtension())) {
                hasJavaContent = true;
                break;
            }
        }

        e.getPresentation().setEnabledAndVisible(hasJavaContent);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        VirtualFile[] virtualFiles = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        
        // Get current editor in case we're processing a single file
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        // Get settings from the IDE
        VarRefactoringSettings settings = VarRefactoringSettings.getInstance();
        RefactoringOptions options = convertToRefactoringOptions(settings);

        // Create the processor with IDE-specific implementation
        IntellijVarRefactoringProcessor processor =
            new IntellijVarRefactoringProcessor(project, options);

        // Collect all Java files to process
        List<PsiJavaFile> javaFiles = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        
        // Show confirmation and preview for batch processing
        if (virtualFiles.length > 1 || (virtualFiles.length == 1 && virtualFiles[0].isDirectory())) {
            String message = "Do you want to convert explicit types to 'var' in ";
            if (virtualFiles.length == 1 && virtualFiles[0].isDirectory()) {
                message += "all Java files under '" + virtualFiles[0].getName() + "'?";
            } else {
                message += "these " + virtualFiles.length + " selected items?";
            }
            
            int result = Messages.showYesNoCancelDialog(
                project, 
                message, 
                "Convert to 'var'", 
                "Preview Changes", 
                "Convert", 
                "Cancel",
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.CANCEL) {
                return;
            }
            
            // If "Preview Changes" was selected
            boolean previewMode = (result == Messages.NO);
            
            // Process files in a background task with progress indicator
            ProgressManager.getInstance().run(new Task.Backgroundable(project, 
                    previewMode ? "Analyzing changes..." : "Converting to 'var'", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    List<PsiJavaFile> files = collectJavaFiles(project, virtualFiles, indicator);
                    indicator.setIndeterminate(false);
                    indicator.setText(previewMode ? "Analyzing Java files..." : "Processing Java files...");
                    
                    AtomicInteger processedFiles = new AtomicInteger(0);
                    AtomicInteger modifiedFiles = new AtomicInteger(0);
                    
                    // For preview mode, collect potential changes
                    List<String> changeDescriptions = new ArrayList<>();
                    
                    if (previewMode) {
                        // In preview mode, we only analyze potential changes
                        for (PsiJavaFile file : files) {
                            indicator.checkCanceled();
                            indicator.setText2("Analyzing " + file.getName());
                            indicator.setFraction((double) processedFiles.incrementAndGet() / files.size());
                            
                            // Use our processor to check if the file would be modified
                            List<String> fileChanges = previewChanges(project, processor, file);
                            if (!fileChanges.isEmpty()) {
                                modifiedFiles.incrementAndGet();
                                // Add file name as header
                                changeDescriptions.add("<b>" + file.getName() + "</b>");
                                // Add all potential changes in this file
                                changeDescriptions.addAll(fileChanges);
                                changeDescriptions.add(""); // Empty line for separation
                            }
                        }
                        
                        // Show preview dialog
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (changeDescriptions.isEmpty()) {
                                Messages.showInfoMessage(
                                    project,
                                    "No eligible variables found for conversion to 'var'.",
                                    "Convert to 'var' Preview"
                                );
                            } else {
                                // Create HTML content for preview
                                StringBuilder previewContent = new StringBuilder();
                                previewContent.append("<html><body>");
                                previewContent.append("<h3>Changes to be made:</h3>");
                                previewContent.append("<ul>");
                                
                                for (String change : changeDescriptions) {
                                    if (change.startsWith("<b>")) {
                                        // File header
                                        if (previewContent.toString().contains("</li>")) {
                                            previewContent.append("</ul></li>");
                                        }
                                        previewContent.append("<li>").append(change).append("<ul>");
                                    } else if (!change.isEmpty()) {
                                        // Change description
                                        previewContent.append("<li>").append(change).append("</li>");
                                    }
                                }
                                
                                previewContent.append("</ul></li></ul>");
                                previewContent.append("<p><b>Total: ").append(modifiedFiles.get())
                                               .append(" files with changes</b></p>");
                                previewContent.append("</body></html>");
                                
                                // Show the preview dialog with option to proceed or cancel
                                int confirmResult = Messages.showYesNoDialog(
                                    project,
                                    previewContent.toString(),
                                    "Convert to 'var' Preview",
                                    "Apply Changes",
                                    "Cancel",
                                    Messages.getInformationIcon()
                                );
                                
                                if (confirmResult == Messages.YES) {
                                    // User confirmed, now apply changes
                                    applyChanges(project, processor, files);
                                }
                            }
                        });
                    } else {
                        // In normal mode, apply changes directly
                        for (PsiJavaFile file : files) {
                            indicator.checkCanceled();
                            indicator.setText2("Processing " + file.getName());
                            indicator.setFraction((double) processedFiles.incrementAndGet() / files.size());
                            
                            boolean modified = processFile(project, processor, file);
                            if (modified) {
                                modifiedFiles.incrementAndGet();
                            }
                        }
                        
                        // Show a summary message
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage(
                                project,
                                "Processed " + processedFiles.get() + " files.\n" +
                                "Modified " + modifiedFiles.get() + " files.",
                                "Convert to 'var' Completed"
                            );
                        });
                    }
                }
            });
        } else if (virtualFiles.length == 1) {
            // Single file processing
            VirtualFile virtualFile = virtualFiles[0];
            PsiFile psiFile = psiManager.findFile(virtualFile);
            
            if (psiFile instanceof PsiJavaFile) {
                // Process single file directly
                WriteCommandAction.runWriteCommandAction(project, "Convert to var", null,
                    () -> processor.processFile((PsiJavaFile) psiFile, editor),
                    psiFile);
            }
        }
    }
    
    /**
     * Process a single Java file within a write command.
     * 
     * @param project The project
     * @param processor The refactoring processor
     * @param file The Java file to process
     * @return true if the file was modified
     */
    private boolean processFile(Project project, IntellijVarRefactoringProcessor processor, PsiJavaFile file) {
        // We need to track if the file was modified
        final boolean[] modified = {false};
        
        WriteCommandAction.runWriteCommandAction(project, "Convert to var", null, () -> {
            // Editor is null in batch mode, which is ok for processing
            modified[0] = processor.processFile(file, null);
        }, file);
        
        return modified[0];
    }
    
    /**
     * Preview changes that would be made to a file without actually making them.
     * 
     * @param project The project
     * @param processor The processor that does the refactoring
     * @param file The file to analyze
     * @return A list of change descriptions
     */
    private List<String> previewChanges(Project project, IntellijVarRefactoringProcessor processor, PsiJavaFile file) {
        // Create a temporary copy of the file to simulate changes
        final List<String> changes = new ArrayList<>();
        
        // Use Application.runReadAction to ensure PSI operations are safe
        ApplicationManager.getApplication().runReadAction(() -> {
            // Find all local variable declarations that would be converted
            Collection<PsiLocalVariable> localVariables = PsiTreeUtil.findChildrenOfType(file, PsiLocalVariable.class);
            
            for (PsiLocalVariable variable : localVariables) {
                // Skip if already using 'var'
                if (processor.isVarType(variable.getType())) {
                    continue;
                }
                
                // Skip if no initializer is present
                PsiExpression initializer = variable.getInitializer();
                if (initializer == null) {
                    continue;
                }
                
                // Get the declared type and initializer type
                String declaredTypeName = variable.getType().getCanonicalText();
                String initializerTypeName = initializer.getType() != null ?
                                          initializer.getType().getCanonicalText() :
                                          "java.lang.Object";
                
                // Check if variable is eligible for replacement
                boolean isInForLoop = processor.isVariableInForLoop(variable);
                
                if (processor.isEligibleForReplacement(
                        declaredTypeName,
                        initializerTypeName,
                        true,
                        isInForLoop)) {
                    
                    // Create a description of the change
                    String location = "";
                    PsiMethod method = PsiTreeUtil.getParentOfType(variable, PsiMethod.class);
                    if (method != null) {
                        location = " in method '" + method.getName() + "'";
                    }
                    
                    changes.add("Replace '" + declaredTypeName + " " + variable.getName() + 
                              "' with 'var " + variable.getName() + "'" + location);
                }
            }
            
            // Preview for-each loop variables if applicable
            if (processor.getOptions().isAllowForLoopVars()) {
                Collection<PsiForeachStatement> foreachStatements =
                    PsiTreeUtil.findChildrenOfType(file, PsiForeachStatement.class);
                
                for (PsiForeachStatement foreachStatement : foreachStatements) {
                    PsiParameter loopParameter = foreachStatement.getIterationParameter();
                    
                    // Skip if already using 'var'
                    if (processor.isVarType(loopParameter.getType())) {
                        continue;
                    }
                    
                    // Get the iteration parameter type
                    String parameterType = loopParameter.getType().getCanonicalText();
                    
                    PsiExpression iteratedValue = foreachStatement.getIteratedValue();
                    if (iteratedValue == null || iteratedValue.getType() == null) {
                        continue;
                    }
                    
                    // Try to infer the element type from the collection
                    String iteratedValueType = iteratedValue.getType().getCanonicalText();
                    String elementType = processor.inferElementType(iteratedValueType);
                    
                    if (processor.isEligibleForReplacement(
                            parameterType,
                            elementType,
                            true,
                            true)) {
                        
                        // Create a description of the change
                        String location = "";
                        PsiMethod method = PsiTreeUtil.getParentOfType(foreachStatement, PsiMethod.class);
                        if (method != null) {
                            location = " in method '" + method.getName() + "'";
                        }
                        
                        changes.add("Replace for-each parameter '" + parameterType + " " + loopParameter.getName() + 
                                  "' with 'var " + loopParameter.getName() + "'" + location);
                    }
                }
            }
        });
        
        return changes;
    }
    
    /**
     * Apply changes to a list of files.
     * 
     * @param project The project
     * @param processor The processor
     * @param files The list of files to process
     */
    private void applyChanges(Project project, IntellijVarRefactoringProcessor processor, List<PsiJavaFile> files) {
        // Run in a background task with progress
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Applying changes", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Processing files...");
                
                AtomicInteger processedFiles = new AtomicInteger(0);
                AtomicInteger modifiedFiles = new AtomicInteger(0);
                
                for (PsiJavaFile file : files) {
                    indicator.checkCanceled();
                    indicator.setText2("Processing " + file.getName());
                    indicator.setFraction((double) processedFiles.incrementAndGet() / files.size());
                    
                    boolean modified = processFile(project, processor, file);
                    if (modified) {
                        modifiedFiles.incrementAndGet();
                    }
                }
                
                // Show a summary message
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(
                        project,
                        "Processed " + processedFiles.get() + " files.\n" +
                        "Modified " + modifiedFiles.get() + " files.",
                        "Convert to 'var' Completed"
                    );
                });
            }
        });
    }
    
    /**
     * Recursively collect all Java files from the selected virtual files.
     * 
     * @param project The project
     * @param virtualFiles The selected virtual files
     * @param indicator Progress indicator
     * @return A list of PsiJavaFiles to process
     */
    private List<PsiJavaFile> collectJavaFiles(Project project, VirtualFile[] virtualFiles, ProgressIndicator indicator) {
        List<PsiJavaFile> javaFiles = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        
        indicator.setText("Collecting Java files...");
        indicator.setIndeterminate(true);
        
        for (VirtualFile virtualFile : virtualFiles) {
            indicator.checkCanceled();
            if (virtualFile.isDirectory()) {
                // Process directory recursively
                collectJavaFilesFromDirectory(javaFiles, virtualFile, psiManager, indicator);
            } else if ("java".equals(virtualFile.getExtension())) {
                // Process individual Java file
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile instanceof PsiJavaFile) {
                    javaFiles.add((PsiJavaFile) psiFile);
                }
            }
        }
        
        return javaFiles;
    }
    
    /**
     * Recursively collect Java files from a directory.
     * 
     * @param javaFiles The list to add Java files to
     * @param directory The directory to process
     * @param psiManager The PsiManager instance
     * @param indicator Progress indicator
     */
    private void collectJavaFilesFromDirectory(List<PsiJavaFile> javaFiles, VirtualFile directory, 
                                              PsiManager psiManager, ProgressIndicator indicator) {
        for (VirtualFile child : directory.getChildren()) {
            indicator.checkCanceled();
            if (child.isDirectory()) {
                collectJavaFilesFromDirectory(javaFiles, child, psiManager, indicator);
            } else if ("java".equals(child.getExtension())) {
                PsiFile psiFile = psiManager.findFile(child);
                if (psiFile instanceof PsiJavaFile) {
                    javaFiles.add((PsiJavaFile) psiFile);
                }
            }
        }
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
