package com.example.myapplication.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.home.HomePage
import com.example.myapplication.screens.home.homeViewModel

@Composable
fun AppNavigationRoot(homeViewModel: homeViewModel){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "homePageGraph"){
        navigation(startDestination = "homepage", route = "homePageGraph"){
            composable("homepage"){
                HomePage(viewModel = homeViewModel)
            }
        }
    }
}
