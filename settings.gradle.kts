plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "java-var-refactoring-plugin"

include("core")
include("common")
include("intellij-plugin")
include("lsp-server")

// Test data module - not part of production artifacts
include("test-data")