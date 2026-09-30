// Сервисы services/: Spring Boot приложение и генерация из contracts/<сервис>/openapi.yaml, если контракт есть.

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("org.springframework.boot")
    id("org.openapi.generator")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Генерация: только интерфейсы контроллеров и модели, без клиентов (CLAUDE.md, «Контракты между сервисами»)
val contract = rootProject.file("contracts/${project.name}/openapi.yaml")

if (contract.exists()) {
    // fake-provider → fakeprovider: дефис в имени пакета недопустим
    val generatedPackage = "com.tickethub.${project.name.replace("-", "")}.adapter.in.web.generated"

    openApiGenerate {
        generatorName = "spring"
        inputSpec = contract.toURI().toString() // URI, а не путь: генератор не разбирает Windows-путь с кириллицей
        outputDir = layout.buildDirectory.dir("generated/openapi").get().asFile.path
        apiPackage = "$generatedPackage.api"
        modelPackage = "$generatedPackage.model"
        configOptions = mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true", // jakarta.*; отдельной опции для Boot 4 в генераторе 7.14 нет
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
        )
    }

    sourceSets.main {
        java.srcDir(tasks.named<GenerateTask>("openApiGenerate").map { "${it.outputDir.get()}/src/main/java" })
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-validation")
    }
}
