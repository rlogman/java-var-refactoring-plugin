package com.rlogman.varrefactoring.core;

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
        // Capture group 1: modifiers (like 'final')
        // Capture group 2: type name
        // Capture group 3: variable name
        // Capture group 4: initializer
        Pattern pattern = Pattern.compile(
            "\\b((?:final\\s+)?)([A-Za-z][A-Za-z0-9_]*(?:<.*?>)?)\\s+([a-z][A-Za-z0-9_]*)\\s*=\\s*(.+?);"
        );

        Matcher matcher = pattern.matcher(fileContent);

        // Track offset changes as we modify the string
        int offset = 0;

        while (matcher.find()) {
            // Group 1 contains modifiers like 'final' - we don't modify these, just preserve them
            String declarationType = matcher.group(2);
            // Group 3 contains variable name - not used for the replacement logic
            String initializer = matcher.group(4);
            
            // Determine if this is a field or local variable
            boolean isLocal = isLocalVariable(fileContent, matcher.start());

            // For demonstration only - would need actual type analysis
            String initializerType = inferType(initializer);

            // Check if this declaration is eligible for 'var' replacement
            if (eligibilityPredicate.test(declarationType, initializerType, isLocal, false)) {
                int typeReplaceStart = matcher.start(2) + offset;
                int typeReplaceEnd = matcher.end(2) + offset;

                // Replace just the type with 'var', preserving any modifiers
                result.replace(typeReplaceStart, typeReplaceEnd, "var");

                // Update offset for future replacements
                offset += "var".length() - declarationType.length();
            }
        }

        return result.toString();
    }
    
    /**
     * Determine if a variable declaration is a local variable based on its context.
     * Very simple heuristic for the demo - a proper parser would be used in reality.
     * 
     * @param fileContent The complete file content
     * @param position The position of the variable declaration
     * @return true if it appears to be a local variable, false if it's likely a field
     */
    private boolean isLocalVariable(String fileContent, int position) {
        // Check if the declaration is inside a method by looking for '{' and '}'
        // This is a very naive implementation for demonstration purposes
        
        // Get substring up to the position
        String beforeDecl = fileContent.substring(0, position);
        
        // Count opening and closing braces
        int openBraces = 0;
        int closeBraces = 0;
        
        for (char c : beforeDecl.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }
        
        // Simple heuristic: if we have at least one more open brace than close brace,
        // and we have at least 2 braces total, then we're likely inside a method
        return openBraces > closeBraces && openBraces >= 2;
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
            return initializer.substring(4).split("[(<]")[0].trim();
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
