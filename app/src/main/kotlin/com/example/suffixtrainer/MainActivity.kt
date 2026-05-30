package com.example.suffixtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.suffixtrainer.ui.SuffixTrainerApp
import com.example.suffixtrainer.ui.theme.TurkishSuffixPracticeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TurkishSuffixPracticeTheme {
                SuffixTrainerApp()
            }
        }
    }
}
