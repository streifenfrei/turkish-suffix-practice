package com.example.suffixtrainer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val PRACTICE_ROUTE = "practice"
private const val SETTINGS_ROUTE = "settings"

/**
 * App root. Practice is the home screen; Settings is reached via the cog in Practice's top bar
 * and dismissed with the system back action (which pops the nav back stack) or its back arrow.
 */
@Composable
fun SuffixTrainerApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = PRACTICE_ROUTE) {
        composable(PRACTICE_ROUTE) {
            PracticeScreen(onOpenSettings = { navController.navigate(SETTINGS_ROUTE) })
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
