import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Con AGP 8, el soporte de Kotlin lo da este plugin (el Kotlin integrado es solo de AGP 9).
    alias(libs.plugins.kotlin.android)
    // Plugin del compilador de Compose: desde Kotlin 2.0 viene con Kotlin, no con AGP.
    alias(libs.plugins.kotlin.compose)
    // KSP genera el código de Hilt en tiempo de compilación (reemplazo moderno de kapt).
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Valores de configuración que NUNCA se escriben en el código ni se suben al repo.
// En local salen de local.properties (está en .gitignore); en GitHub Actions, de variables
// de entorno que el workflow carga desde los secrets. Se exponen vía BuildConfig.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configValue(name: String): String =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name).orEmpty()

android {
    // namespace = paquete de las clases generadas (R, BuildConfig).
    namespace = "com.cvpro.app"
    // Fijado en 35 a pedido de DevOps. Varias librerías del
    // catálogo están ancladas a esta versión: ver libs.versions.toml.
    compileSdk = 35

    defaultConfig {
        // Contrato con DevOps (firma del APK + Firebase). No se cambia.
        applicationId = "com.cvpro.app"
        minSdk = 26
        targetSdk = 35
        // Se sube en cada hito etiquetado. versionCode siempre crece: Firebase y Play
        // rechazan una subida con un número igual o menor al anterior.
        versionCode = 3
        versionName = "0.3.0"

        // Se inyectan como constantes en BuildConfig.
        buildConfigField("String", "APIM_BASE_URL", "\"${configValue("APIM_BASE_URL")}\"")
        // Web client ID de Google (OAuth). Es el mismo que usa la web como VITE_GOOGLE_CLIENT_ID.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${configValue("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildTypes {
        debug {
            // Sufijo para poder tener debug y release instalados a la vez.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // Java 17: lo que pide DevOps para el runner de CI.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Necesario para generar BuildConfig (versión visible + URL del APIM).
        buildConfig = true
    }
}

// Kotlin debe apuntar al mismo target que Java; si no coinciden, AGP 8 falla al compilar.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)


    // Login con Google: Credential Manager + Google ID (Google Sign-In nativo).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // El BOM alinea las versiones de todo Compose; por eso las libs de abajo no llevan versión.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Red: cliente HTTP y conversión JSON.
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)


    // Solo en debug: motor de los @Preview de Android Studio.
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.compose)
}
