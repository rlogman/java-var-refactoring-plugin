plugins {
    id("java-library")
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
        resources {
            srcDirs("src/test/resources")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))

    // Eclipse LSP4J for Language Server Protocol implementation
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.21.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.21.1")
}

application {
    mainClass.set("com.rlogman.varrefactoring.lsp.VarRefactoringLanguageServer")
}