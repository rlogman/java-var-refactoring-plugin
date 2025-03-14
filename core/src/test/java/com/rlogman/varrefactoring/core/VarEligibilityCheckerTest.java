package com.rlogman.varrefactoring.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VarEligibilityCheckerTest {

    private VarEligibilityChecker checker;
    private RefactoringOptions options;
    
    @BeforeEach
    void setUp() {
        options = new RefactoringOptions();
        checker = new VarEligibilityChecker(options);
    }
    
    @Test
    void shouldNotReplaceNonLocalVariables() {
        // Given
        boolean isLocal = false;
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            "String", "String", isLocal, false);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @ParameterizedTest
    @CsvSource({
        "int, int, true, true",
        "int, int, false, false",
        "long, long, true, true",
        "boolean, boolean, true, true",
        "double, double, true, true"
    })
    void shouldReplaceLocalPrimitiveTypesBasedOnSettings(
            String declaredType, String initializerType, 
            boolean allowPrimitives, boolean expected) {
        
        // Given
        options.setAllowPrimitiveTypes(allowPrimitives);
        checker = new VarEligibilityChecker(options);
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            declaredType, initializerType, true, false);
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "String, String, true",
        "java.util.List<String>, java.util.List<String>, true",
        "Map<String,Integer>, Map<String,Integer>, true"
    })
    void shouldReplaceObjectTypesWithMatchingInitializer(
            String declaredType, String initializerType, boolean expected) {
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            declaredType, initializerType, true, false);
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "true, true",
        "false, false"
    })
    void shouldReplaceForLoopVarsBasedOnSettings(boolean allowForLoopVars, boolean expected) {
        // Given
        options.setAllowForLoopVars(allowForLoopVars);
        checker = new VarEligibilityChecker(options);
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            "String", "String", true, true);
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "List<>, ArrayList<String>, true, true",
        "List<>, ArrayList<String>, false, false",
        "Map<,>, HashMap<String,Integer>, true, true"
    })
    void shouldReplaceDiamondOperatorBasedOnSettings(
            String declaredType, String initializerType, 
            boolean allowDiamondOperator, boolean expected) {
        
        // Given
        options.setAllowDiamondOperator(allowDiamondOperator);
        checker = new VarEligibilityChecker(options);
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            declaredType, initializerType, true, false);
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "List<String>, ArrayList<String>, true, true",
        "List<String>, ArrayList<String>, false, false",
        "CharSequence, String, true, true",
        "Object, String, true, true"
    })
    void shouldReplaceDifferentTypesBasedOnSettings(
            String declaredType, String initializerType, 
            boolean allowDifferentTypes, boolean expected) {
        
        // Given
        options.setAllowDifferentTypes(allowDifferentTypes);
        checker = new VarEligibilityChecker(options);
        
        // When
        boolean result = checker.isEligibleForVarReplacement(
            declaredType, initializerType, true, false);
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
    
    @Test
    void shouldApplyMultipleRulesCorrectly() {
        // Given a checker with multiple restrictions
        options.setAllowPrimitiveTypes(false);
        options.setAllowForLoopVars(false);
        options.setAllowDiamondOperator(false);
        checker = new VarEligibilityChecker(options);
        
        // When / Then - Various scenarios
        assertThat(checker.isEligibleForVarReplacement(
            "int", "int", true, false)).isFalse(); // Primitive not allowed
            
        assertThat(checker.isEligibleForVarReplacement(
            "String", "String", true, true)).isFalse(); // For loop not allowed
            
        assertThat(checker.isEligibleForVarReplacement(
            "List<>", "ArrayList<String>", true, false)).isFalse(); // Diamond not allowed
            
        assertThat(checker.isEligibleForVarReplacement(
            "String", "String", true, false)).isTrue(); // Regular local var should be allowed
    }
}