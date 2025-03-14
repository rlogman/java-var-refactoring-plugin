package com.rlogman.varrefactoring.core;

/**
 * Determines if a variable declaration is eligible for 'var' replacement.
 */
public class VarEligibilityChecker {
    private final RefactoringOptions options;

    public VarEligibilityChecker(RefactoringOptions options) {
        this.options = options;
    }

    /**
     * Checks if a variable declaration can be replaced with 'var'.
     *
     * @param declarationType The declared type name
     * @param initializerType The type name of the initializer expression
     * @param isLocal Whether the variable is a local variable
     * @param isLoopVariable Whether the variable is a for-loop variable
     * @return true if the variable can be replaced with 'var'
     */
    public boolean isEligibleForVarReplacement(
            String declarationType,
            String initializerType,
            boolean isLocal,
            boolean isLoopVariable) {

        // 'var' can only be used for local variables with initializers
        if (!isLocal) {
            return false;
        }

        // Skip if this is a for-loop variable and the options disallow it
        if (isLoopVariable && !options.isAllowForLoopVars()) {
            return false;
        }

        // Check if the declaration involves a diamond operator
        boolean hasDiamondOperator = containsDiamondOperator(declarationType);
        
        // Skip if the declaration uses diamond operator and options disallow it
        if (hasDiamondOperator && !options.isAllowDiamondOperator()) {
            return false;
        }

        // Skip primitive types if option is disabled
        if (isPrimitiveType(declarationType) && !options.isAllowPrimitiveTypes()) {
            return false;
        }

        // Check if the declared type and initializer type match
        // If they don't match, we might want to keep the explicit type
        if (!options.isAllowDifferentTypes() && !declarationType.equals(initializerType)) 
            return false;

        return true;
    }
    
    /**
     * Check if a type declaration contains a diamond operator.
     */
    private boolean containsDiamondOperator(String type) {
        return type.contains("<>") || type.endsWith("<>");
    }

    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("int") ||
               typeName.equals("long") ||
               typeName.equals("short") ||
               typeName.equals("byte") ||
               typeName.equals("char") ||
               typeName.equals("float") ||
               typeName.equals("double") ||
               typeName.equals("boolean");
    }
}