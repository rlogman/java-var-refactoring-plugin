package com.rlogman.varrefactoring.intellij;

import com.rlogman.varrefactoring.intellij.settings.VarRefactoringSettings;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configuration UI for the Java Var Refactoring plugin.
 */
public class VarRefactoringConfigurable implements Configurable {
    private JPanel mainPanel;
    private JCheckBox allowPrimitiveTypesCheckBox;
    private JCheckBox allowForLoopVarsCheckBox;
    private JCheckBox allowDiamondOperatorCheckBox;
    private JCheckBox allowDifferentTypesCheckBox;
    private JCheckBox refactorAnonymousClassesCheckBox;
    private JCheckBox refactorLambdaExpressionsCheckBox;

    private VarRefactoringSettings settings;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Java Var Refactoring";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settings = VarRefactoringSettings.getInstance();

        mainPanel = new JPanel(new GridLayout(0, 1));
        
        allowPrimitiveTypesCheckBox = new JCheckBox("Replace primitive types with 'var'");
        allowForLoopVarsCheckBox = new JCheckBox("Replace for-loop variables with 'var'");
        allowDiamondOperatorCheckBox = new JCheckBox("Replace diamond operator types with 'var'");
        allowDifferentTypesCheckBox = new JCheckBox("Replace when declared type differs from initializer type");
        refactorAnonymousClassesCheckBox = new JCheckBox("Refactor variables initialized with anonymous classes");
        refactorLambdaExpressionsCheckBox = new JCheckBox("Refactor variables initialized with lambda expressions");

        mainPanel.add(new JLabel("Configure which variable types should be converted to 'var':"));
        mainPanel.add(allowPrimitiveTypesCheckBox);
        mainPanel.add(allowForLoopVarsCheckBox);
        mainPanel.add(allowDiamondOperatorCheckBox);
        mainPanel.add(allowDifferentTypesCheckBox);
        mainPanel.add(refactorAnonymousClassesCheckBox);
        mainPanel.add(refactorLambdaExpressionsCheckBox);

        // Load current settings
        reset();
        
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return 
            allowPrimitiveTypesCheckBox.isSelected() != settings.isAllowPrimitiveTypes() ||
            allowForLoopVarsCheckBox.isSelected() != settings.isAllowForLoopVars() ||
            allowDiamondOperatorCheckBox.isSelected() != settings.isAllowDiamondOperator() ||
            allowDifferentTypesCheckBox.isSelected() != settings.isAllowDifferentTypes() ||
            refactorAnonymousClassesCheckBox.isSelected() != settings.isRefactorAnonymousClasses() ||
            refactorLambdaExpressionsCheckBox.isSelected() != settings.isRefactorLambdaExpressions();
    }

    @Override
    public void apply() {
        settings.setAllowPrimitiveTypes(allowPrimitiveTypesCheckBox.isSelected());
        settings.setAllowForLoopVars(allowForLoopVarsCheckBox.isSelected());
        settings.setAllowDiamondOperator(allowDiamondOperatorCheckBox.isSelected());
        settings.setAllowDifferentTypes(allowDifferentTypesCheckBox.isSelected());
        settings.setRefactorAnonymousClasses(refactorAnonymousClassesCheckBox.isSelected());
        settings.setRefactorLambdaExpressions(refactorLambdaExpressionsCheckBox.isSelected());
    }

    @Override
    public void reset() {
        allowPrimitiveTypesCheckBox.setSelected(settings.isAllowPrimitiveTypes());
        allowForLoopVarsCheckBox.setSelected(settings.isAllowForLoopVars());
        allowDiamondOperatorCheckBox.setSelected(settings.isAllowDiamondOperator());
        allowDifferentTypesCheckBox.setSelected(settings.isAllowDifferentTypes());
        refactorAnonymousClassesCheckBox.setSelected(settings.isRefactorAnonymousClasses());
        refactorLambdaExpressionsCheckBox.setSelected(settings.isRefactorLambdaExpressions());
    }
}