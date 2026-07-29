plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

val gdxVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
