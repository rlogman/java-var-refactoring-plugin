package com.rlogman.varrefactoring.core;

/**
 * Configuration options for the var refactoring process.
 */
public class RefactoringOptions {
    private boolean allowPrimitiveTypes = true;
    private boolean allowForLoopVars = true;
    private boolean allowDiamondOperator = true;
    private boolean allowDifferentTypes = false;
    private boolean refactorAnonymousClasses = false;
    private boolean refactorLambdaExpressions = false;

    // Default constructor with default values
    public RefactoringOptions() {
    }

    // Constructor with all options
    public RefactoringOptions(
            boolean allowPrimitiveTypes,
            boolean allowForLoopVars,
            boolean allowDiamondOperator,
            boolean allowDifferentTypes,
            boolean refactorAnonymousClasses,
            boolean refactorLambdaExpressions) {
        this.allowPrimitiveTypes = allowPrimitiveTypes;
        this.allowForLoopVars = allowForLoopVars;
        this.allowDiamondOperator = allowDiamondOperator;
        this.allowDifferentTypes = allowDifferentTypes;
        this.refactorAnonymousClasses = refactorAnonymousClasses;
        this.refactorLambdaExpressions = refactorLambdaExpressions;
    }

    // Getters and setters
    public boolean isAllowPrimitiveTypes() {
        return allowPrimitiveTypes;
    }

    public void setAllowPrimitiveTypes(boolean allowPrimitiveTypes) {
        this.allowPrimitiveTypes = allowPrimitiveTypes;
    }

    public boolean isAllowForLoopVars() {
        return allowForLoopVars;
    }

    public void setAllowForLoopVars(boolean allowForLoopVars) {
        this.allowForLoopVars = allowForLoopVars;
    }

    public boolean isAllowDiamondOperator() {
        return allowDiamondOperator;
    }

    public void setAllowDiamondOperator(boolean allowDiamondOperator) {
        this.allowDiamondOperator = allowDiamondOperator;
    }

    public boolean isAllowDifferentTypes() {
        return allowDifferentTypes;
    }

    public void setAllowDifferentTypes(boolean allowDifferentTypes) {
        this.allowDifferentTypes = allowDifferentTypes;
    }

    public boolean isRefactorAnonymousClasses() {
        return refactorAnonymousClasses;
    }

    public void setRefactorAnonymousClasses(boolean refactorAnonymousClasses) {
        this.refactorAnonymousClasses = refactorAnonymousClasses;
    }

    public boolean isRefactorLambdaExpressions() {
        return refactorLambdaExpressions;
    }

    public void setRefactorLambdaExpressions(boolean refactorLambdaExpressions) {
        this.refactorLambdaExpressions = refactorLambdaExpressions;
    }
}
