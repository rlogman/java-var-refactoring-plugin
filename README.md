# Java Var Refactoring Plugin

A JetBrains plugin that refactors Java code to use the idiomatic `var` keyword introduced in Java 10. This plugin helps modernize your codebase by automatically replacing explicit type declarations with the `var` keyword where appropriate.

## Features

- Automatically identifies eligible variables for conversion to `var`
- Handles local variable declarations in Java 10+ projects
- Supports batch refactoring for multiple files
- Configurable rules for when to apply `var` conversions
- Works via action in the refactoring menu and as intention actions

## Installation

### IntelliJ IDEA / JetBrains IDEs

1. In your IDE, go to `Settings` > `Plugins` > `Marketplace`
2. Search for "Java Var Refactoring"
3. Click `Install` and restart the IDE

### Manual Installation

1. Download the latest release JAR file from the [Releases](https://github.com/rlogman/java-var-refactoring-plugin/releases) page
2. In your IDE, go to `Settings` > `Plugins` > `⚙️` > `Install Plugin from Disk...`
3. Select the downloaded JAR file and restart the IDE

## Usage

### Refactoring Menu

1. Select a Java file, class, or package in the Project view
2. Right-click and select `Refactor` > `Convert to 'var'`
3. Review the changes in the preview and apply them

### Intention Action

1. Place the cursor on a variable declaration
2. Press `Alt+Enter` (or `Option+Enter` on macOS)
3. Select `Convert to 'var'` from the menu

### Settings

Configure the plugin behavior at `Settings` > `Tools` > `Java Var Refactoring`:

- **Replace primitive types with 'var'** - Convert `int`, `boolean`, etc. to `var`
- **Replace for-loop variables with 'var'** - Apply to variables in for loops
- **Replace diamond operator types with 'var'** - Replace `List<>` usage with `var`
- **Replace when declared type differs from initializer type** - Apply even when explicit type provides additional information
- **Refactor variables initialized with anonymous classes** - Apply to variables with anonymous class initializers
- **Refactor variables initialized with lambda expressions** - Apply to variables with lambda initializers

## Project Structure

The project is organized into several modules to promote cross-platform compatibility:

- **core** - Contains the core refactoring logic, independent of any IDE
- **common** - Shared utilities and interfaces
- **intellij-plugin** - IntelliJ-specific implementation
- **lsp-server** - Language Server Protocol implementation for other editors

## Cross-Platform Compatibility

This plugin is designed with cross-platform compatibility in mind. Here's how it can be extended to other IDEs and editors:

### Visual Studio Code

The `lsp-server` module provides a Language Server Protocol implementation that can be used with VS Code:

1. Build the LSP server: `./gradlew :lsp-server:shadowJar`
2. Create a VS Code extension that launches the server JAR
3. Configure the extension to handle Java files and provide the appropriate commands

Example `package.json` for a VS Code extension:

```json
{
  "name": "java-var-refactoring-plugin",
  "displayName": "Java Var Refactoring",
  "description": "Refactors Java code to use the var keyword",
  "version": "1.0.0",
  "engines": {
    "vscode": "^1.65.0"
  },
  "categories": [
    "Programming Languages"
  ],
  "activationEvents": [
    "onLanguage:java"
  ],
  "main": "./out/extension.js",
  "contributes": {
    "commands": [
      {
        "command": "java.var.refactor",
        "title": "Convert to 'var'"
      }
    ],
    "menus": {
      "editor/context": [
        {
          "when": "editorLangId == java",
          "command": "java.var.refactor",
          "group": "1_modification"
        }
      ]
    }
  }
}
```

### Eclipse

For Eclipse integration:

1. Build a bundle using the core module
2. Implement an Eclipse plugin that bridges between Eclipse's JDT and the core refactoring logic
3. Provide appropriate extension points for refactoring actions

### NetBeans

For NetBeans integration:

1. Use the core module in a NetBeans module
2. Implement the NetBeans-specific UI and action handlers
3. Connect to the NetBeans Java source tree API

## Development

### Prerequisites

- JDK 17 or higher
- Gradle 7.4 or higher

### Building from Source

1. Clone the repository
   ```
   git clone https://github.com/rlogman/java-var-refactoring-plugin.git
   cd java-var-refactoring-plugin
   ```

2. Build the project
   ```
   ./gradlew build
   ```

3. Run the plugin in a development IDE instance
   ```
   ./gradlew runIde
   ```

### Project Setup

The project is set up with the following structure:

```
java-var-refactoring-plugin/
├── core/                      # Core refactoring logic
├── common/                    # Shared utilities
├── intellij-plugin/           # IntelliJ plugin implementation
├── lsp-server/                # Language Server Protocol implementation
├── docs/                      # Documentation
├── build.gradle.kts           # Main build configuration
├── settings.gradle.kts        # Project settings
└── README.md                  # Project documentation
```

### Testing

Run the tests with:

```
./gradlew test
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.