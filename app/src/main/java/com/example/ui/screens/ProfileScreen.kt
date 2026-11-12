package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthManager
import com.example.data.model.Booster
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SoundAndHaptics

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenAdminDashboard: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val user by viewModel.userProfile.collectAsState()
    val balance by viewModel.userBalance.collectAsState()
    val boosters by viewModel.boosters.collectAsState()
    val userBoosters by viewModel.userBoosters.collectAsState()

    var referralInput by remember { mutableStateOf("") }
    var isSoundOn by remember { mutableStateOf(SoundAndHaptics.isSoundEnabled) }
    var isVibOn by remember { mutableStateOf(SoundAndHaptics.isVibrationEnabled) }

    val isAdmin = user?.role == "admin" || viewModel.repository.isCurrentUserAdmin()

    // Default boosters if database is initial
    val displayBoosters = if (boosters.isEmpty()) {
        listOf(
            Booster(id = "b1", name = "Multitap x2", description = "Doubles coins earned on every tap for 5 minutes", price = 300L, durationSeconds = 300L, boosterType = "multitap", multiplier = 2),
            Booster(id = "b2", name = "Energy Capacity Boost", description = "Increases maximum energy capacity by +500", price = 600L, durationSeconds = 600L, boosterType = "energy_boost", multiplier = 1),
            Booster(id = "b3", name = "Turbo Recharge", description = "Triples energy regeneration rate per second", price = 500L, durationSeconds = 300L, boosterType = "energy_recovery", multiplier = 3)
        )
    } else {
        boosters
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
            .padding(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "PROFILE & SETTINGS",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Profile Identity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF243048)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D))
                            .border(2.dp, Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user?.username ?: "Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("ADMIN", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(user?.email ?: "Google Account", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("UID: ${user?.userId?.take(10) ?: ""}...", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }
        }

        // Referral System Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Referral Program", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share your referral code. You and your invited friend both receive bonus coins!",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A2338))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = user?.referralCode?.ifBlank { "TAP_VIP" } ?: "TAP_VIP",
                            color = Color(0xFFFFD700),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Referral Code", user?.referralCode ?: "")
                                clipboard?.setPrimaryClip(clip)
                                viewModel.showMessage("Referral code copied to clipboard!")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (user?.referredBy.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = referralInput,
                                onValueChange = { referralInput = it },
                                placeholder = { Text("Enter friend's code", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(50.dp).testTag("enter_referral_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF243048)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (referralInput.isNotBlank()) {
                                        viewModel.applyReferralCode(referralInput)
                                        referralInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Claim", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Available Boosters Card
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("BOOSTERS & UPGRADES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Multiply coins and maximize tapping speed", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }

        items(displayBoosters) { booster ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(booster.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(booster.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.purchaseBooster(booster) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("${booster.price} C", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Preferences & Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PREFERENCES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tapping Sound Effects", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                        Switch(
                            checked = isSoundOn,
                            onCheckedChange = {
                                isSoundOn = it
                                SoundAndHaptics.isSoundEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0xFF3B2D00)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1F293D), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Haptic Feedback (Vibration)", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                        Switch(
                            checked = isVibOn,
                            onCheckedChange = {
                                isVibOn = it
                                SoundAndHaptics.isVibrationEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0xFF3B2D00)
                            )
                        )
                    }
                }
            }
        }

        // Admin Portal Button (for administrators)
        if (isAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenAdminDashboard() }
                        .testTag("admin_dashboard_entry_button"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2636)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Administrator Control Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Manage users, configs, tasks, and payouts", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF38BDF8))
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = {
                        AuthManager.signOut(context, coroutineScope, onSignedOut)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
