plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.suffixtrainer.pipeline.MainKt")
}

dependencies {
    implementation(project(":core-model"))
}
