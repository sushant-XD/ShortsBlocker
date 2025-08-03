package com.example.myapplication.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.home.HomeScreen
import com.example.myapplication.screens.home.HomeViewModel

@Composable
fun AppNavigationRoot(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "homePageGraph") {
        navigation(startDestination = "homepage", route = "homePageGraph") {
            composable("homepage") {
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }
}
