package com.seguimiento.clases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.seguimiento.clases.ui.navigation.AppNavigation
import com.seguimiento.clases.ui.theme.SeguimientoClasesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as AppApplication).repository

        setContent {
            SeguimientoClasesTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}
