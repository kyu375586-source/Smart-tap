package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.supabase.SupabaseSessionManager
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    TASKS("Tasks", Icons.Default.Checklist),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    LEADERBOARD("Ranks", Icons.Default.Leaderboard),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { SupabaseSessionManager.getInstance(context) }
    val currentUser by sessionManager.currentUser.collectAsState()

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                // Handled reactively via sessionManager.currentUser
            }
        )
    } else {
        val user = currentUser!!
        key(user.id) {
            MainAppContent(
                userId = user.id,
                displayName = user.username,
                email = user.email,
                photoUrl = null,
                onSignedOut = {
                    sessionManager.clearSession()
                }
            )
        }
    }
}

@Composable
fun MainAppContent(
    userId: String,
    displayName: String?,
    email: String?,
    photoUrl: String?,
    onSignedOut: () -> Unit
) {
    val mainViewModel: MainViewModel = viewModel()
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var inAdminMode by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by mainViewModel.uiMessage.collectAsState()

    LaunchedEffect(userId) {
        mainViewModel.bindUser(userId, displayName, email, photoUrl)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.text)
            mainViewModel.dismissMessage()
        }
    }

    if (inAdminMode) {
        AdminDashboardScreen(
            viewModel = mainViewModel,
            onBack = { inAdminMode = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0C1019),
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color(0xFFFFD700) else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFFFFD700) else Color(0xFF64748B)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFF261E00)
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            containerColor = Color(0xFF090C12)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            viewModel = mainViewModel,
                            onNavigateToTasks = { currentTab = AppTab.TASKS },
                            onNavigateToWallet = { currentTab = AppTab.WALLET },
                            onOpenAdminDashboard = { inAdminMode = true }
                        )
                        AppTab.TASKS -> TasksScreen(viewModel = mainViewModel)
                        AppTab.WALLET -> WalletScreen(viewModel = mainViewModel)
                        AppTab.LEADERBOARD -> LeaderboardScreen(viewModel = mainViewModel)
                        AppTab.PROFILE -> ProfileScreen(
                            viewModel = mainViewModel,
                            onOpenAdminDashboard = { inAdminMode = true },
                            onSignedOut = onSignedOut
                        )
                    }
                }
            }
        }
    }
}
