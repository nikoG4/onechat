plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "com.chat"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    // Ktor core
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.12")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.3.12")

    // WebSocket
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.12")

    // Auth
    implementation("io.ktor:ktor-server-auth-jvm:2.3.12")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.12")
    implementation("io.ktor:ktor-client-core-jvm:2.3.12")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.12")

    // Database - Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.3")

    // Connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")
}
