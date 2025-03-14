# Makefile for Java Var Refactoring Plugin
# This file provides useful shortcuts for common development tasks

# Variables
GRADLEW := ./gradlew
PROJECT_NAME := java-var-refactoring
INTELLIJ_PLUGIN_DIR := intellij-plugin
LSP_SERVER_DIR := lsp-server
CORE_DIR := core
COMMON_DIR := common
VERSION := 1.0.0

# Default target
.PHONY: all
all: build

# Build the entire project
.PHONY: build
build:
	$(GRADLEW) build

# Clean the entire project
.PHONY: clean
clean:
	$(GRADLEW) clean

# Run all tests
.PHONY: test
test:
	$(GRADLEW) test

# Build IntelliJ plugin
.PHONY: plugin
plugin:
	$(GRADLEW) :$(INTELLIJ_PLUGIN_DIR):buildPlugin

# Run plugin in a development IDE instance
.PHONY: run-ide
run-ide:
	$(GRADLEW) :$(INTELLIJ_PLUGIN_DIR):runIde

# Build the LSP server
.PHONY: lsp
lsp:
	$(GRADLEW) :$(LSP_SERVER_DIR):shadowJar

# Run the LSP server for testing
.PHONY: run-lsp
run-lsp: lsp
	java -jar $(LSP_SERVER_DIR)/build/libs/lsp-server-$(VERSION)-all.jar

# Build the core module
.PHONY: core
core:
	$(GRADLEW) :$(CORE_DIR):build

# Build the common module
.PHONY: common
common:
	$(GRADLEW) :$(COMMON_DIR):build

# Publish plugin to JetBrains Marketplace
.PHONY: publish
publish:
	$(GRADLEW) :$(INTELLIJ_PLUGIN_DIR):publishPlugin

# Generate IntelliJ IDEA run configurations
.PHONY: idea
idea:
	$(GRADLEW) idea

# Check dependencies for vulnerabilities
.PHONY: check-deps
check-deps:
	$(GRADLEW) dependencyUpdates

# Enable verbose Gradle logs
.PHONY: verbose
verbose:
	$(GRADLEW) build --info --stacktrace

# Clean and then build the project
.PHONY: rebuild
rebuild: clean build

# Create a release package
.PHONY: release
release: clean test plugin lsp
	mkdir -p releases
	cp $(INTELLIJ_PLUGIN_DIR)/build/distributions/$(PROJECT_NAME)-$(VERSION).zip releases/
	cp $(LSP_SERVER_DIR)/build/libs/lsp-server-$(VERSION)-all.jar releases/$(PROJECT_NAME)-lsp-$(VERSION).jar
	@echo "Release packages created in releases/ directory"

# Install plugin locally for testing
.PHONY: install-local
install-local: plugin
	mkdir -p ~/.local/share/$(PROJECT_NAME)
	unzip -o $(INTELLIJ_PLUGIN_DIR)/build/distributions/$(PROJECT_NAME)-$(VERSION).zip -d ~/.local/share/$(PROJECT_NAME)
	@echo "Plugin installed to ~/.local/share/$(PROJECT_NAME)"

# Show help information
.PHONY: help
help:
	@echo "Java Var Refactoring Plugin - Build Targets"
	@echo ""
	@echo "Main targets:"
	@echo "  all           - Default target, builds the entire project"
	@echo "  build         - Builds the entire project"
	@echo "  clean         - Cleans build artifacts"
	@echo "  test          - Runs all tests"
	@echo "  rebuild       - Cleans and rebuilds the project"
	@echo ""
	@echo "Module targets:"
	@echo "  plugin        - Builds the IntelliJ plugin"
	@echo "  lsp           - Builds the LSP server"
	@echo "  core          - Builds the core module"
	@echo "  common        - Builds the common module"
	@echo ""
	@echo "Run targets:"
	@echo "  run-ide       - Runs IntelliJ with the plugin installed"
	@echo "  run-lsp       - Runs the LSP server for testing"
	@echo ""
	@echo "Release targets:"
	@echo "  release       - Creates release packages"
	@echo "  publish       - Publishes plugin to JetBrains Marketplace"
	@echo "  install-local - Installs plugin locally for testing"
	@echo ""
	@echo "Other targets:"
	@echo "  idea          - Generates IntelliJ IDEA run configurations"
	@echo "  check-deps    - Checks dependencies for updates"
	@echo "  verbose       - Runs build with verbose logging"
	@echo "  help          - Shows this help message"