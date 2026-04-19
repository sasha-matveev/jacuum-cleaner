import com.github.gradle.node.npm.task.NpmTask

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.jacuum"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

node {
    version = "20.17.0"
    download = true
    workDir = file("${project.projectDir}/.gradle/nodejs")
    nodeProjectDir = file("${project.projectDir}")
}

val npmRunTest by tasks.registering(NpmTask::class) {
    args = listOf("test")
    dependsOn(tasks.named("npmInstall"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn(npmRunTest)
}
