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

    // Zemberek morphology pulls zemberek-core/tokenization/lm transitively, plus
    // caffeine + protobuf from Maven Central.
    implementation(libs.zemberek.morphology)

    // SQLite emission + Tatoeba dump decompression (bzip2 / tar).
    implementation(libs.sqlite.jdbc)
    implementation(libs.commons.compress)
    // Parsing Room's exported schema JSON (createSql + identityHash).
    implementation(libs.org.json)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

// Zemberek's default model load is RAM-heavy; give the run/test JVMs headroom.
tasks.named<JavaExec>("run") { maxHeapSize = "2g" }
tasks.withType<Test>().configureEach { maxHeapSize = "2g" }
