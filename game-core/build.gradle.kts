plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
