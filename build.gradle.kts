
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val releaseVersion = providers.gradleProperty("modVersion").get()
version = releaseVersion

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    archiveVersion.set(releaseVersion)
}

val compilePolaritySimulation by tasks.registering(JavaCompile::class) {
    group = "verification"
    description = "编译极性确定性模拟器"
    dependsOn(tasks.named("compileJava"))
    source(fileTree("src/polaritySimulation/java") { include("**/PolarityPhysicsSimulation.java") })
    classpath = files(
        tasks.named<JavaCompile>("compileJava").map { compileTask ->
            compileTask.classpath.filter { !it.name.lowercase().contains("jabel") }
        },
        layout.buildDirectory.dir("classes/java/main"),
    )
    destinationDirectory.set(layout.buildDirectory.dir("classes/java/polaritySimulation"))
    options.encoding = "UTF-8"
    options.compilerArgs.add("-proc:none")
}

val polaritySimulation by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "运行极性磁力、碰撞与伤害公式的确定性模拟"
    dependsOn(compilePolaritySimulation)
    classpath = files(
        compilePolaritySimulation.map { it.destinationDirectory },
        layout.buildDirectory.dir("classes/java/main"),
        tasks.named<JavaCompile>("compileJava").map { compileTask ->
            compileTask.classpath.filter { !it.name.lowercase().contains("jabel") }
        },
    )
    mainClass.set("com.greyhat.dark_grey.combat.PolarityPhysicsSimulation")
    args(layout.buildDirectory.file("reports/polarity/极性物理模拟.csv").get().asFile.absolutePath)
}
