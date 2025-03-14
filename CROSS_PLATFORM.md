# Cross-Platform Extension Development Guide

This document provides detailed information about extending the Java Var Refactoring tool to multiple IDEs and editors.

## Architecture Overview

The project uses a modular architecture designed for cross-platform compatibility:

```
+----------------+      +-------------------+
|   Core Logic   | <--- | IDE Specific Code |
+----------------+      +-------------------+
        ^                        ^
        |                        |
+----------------+      +-------------------+
| Common Utilities| <--- |  LSP Server      |
+----------------+      +-------------------+
```

- **Core Module**: Contains the IDE-agnostic refactoring logic
- **Common Module**: Shared utilities and interfaces
- **IDE-Specific Modules**: Adapters for each supported IDE
- **LSP Server Module**: Language Server Protocol implementation for editor-agnostic support

## Extension Strategies

### 1. Direct Integration (IntelliJ, Eclipse, NetBeans)

For IDEs with robust Java parsing and refactoring capabilities, direct integration provides the best user experience:

1. Create an IDE-specific module (similar to `intellij-plugin`)
2. Implement IDE-specific service classes that delegate to the core module
3. Use the IDE's extension points to hook into the UI

#### Example for Eclipse Integration

```java
// Eclipse-specific action
public class EclipseVarRefactoringAction implements IObjectActionDelegate {
    private RefactoringOptions options;
    
    @Override
    public void run(IAction action) {
        // Get selected element in Eclipse
        // Convert to a format the core module can use
        // Call core module to perform refactoring
        // Apply changes using Eclipse's API
    }
}
```

### 2. LSP-Based Integration (VS Code, Vim, Emacs, etc.)

For editors that support the Language Server Protocol:

1. Use the `lsp-server` module which implements the LSP
2. Create a thin client extension for the specific editor
3. Configure the client to communicate with our LSP server

#### Example for VS Code Extension

```typescript
// VS Code extension.ts
import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

export function activate(context: vscode.ExtensionContext) {
    // Start the LSP server
    const serverOptions = {
        command: 'java',
        args: ['-jar', context.asAbsolutePath('server/lsp-server.jar')]
    };
    
    // Configure client options
    const clientOptions = {
        documentSelector: [{ scheme: 'file', language: 'java' }]
    };
    
    // Create and start the client
    const client = new LanguageClient(
        'javaVarRefactoring',
        'Java Var Refactoring',
        serverOptions,
        clientOptions
    );
    
    context.subscriptions.push(client.start());
    
    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('java.var.refactor', () => {
            // Call the LSP command
            vscode.commands.executeCommand(
                'java.var.refactor',
                vscode.window.activeTextEditor?.document.uri.toString()
            );
        })
    );
}
```

## Separation of Concerns

To maintain clean separation between core functionality and IDE-specific code:

1. **Core Logic**:
    - Should have no dependencies on any IDE
    - Define clear interfaces for IDE-specific code to implement
    - Be thoroughly tested independently of any IDE

2. **IDE-Specific Code**:
    - Implement interfaces defined by the core
    - Handle UI interactions and IDE integration
    - Translate between IDE-specific and core data structures

3. **Common Utilities**:
    - Shared code used by multiple modules
    - No dependencies on specific IDEs
    - General purpose utilities and constants

## Data Model Considerations

The key to cross-platform compatibility is a well-defined data model:

1. Core module should operate on simple data types (strings, lists, maps)
2. Avoid using IDE-specific types in interfaces
3. Create adapter classes to convert between IDE-specific and core data types

Example:

```java
// Bad - Using IDE-specific type in core interface
public interface TypeReplacer {
    void replaceType(PsiVariable variable);  // PsiVariable is IntelliJ-specific
}

// Good - Using generic types in core interface
public interface TypeReplacer {
    void replaceType(VariableInfo variableInfo);  // VariableInfo is our own type
}

// Then implement adapters in IDE-specific modules
class IntellijAdapter {
    VariableInfo convertToVariableInfo(PsiVariable variable) {
        // Convert IntelliJ types to our core types
    }
}
```

## Extension Points

The following extension points are available for integrating with different IDEs:

### Core Extension Points

1. **VarEligibilityChecker**: Determines if a variable is eligible for replacement
2. **JavaTypeReplacer**: Performs the actual text replacement
3. **RefactoringOptions**: Configures the behavior of the refactoring

### IDE Integration Points

1. **Actions/Commands**: Trigger the refactoring
2. **UI Components**: Settings panels, dialogs, etc.
3. **Project-specific settings**: Configure per-project behavior

## Testing Cross-Platform Extensions

For testing cross-platform functionality:

1. Create unit tests for the core logic using simple data models
2. Create IDE-specific tests for each supported IDE
3. Develop integration tests that verify the correct interaction between components

Example test structure:

```
tests/
├── core/                      # Core module tests
│   ├── VarRefactoringProcessorTest.java
│   ├── VarEligibilityCheckerTest.java
│   └── JavaTypeReplacerTest.java
├── intellij/                  # IntelliJ-specific tests
│   ├── VarRefactoringActionTest.java
│   └── IntellijVarRefactoringProcessorTest.java
├── eclipse/                   # Eclipse-specific tests
│   └── EclipseVarRefactoringActionTest.java
└── lsp/                       # LSP tests
    └── VarRefactoringLanguageServerTest.java
```

## Packaging and Distribution

For each supported platform:

1. **IntelliJ**: Package as a plugin JAR using the Gradle IntelliJ plugin
2. **Eclipse**: Package as an Eclipse plugin using the Eclipse PDE
3. **NetBeans**: Package as a NetBeans module
4. **VS Code**: Package as a VSIX extension
5. **LSP Clients**: Package the LSP server as a standalone JAR

Consider using CI/CD to automate building and testing for each platform.

## Resources

- [JetBrains Plugin Development](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Eclipse Plugin Development](https://www.eclipse.org/articles/Article-PDE-does-plugins/PDE-intro.html)
- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/)
- [VS Code Extension API](https://code.visualstudio.com/api)