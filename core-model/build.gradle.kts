plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Room annotations live in room-common, a plain JVM jar with no Android
    // dependency. Exposed via `api` so :app and :datapipeline see the same
    // @Entity definitions. This is the single source of truth for the schema.
    api(libs.androidx.room.common)

    testImplementation(libs.junit)
}
