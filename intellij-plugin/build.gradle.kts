plugins {
    id("java")
    id("org.jetbrains.intellij")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    
    // IntelliJ platform dependencies
    implementation("org.jetbrains:annotations:24.0.1")
}

// Configure Gradle IntelliJ Plugin
// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2023.3")
    type.set("IC") // Target IntelliJ IDEA Community Edition
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("233") // Minimum IDEA version
        untilBuild.set("242.*") // Maximum IDEA version
        changeNotes.set("""
            Initial release:
            - Adds refactoring action to convert explicit type declarations to 'var'
            - Adds intention action for local variable declarations
            - Supports configuration options for refactoring behavior
        """)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN") ?: "")
        privateKey.set(System.getenv("PRIVATE_KEY") ?: "")
        password.set(System.getenv("PRIVATE_KEY_PASSWORD") ?: "")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN") ?: "")
        channels.set(listOf("default"))
    }
}
