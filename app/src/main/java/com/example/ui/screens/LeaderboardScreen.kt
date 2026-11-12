package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LeaderboardScreen(
    viewModel: MainViewModel
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val currentUser by viewModel.userProfile.collectAsState()

    val topUsers = if (leaderboard.isEmpty()) {
        listOf(
            UserProfile(id = "p1", userId = "u1", username = "CryptoKing", totalEarnings = 145000L, currentLevel = 12),
            UserProfile(id = "p2", userId = "u2", username = "TapperPro", totalEarnings = 98500L, currentLevel = 9),
            UserProfile(id = "p3", userId = "u3", username = "GoldSeeker", totalEarnings = 72400L, currentLevel = 7),
            UserProfile(id = "p4", userId = "u4", username = "LightningTap", totalEarnings = 54200L, currentLevel = 6),
            UserProfile(id = "p5", userId = "u5", username = "EliteEarner", totalEarnings = 39800L, currentLevel = 5)
        )
    } else {
        leaderboard
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
                    text = "GLOBAL LEADERBOARD",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Top tappers ranked by verified lifetime earnings",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Podium for top 3
        if (topUsers.size >= 3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place
                    PodiumPillar(
                        user = topUsers[1],
                        rank = 2,
                        medalColor = Color(0xFFC0C0C0),
                        height = 110.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // 1st Place
                    PodiumPillar(
                        user = topUsers[0],
                        rank = 1,
                        medalColor = Color(0xFFFFD700),
                        height = 140.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // 3rd Place
                    PodiumPillar(
                        user = topUsers[2],
                        rank = 3,
                        medalColor = Color(0xFFCD7F32),
                        height = 90.dp
                    )
                }
            }
        }

        item {
            Text(
                text = "ALL RANKINGS",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        itemsIndexed(topUsers) { index, u ->
            val rank = index + 1
            val isCurrent = u.userId == currentUser?.userId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Color(0xFF1F293D) else Color(0xFF111726)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF6D00)))) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#$rank",
                        color = when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(36.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = u.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("YOU", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("Level ${u.currentLevel}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${u.totalEarnings} Coins",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${u.totalTaps} Taps",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumPillar(
    user: UserProfile,
    rank: Int,
    medalColor: Color,
    height: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF161E2E))
                .border(2.dp, medalColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = medalColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.username.take(9),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${user.totalEarnings}",
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(medalColor.copy(alpha = 0.35f), Color(0xFF121826))
                    )
                )
                .border(1.dp, medalColor.copy(alpha = 0.5f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                color = medalColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
    }
}
