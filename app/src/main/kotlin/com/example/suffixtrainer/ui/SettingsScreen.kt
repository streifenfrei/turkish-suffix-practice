package com.example.suffixtrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.suffixtrainer.ui.theme.TurkishSuffixPracticeTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Settings")
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TurkishSuffixPracticeTheme {
        SettingsScreen()
    }
}
