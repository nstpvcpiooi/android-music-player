package com.example.musicplayer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

enum class BottomNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null,
    val route: String
) {
    HOME(
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = "home_route"
    ),
    LIBRARY(
        title = "Library",
        selectedIcon = Icons.Filled.Menu,
        unselectedIcon = Icons.Outlined.Menu,
        route = "library_route"
    ),
    BROWSE(
        title = "Browse",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        route = "browse_route"
    ),
    PROFILE(
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        route = "profile_route",
        hasNews = true
    )
}

@Composable
fun AppNavigationBar(navController: NavHostController) {
    val items = BottomNavigationItem.entries.toTypedArray()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(

                selected = currentRoute == item.route,
                // selected = selectedItemIndex == index,

                onClick = {
                    // selectedItemIndex = index
                    // navController.navigate(item.route)

                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                label = { Text(text = item.title) },
                alwaysShowLabel = false,
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount != null) {
                                Badge {
                                    Text(text = item.badgeCount.toString())
                                }
                            } else if (item.hasNews) {
                                Badge()
                            }
                        }
                    ) {
                        /**
                        Icon(
                        imageVector = if (index == selectedItemIndex) {
                        item.selectedIcon
                        } else item.unselectedIcon,
                        contentDescription = item.title
                        ) */
                        Icon(
                            imageVector = if (currentRoute == item.route) {
                                item.selectedIcon
                            } else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun MyNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavigationItem.HOME.route, // Màn hình bắt đầu
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(BottomNavigationItem.HOME.route) { HomeScreen(navController) }
        composable(BottomNavigationItem.LIBRARY.route) { LibraryScreen(navController) }
        composable(BottomNavigationItem.BROWSE.route) { BrowseScreen(navController) }
        composable(BottomNavigationItem.PROFILE.route) { ProfileScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LibraryScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Library Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BrowseScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍 Search Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Search...") })
        }
    }
}

@Composable
fun ProfileScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(80.dp)
            )
            Text("👤 Profile Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Logout action */ }) {
                Text("Logout")
            }
        }
    }
}

@Preview
@Composable
fun ShowNavigationBarPreview(modifier: Modifier = Modifier) {
    val navController = rememberNavController() // Khởi tạo NavController

    Scaffold(
        bottomBar = { AppNavigationBar(navController) },
    ) { innerPadding -> MyNavHost(navController, innerPadding) }
}

