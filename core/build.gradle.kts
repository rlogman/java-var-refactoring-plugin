plugins {
    id("java-library")
}

dependencies {
    implementation(project(":common"))

    // Use Eclipse JDT Core for Java parsing
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.35.0")

    // JavaParser as an alternative for Java parsing
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.5")
}
