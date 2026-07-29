plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

val gdxVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
}
