package com.dena

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dena.core.LocaleHelper
import com.dena.data.DenaDatabaseProvider
import com.dena.ui.DenaApp
import com.dena.ui.theme.DenaTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val database = DenaDatabaseProvider.get(applicationContext)

        setContent {
            DenaApp(database = database)
        }
    }
}
