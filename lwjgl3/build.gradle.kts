plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

val gdxVersion: String by project

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass = "com.dungeonarchitect.lwjgl3.Lwjgl3LauncherKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")

    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread", "-Xdock:name=Dungeon Architect")
    }
}
