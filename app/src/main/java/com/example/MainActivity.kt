package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppStateViewModel
import com.example.ui.navigation.NucleoIaApp
import com.example.ui.theme.NucleoIaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppStateViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            NucleoIaTheme(darkTheme = prefs.darkTheme) {
                NucleoIaApp(viewModel = viewModel)
            }
        }
    }
}
