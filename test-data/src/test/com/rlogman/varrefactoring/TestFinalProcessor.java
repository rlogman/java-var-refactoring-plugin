package test.com.rlogman.varrefactoring;

// Core package imports with full paths to resolve properly
import com.rlogman.varrefactoring.core.RefactoringOptions;
import com.rlogman.varrefactoring.core.VarRefactoringProcessor;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestFinalProcessor {
    public static void main(String[] args) throws Exception {
        String javaFile = Files.readString(Paths.get("test-data/TestFinal.java"));
        
        RefactoringOptions options = new RefactoringOptions();
        options.setAllowPrimitiveTypes(true);
        options.setAllowDifferentTypes(true);
        
        VarRefactoringProcessor processor = new VarRefactoringProcessor(options);
        String refactored = processor.processFile(javaFile, "11");
        
        System.out.println("Original file:");
        System.out.println("-------------");
        System.out.println(javaFile);
        
        System.out.println("\nRefactored file:");
        System.out.println("---------------");
        System.out.println(refactored);
    }
}