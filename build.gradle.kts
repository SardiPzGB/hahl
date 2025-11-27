plugins {
	kotlin("jvm") version "1.9.22"
	kotlin("plugin.spring") version "1.9.22"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
	id ("maven-publish")
	application
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"
description = "HAHL project on Spring Boot"


repositories {
	mavenCentral()
}

dependencies {
	implementation ("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.flywaydb:flyway-database-postgresql")
	implementation ("org.postgresql:postgresql")

	implementation("org.jetbrains.kotlin:kotlin-reflect")

	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

application {
	mainClass.set("ru.itmo.hahl.HahlApplicationKt")
}