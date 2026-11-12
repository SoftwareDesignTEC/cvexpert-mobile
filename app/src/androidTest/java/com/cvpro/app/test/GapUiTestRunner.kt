package com.cvpro.app.test

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Pure Compose fixture tests do not initialize accounts, notifications or the production Hilt graph. */
class GapUiTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
