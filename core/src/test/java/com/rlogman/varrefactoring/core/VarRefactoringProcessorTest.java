package com.rlogman.varrefactoring.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VarRefactoringProcessorTest {

    private VarRefactoringProcessor processor;
    private RefactoringOptions defaultOptions;

    @BeforeEach
    void setUp() {
        defaultOptions = new RefactoringOptions();
        processor = new VarRefactoringProcessor(defaultOptions);
    }

    @Test
    void shouldNotRefactorWithUnsupportedJavaVersion() {
        // Given
        String code = "class Test { void method() { String text = \"hello\"; } }";
        String javaVersion = "8";

        // When
        String result = processor.processFile(code, javaVersion);

        // Then
        assertThat(result).isEqualTo(code);
    }

    @Test
    void shouldRefactorLocalVariable() {
        // Given
        String code = "class Test { void method() { String text = \"hello\"; } }";
        String expected = "class Test { void method() { var text = \"hello\"; } }";
        String javaVersion = "11";

        // When
        String result = processor.processFile(code, javaVersion);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldNotRefactorFieldVariable() {
        // Given
        String code = "class Test { String text = \"hello\"; }";
        String javaVersion = "11";

        // When
        String result = processor.processFile(code, javaVersion);

        // Then
        assertThat(result).isEqualTo(code);
    }

    @Test
    void shouldNotRefactorVariableWithoutInitializer() {
        // Given
        String code = "class Test { void method() { String text; text = \"hello\"; } }";
        String javaVersion = "11";

        // When
        String result = processor.processFile(code, javaVersion);

        // Then
        assertThat(result).isEqualTo(code);
    }

    @ParameterizedTest
    @MethodSource("primitiveTypeExamples")
    void shouldRefactorPrimitiveTypesBasedOnOptions(String code, String expected, boolean allowPrimitives) {
        // Given
        String javaVersion = "11";
        var options = new RefactoringOptions();
        options.setAllowPrimitiveTypes(allowPrimitives);
        processor = new VarRefactoringProcessor(options);

        // When
        String result = processor.processFile(code, javaVersion);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> primitiveTypeExamples() {
        return Stream.of(
                // code, expected result when primitives allowed, allow primitives flag
                arguments(
                        "class Test { void method() { int value = 42; } }",
                        "class Test { void method() { var value = 42; } }",
                        true
                ),
                arguments(
                        "class Test { void method() { int value = 42; } }",
                        "class Test { void method() { int value = 42; } }",
                        false
                ),
                arguments(
                        "class Test { void method() { boolean flag = true; } }",
                        "class Test { void method() { var flag = true; } }",
                        true
                )
        );
    }

    @Test
    void shouldProcessMultipleFiles() {
        // Given
        List<String> files = List.of(
                "class Test1 { void method() { String text = \"hello\"; } }",
                "class Test2 { void method() { int value = 42; } }"
        );

        List<String> expected = List.of(
                "class Test1 { void method() { var text = \"hello\"; } }",
                "class Test2 { void method() { var value = 42; } }"
        );

        String javaVersion = "11";

        // When
        List<String> results = processor.processFiles(files, javaVersion);

        // Then
        assertThat(results).isEqualTo(expected);
    }
}