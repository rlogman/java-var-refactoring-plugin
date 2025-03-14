package com.rlogman.varrefactoring.core;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Core processor for handling 'var' refactoring operations.
 * This class is IDE-agnostic and can be used in different environments.
 */
public class VarRefactoringProcessor {
    private final RefactoringOptions options;
    private final JavaTypeReplacer typeReplacer;
    private final VarEligibilityChecker eligibilityChecker;

    public VarRefactoringProcessor(RefactoringOptions options) {
        this.options = options;
        this.typeReplacer = new JavaTypeReplacer();
        this.eligibilityChecker = new VarEligibilityChecker(options);
    }

    /**
     * Process a single Java file for var refactoring.
     *
     * @param fileContent The content of a Java file
     * @param javaVersion The Java version of the source code (e.g., "11")
     * @return The refactored file content
     */
    public String processFile(String fileContent, String javaVersion) {
        // Check if Java version supports 'var'
        if (!isVarSupported(javaVersion)) {
            return fileContent;
        }

        // The actual implementation will parse the Java file and
        // apply var replacements using the JavaTypeReplacer

        return typeReplacer.replaceExplicitTypes(
            fileContent,
            eligibilityChecker::isEligibleForVarReplacement
        );
    }

    /**
     * Process multiple Java files for var refactoring.
     *
     * @param filesContent List of Java file contents
     * @param javaVersion The Java version of the source code
     * @return List of refactored file contents
     */
    public List<String> processFiles(List<String> filesContent, String javaVersion) {
        if (!isVarSupported(javaVersion)) {
            return new ArrayList<>(filesContent);
        }

        var results = new ArrayList<String>(filesContent.size());
        for (var content : filesContent) {
            results.add(processFile(content, javaVersion));
        }

        return results;
    }

    /**
     * Determine if the given Java version supports the 'var' keyword.
     *
     * @param javaVersion The Java version string
     * @return true if 'var' is supported, false otherwise
     */
    private boolean isVarSupported(String javaVersion) {
        try {
            var version = Integer.parseInt(javaVersion);
            return version >= 10;
        } catch (NumberFormatException e) {
            // Handle non-numeric version strings if needed
            return false;
        }
    }
}