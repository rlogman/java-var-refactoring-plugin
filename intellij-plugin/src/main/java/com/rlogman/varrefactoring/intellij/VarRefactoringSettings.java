package com.rlogman.varrefactoring.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the Java Var Refactoring plugin.
 */
@State(
    name = "VarRefactoringSettings",
    storages = @Storage("VarRefactoringSettings.xml")
)
public class VarRefactoringSettings implements PersistentStateComponent<VarRefactoringSettings> {

    // Default values for settings
    private boolean allowPrimitiveTypes = true;
    private boolean allowForLoopVars = true;
    private boolean allowDiamondOperator = true;
    private boolean allowDifferentTypes = false;
    private boolean refactorAnonymousClasses = false;
    private boolean refactorLambdaExpressions = false;

    /**
     * Get the instance of settings for the application.
     */
    public static VarRefactoringSettings getInstance() {
        return ServiceManager.getService(VarRefactoringSettings.class);
    }

    @Override
    public @Nullable VarRefactoringSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull VarRefactoringSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and setters for settings

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
