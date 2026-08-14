plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

val gdxVersion: String by project
val kotestVersion = "6.2.0"

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
