// Configuración del build: aquí se declara de dónde salen los plugins y las dependencias.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Permite que Gradle descargue solo el JDK 17 que exige gradle/gradle-daemon-jvm.properties;
    // sin este plugin, Gradle 8.14 no sabe de dónde bajarlo.
    // La versión va escrita acá porque el version catalog no está disponible en settings.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Ningún módulo puede declarar sus propios repositorios: todos usan estos.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// TODO: confirmar nombre comercial de la app.
rootProject.name = "cvpro-app"
include(":app")
