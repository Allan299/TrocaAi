package com.example.trocaai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.trocaai.navigation.TrocaAiApp
import com.example.trocaai.ui.theme.TrocaAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrocaAiTheme {
                TrocaAiApp()
            }
        }
    }
}
