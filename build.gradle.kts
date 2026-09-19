// Build de raíz: solo declara los plugins para que cada módulo los aplique.
// "apply false" = se registran las versiones pero no se aplican a este proyecto raíz.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
