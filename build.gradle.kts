plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "io.github.potatocurry"

application {
    mainClassName = "io.github.potatocurry.spyfallkt.SpyfallAppKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.kwebio:kweb-core:0.5.11")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.2.0")
    implementation("com.soywiz.korlibs.klock:klock-jvm:1.8.5")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

task("stage") {
    dependsOn.add("build")
}
