
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val releaseVersion = providers.gradleProperty("modVersion").get()
version = releaseVersion

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    archiveVersion.set(releaseVersion)
}
