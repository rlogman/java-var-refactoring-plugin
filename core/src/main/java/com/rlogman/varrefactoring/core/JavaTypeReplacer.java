package com.rlogman.varrefactoring.core;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Handles the actual replacement of explicit types with 'var' in Java code.
 * This is a simplified implementation that would be replaced with proper
 * Java parsing in a real implementation.
 */
public class JavaTypeReplacer {

    /**
     * Replace explicit type declarations with 'var' where appropriate.
     *
     * @param fileContent The Java file content
     * @param eligibilityPredicate A predicate that determines if a declaration is eligible
     * @return The refactored file content
     */
    public String replaceExplicitTypes(
            String fileContent,
            EligibilityPredicate eligibilityPredicate) {

        // In a real implementation, this would use a proper Java parser
        // like JavaParser or Eclipse JDT to properly understand the code
        // This is a simplified example

        StringBuilder result = new StringBuilder(fileContent);

        // Simple pattern to match variable declarations
        // This is oversimplified and would need a proper parser in reality
        Pattern pattern = Pattern.compile(
            "\\b([A-Za-z][A-Za-z0-9_]*(?:<.*?>)?)\\s+([a-z][A-Za-z0-9_]*)\\s*=\\s*(.+?);"
        );

        Matcher matcher = pattern.matcher(fileContent);

        // Track offset changes as we modify the string
        int offset = 0;

        while (matcher.find()) {
            String declarationType = matcher.group(1);
            String variableName = matcher.group(2);
            String initializer = matcher.group(3);

            // For demonstration only - would need actual type analysis
            String initializerType = inferType(initializer);

            // Check if this declaration is eligible for 'var' replacement
            if (eligibilityPredicate.test(declarationType, initializerType, true, false)) {
                int replaceStart = matcher.start(1) + offset;
                int replaceEnd = matcher.end(1) + offset;

                // Replace the type with 'var'
                result.replace(replaceStart, replaceEnd, "var");

                // Update offset for future replacements
                offset += "var".length() - declarationType.length();
            }
        }

        return result.toString();
    }

    /**
     * Simple type inference for demonstration purposes.
     * A real implementation would use a Java parser.
     */
    private String inferType(String initializer) {
        // Very simplistic inference for demonstration
        initializer = initializer.trim();

        if (initializer.matches("\\d+")) {
            return "int";
        } else if (initializer.matches("\\d+L")) {
            return "long";
        } else if (initializer.matches("\\d+\\.\\d+f")) {
            return "float";
        } else if (initializer.matches("\\d+\\.\\d+")) {
            return "double";
        } else if (initializer.equals("true") || initializer.equals("false")) {
            return "boolean";
        } else if (initializer.startsWith("\"") && initializer.endsWith("\"")) {
            return "String";
        } else if (initializer.matches("new \\w+.*")) {
            // Extract type from "new Type(...)"
            String type = initializer.substring(4).split("[(<]")[0].trim();
            return type;
        }

        // Default fallback
        return "Object";
    }

    /**
     * Functional interface for eligibility checking
     */
    @FunctionalInterface
    public interface EligibilityPredicate {
        boolean test(String declarationType, String initializerType, boolean isLocal, boolean isLoopVariable);
    }
}
