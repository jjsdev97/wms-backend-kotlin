plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.project.wms"
version = "0.0.1-SNAPSHOT"
description = "wms"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:2.0.0")
		mavenBom("org.testcontainers:testcontainers-bom:1.20.6")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-json")
	implementation("org.aspectj:aspectjweaver")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	// Testcontainers 1.20.6 번들 docker-java(3.4.1)는 최신 Docker Desktop named pipe와 400 충돌.
	// HTTP Host 헤더 처리가 고쳐진 최신 트랜스포트로 올린다.
	testImplementation("com.github.docker-java:docker-java-core:3.7.1")
	testImplementation("com.github.docker-java:docker-java-transport-zerodep:3.7.1")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Docker Desktop(Windows)은 default(docker_engine) 대신 dockerDesktopLinuxEngine 파이프를 쓴다.
	// Testcontainers가 데몬을 찾도록 지정. DOCKER_HOST가 이미 있거나 비-Windows면 건드리지 않음(CI 안전).
	if (System.getenv("DOCKER_HOST") == null && System.getProperty("os.name").startsWith("Windows")) {
		environment("DOCKER_HOST", "npipe:////./pipe/docker_engine")
		// 최신 Docker Engine은 구버전 API를 거부(minAPI 1.40+). docker-java가 협상 없이 보내면 400 →
		// 데몬이 받는 API 버전을 명시. CI에서 DOCKER_API_VERSION을 이미 주면 그걸 존중.
		if (System.getenv("DOCKER_API_VERSION") == null) {
			environment("DOCKER_API_VERSION", "1.43")
		}
	}
}
