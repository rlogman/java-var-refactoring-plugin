plugins {
    id("java-library")
    id("application")
}

sourceSets {
    test {
        java {
            srcDirs("src")
        }
    }
}

// This module contains test data and examples only, not production code
dependencies {
    // Direct dependency on core module
    implementation(project(":core"))
    implementation(project(":common"))
}
