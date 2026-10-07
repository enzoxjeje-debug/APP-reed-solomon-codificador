package com.example.reedsolomonlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.reedsolomonlab.ui.theme.ReedSolomonLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReedSolomonLabTheme {
                ReedSolomonApp()
            }
        }
    }
}
