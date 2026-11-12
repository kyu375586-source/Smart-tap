package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    TASKS("Tasks", Icons.Default.Checklist),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    LEADERBOARD("Ranks", Icons.Default.Leaderboard),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainAppContent(
                userId = uid,
                displayName = currentUser?.displayName,
                email = currentUser?.email,
                photoUrl = currentUser?.photoUrl?.toString(),
                onSignedOut = {
                    currentUser = null
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
