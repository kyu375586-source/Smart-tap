package com.example.ui.screens

import androidx.compose.animation.core.*
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.monetization.AdBannerCard
import com.example.monetization.RewardedAdDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SoundAndHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val user by viewModel.userProfile.collectAsState()
    val balance by viewModel.userBalance.collectAsState()
    val energy by viewModel.energyRecord.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val particles by viewModel.tapParticles.collectAsState()
    val checkin by viewModel.dailyCheckin.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val aiStrategy by viewModel.aiStrategyText.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showRewardedAd by remember { mutableStateOf(false) }
    var showStrategyDialog by remember { mutableStateOf(false) }
    var coinScale by remember { mutableFloatStateOf(1f) }

    // Coin bounce animation
    val animatedScale by animateFloatAsState(
        targetValue = coinScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "coin_scale"
    )

    // Animated coin balance counter
    val animatedCoins by animateIntAsState(
        targetValue = balance.coinBalance.toInt(),
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "coin_counter"
    )

    // Glowing border rotation
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "halo_angle"
    )

    val maxEnergy = if (config.baseEnergy > 0) config.baseEnergy else energy.maxEnergy
    val energyRatio = (energy.currentEnergy.toFloat() / maxEnergy.toFloat()).coerceIn(0f, 1f)

    if (showRewardedAd) {
        RewardedAdDialog(
            title = "Free Energy Refill & Bonus Coins",
            rewardDescription = "100% Full Energy + 250 Coins",
            onRewardEarned = {
                viewModel.onRewardedAdCompleted(refillEnergy = true, bonusCoins = 250L)
            },
            onDismiss = { showRewardedAd = false }
        )
    }

    if (showStrategyDialog || aiStrategy != null || isAiLoading) {
        AlertDialog(
            onDismissRequest = {
                showStrategyDialog = false
                viewModel.dismissAiStrategy()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Tapping Strategy Advisor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                if (isAiLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Analyzing energy rates and optimal tapping velocity...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                } else {
                    Text(
                        text = aiStrategy ?: "Tap 'Generate Strategy' to receive personalized insights powered by Google Gemini.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStrategyDialog = false
                        viewModel.dismissAiStrategy()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isAiLoading) {
                    OutlinedButton(onClick = { viewModel.generateAiStrategy() }) {
                        Text("Re-analyze", color = Color(0xFFFFD700))
                    }
                }
            },
            containerColor = Color(0xFF131A29),
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offline / Online Notice
        if (!isOnline) {
            item {
                Surface(
                    color = Color(0xFFDC2626),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Offline Mode: Reconnect to synchronize coins and rewards", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Active Announcement Banner
        if (announcements.isNotEmpty()) {
            val announcement = announcements.first()
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E283D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(announcement.heading, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(announcement.description, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Top Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161F30))
                            .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hello, ${user?.username ?: "Tapper"}!",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LVL ${user?.currentLevel ?: 1}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (user?.role == "admin" || viewModel.repository.isCurrentUserAdmin()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { onOpenAdminDashboard() }
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Strategy button & Sound toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            showStrategyDialog = true
                            if (aiStrategy == null) viewModel.generateAiStrategy()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF161F30), CircleShape)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Advisor", tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            SoundAndHaptics.isSoundEnabled = !SoundAndHaptics.isSoundEnabled
                            viewModel.showMessage(if (SoundAndHaptics.isSoundEnabled) "Sound Enabled" else "Sound Muted")
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF161F30), CircleShape)
                    ) {
                        Icon(
                            if (SoundAndHaptics.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Toggle",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Total Balance Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onNavigateToWallet() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF2B3A57), Color(0xFFFFD700).copy(alpha = 0.4f), Color(0xFF2B3A57))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL COIN BALANCE",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = animatedCoins.toString(),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "≈ $${String.format(Locale.US, "%.2f", balance.coinBalance * config.coinToUsdRate)} USD Withdrawable",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // TAPPING COIN ARENA
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow aura
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer { rotationZ = haloAngle }
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.25f),
                                    Color(0xFFFF6D00).copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color(0xFFFFD700).copy(alpha = 0.35f),
                                    Color(0xFFFF6D00).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Large Glowing Golden Coin Button
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(Color(0xFF111726))
                        .border(3.5.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF6D00))), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    coinScale = 0.92f
                                    viewModel.onCoinTapped(context)
                                    tryAwaitRelease()
                                    coinScale = 1f
                                }
                            )
                        }
                        .testTag("main_coin_tap_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.smart_tap_coin_hero),
                        contentDescription = "Golden Coin",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Floating Particle Indicators
                particles.forEach { particle ->
                    val transition = rememberInfiniteTransition(label = "particle")
                    val offsetY by transition.animateFloat(
                        initialValue = particle.yOffset,
                        targetValue = particle.yOffset - 90f,
                        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing)),
                        label = "p_offset"
                    )
                    Text(
                        text = "+${particle.value}",
                        color = Color(0xFFFFD700),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .offset(x = particle.xOffset.dp, y = offsetY.dp)
                            .graphicsLayer { shadowElevation = 8f }
                    )
                }
            }
        }

        // ENERGY PROGRESS BAR
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Energy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "${energy.currentEnergy} / $maxEnergy",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { energyRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFFFFD700),
                        trackColor = Color(0xFF1F293D)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "+${config.energyRegenPerSec}/sec recovery",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (energy.currentEnergy < maxEnergy) "Regenerating..." else "Full Capacity",
                            color = if (energy.currentEnergy < maxEnergy) Color(0xFF38BDF8) else Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // QUICK ACTION SHORTCUTS (Daily Bonus, Watch Ad Refill, Tasks)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily Check-in Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.claimDailyCheckin() }
                        .testTag("daily_bonus_shortcut"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Daily Bonus", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Streak: ${checkin.streakCount}d", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // Refill via Rewarded Ad
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showRewardedAd = true }
                        .testTag("watch_ad_shortcut"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Instant Refill", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Watch Video", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Tasks Shortcut
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTasks() }
                        .testTag("tasks_shortcut"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151C2C)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Earn More", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Quests & Ads", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            }
        }

        // SPONSORED BANNER
        item {
            AdBannerCard(
                modifier = Modifier.padding(top = 8.dp),
                adText = "SMART TAP VIP: Boost multitap x2 for 5 minutes in the Boosters tab!"
            )
        }
    }
}
