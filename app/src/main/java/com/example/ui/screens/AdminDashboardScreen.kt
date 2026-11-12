package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppConfiguration
import com.example.data.model.TaskItem
import com.example.data.model.TaskSubmission
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.appConfig.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val submissions by viewModel.allSubmissions.collectAsState()
    val withdrawals by viewModel.allWithdrawals.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) }
    val sectionNames = listOf("Overview", "Remote Config", "Users", "Withdrawals", "Tasks", "Announcements")

    // Modals
    var userToAdjust by remember { mutableStateOf<UserProfile?>(null) }
    var adjustDeltaInput by remember { mutableStateOf("") }
    var adjustReasonInput by remember { mutableStateOf("") }

    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }
    var newTaskReward by remember { mutableStateOf("500") }
    var newTaskCategory by remember { mutableStateOf("daily") }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    var newAnnounceHeading by remember { mutableStateOf("") }
    var newAnnounceDesc by remember { mutableStateOf("") }
    var showAddAnnounceDialog by remember { mutableStateOf(false) }

    // Remote config local editing state
    var editCoinsPerTap by remember(config) { mutableStateOf(config.coinsPerTap.toString()) }
    var editBaseEnergy by remember(config) { mutableStateOf(config.baseEnergy.toString()) }
    var editEnergyPerTap by remember(config) { mutableStateOf(config.energyPerTap.toString()) }
    var editRegenRate by remember(config) { mutableStateOf(config.energyRegenPerSec.toString()) }
    var editMinWithdrawal by remember(config) { mutableStateOf(config.minWithdrawalCoins.toString()) }
    var editMaintenanceMode by remember(config) { mutableStateOf(config.maintenanceMode) }
    var editBannerNotice by remember(config) { mutableStateOf(config.bannerNotice) }

    // User Balance Adjustment Dialog
    if (userToAdjust != null) {
        val target = userToAdjust!!
        AlertDialog(
            onDismissRequest = { userToAdjust = null },
            title = { Text("Adjust Balance: ${target.username}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add positive or negative coin delta to user balance:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustDeltaInput,
                        onValueChange = { adjustDeltaInput = it },
                        label = { Text("Coin Delta (e.g. 500 or -200)") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_adjust_delta_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustReasonInput,
                        onValueChange = { adjustReasonInput = it },
                        label = { Text("Audit Reason") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_adjust_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = adjustDeltaInput.toLongOrNull() ?: 0L
                        if (delta != 0L && adjustReasonInput.isNotBlank()) {
                            viewModel.adminAdjustBalance(target.userId, delta, adjustReasonInput)
                            userToAdjust = null
                            adjustDeltaInput = ""
                            adjustReasonInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Apply Adjustment", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToAdjust = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131A29)
        )
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Create New Reward Task", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text("Description & Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskReward,
                        onValueChange = { newTaskReward = it },
                        label = { Text("Coin Reward") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reward = newTaskReward.toLongOrNull() ?: 500L
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.adminSaveTask(
                                TaskItem(
                                    title = newTaskTitle,
                                    description = newTaskDesc,
                                    category = newTaskCategory,
                                    rewardCoins = reward,
                                    isActive = true
                                )
                            )
                            showAddTaskDialog = false
                            newTaskTitle = ""
                            newTaskDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Save Task", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131A29)
        )
    }

    // Add Announcement Dialog
    if (showAddAnnounceDialog) {
        AlertDialog(
            onDismissRequest = { showAddAnnounceDialog = false },
            title = { Text("Publish Announcement", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAnnounceHeading,
                        onValueChange = { newAnnounceHeading = it },
                        label = { Text("Heading") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAnnounceDesc,
                        onValueChange = { newAnnounceDesc = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAnnounceHeading.isNotBlank()) {
                            viewModel.adminSaveAnnouncement(
                                AnnouncementItem(
                                    heading = newAnnounceHeading,
                                    description = newAnnounceDesc,
                                    isActive = true
                                )
                            )
                            showAddAnnounceDialog = false
                            newAnnounceHeading = ""
                            newAnnounceDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Publish Banner", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAnnounceDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131A29)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administrator Control Center", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1523),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF090C12)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Section Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                containerColor = Color(0xFF0F1523),
                contentColor = Color(0xFFFFD700),
                edgePadding = 12.dp
            ) {
                sectionNames.forEachIndexed { idx, name ->
                    Tab(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        text = { Text(name, fontWeight = if (selectedSection == idx) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // OVERVIEW
                if (selectedSection == 0) {
                    item {
                        Text("SYSTEM TELEMETRY & OVERVIEW", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(title = "Total Users", value = "${users.size}", modifier = Modifier.weight(1f))
                            MetricCard(title = "Active Tappers", value = "${users.count { it.status == "active" }}", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(title = "Pending Payouts", value = "${withdrawals.count { it.status == "Pending" }}", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                            MetricCard(title = "Approved Payouts", value = "${withdrawals.count { it.status == "Approved" || it.status == "Completed" }}", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard(title = "Total Tasks", value = "${tasks.size}", modifier = Modifier.weight(1f))
                            MetricCard(title = "Submissions to Review", value = "${submissions.count { it.status == "pending" }}", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Backend Engine Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Google Cloud Firestore (Real-time sync active)", color = Color(0xFF10B981), fontSize = 12.sp)
                                Text("• Firebase Authentication (Google Credential Manager)", color = Color(0xFF10B981), fontSize = 12.sp)
                                Text("• App Check Attestation & Security Rules (Zero-trust ABAC)", color = Color(0xFF10B981), fontSize = 12.sp)
                                Text("• Firebase AI Logic (Gemini 3.8 Flash)", color = Color(0xFF10B981), fontSize = 12.sp)
                                Text("• Remote Config Version: v${config.version}", color = Color(0xFFFFD700), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // REMOTE CONFIGURATION
                else if (selectedSection == 1) {
                    item {
                        Text("DYNAMIC APPLICATION CONFIGURATION", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Changes broadcast immediately to all connected clients in real time.", color = Color(0xFF64748B), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = editCoinsPerTap,
                            onValueChange = { editCoinsPerTap = it },
                            label = { Text("Coins per tap") },
                            modifier = Modifier.fillMaxWidth().testTag("config_coins_per_tap")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editBaseEnergy,
                            onValueChange = { editBaseEnergy = it },
                            label = { Text("Base Max Energy") },
                            modifier = Modifier.fillMaxWidth().testTag("config_base_energy")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editEnergyPerTap,
                            onValueChange = { editEnergyPerTap = it },
                            label = { Text("Energy consumed per tap") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editRegenRate,
                            onValueChange = { editRegenRate = it },
                            label = { Text("Energy regenerated per second") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editMinWithdrawal,
                            onValueChange = { editMinWithdrawal = it },
                            label = { Text("Minimum coins to withdraw") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editBannerNotice,
                            onValueChange = { editBannerNotice = it },
                            label = { Text("Global Banner Notice (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Maintenance Mode Lock", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = editMaintenanceMode,
                                onCheckedChange = { editMaintenanceMode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700))
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val newCfg = config.copy(
                                    coinsPerTap = editCoinsPerTap.toLongOrNull() ?: config.coinsPerTap,
                                    baseEnergy = editBaseEnergy.toLongOrNull() ?: config.baseEnergy,
                                    energyPerTap = editEnergyPerTap.toLongOrNull() ?: config.energyPerTap,
                                    energyRegenPerSec = editRegenRate.toLongOrNull() ?: config.energyRegenPerSec,
                                    minWithdrawalCoins = editMinWithdrawal.toLongOrNull() ?: config.minWithdrawalCoins,
                                    maintenanceMode = editMaintenanceMode,
                                    bannerNotice = editBannerNotice
                                )
                                viewModel.adminSaveConfig(newCfg)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_config_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish Configuration Update", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // USERS MANAGEMENT
                else if (selectedSection == 2) {
                    item {
                        Text("USER DIRECTORY (${users.size})", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(users) { u ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(u.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (u.status == "active") Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(u.status.uppercase(), color = if (u.status == "active") Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(u.email ?: "No email", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("Earnings: ${u.totalEarnings} • Taps: ${u.totalTaps}", color = Color(0xFFFFD700), fontSize = 11.sp)
                                }

                                Row {
                                    IconButton(onClick = { userToAdjust = u }) {
                                        Icon(Icons.Default.PriceChange, contentDescription = "Adjust", tint = Color(0xFFFFD700))
                                    }
                                    IconButton(onClick = {
                                        val newSt = if (u.status == "active") "suspended" else "active"
                                        viewModel.adminSetUserStatus(u.userId, newSt)
                                    }) {
                                        Icon(if (u.status == "active") Icons.Default.Block else Icons.Default.CheckCircle, contentDescription = "Toggle Status", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }

                // WITHDRAWALS MANAGEMENT
                else if (selectedSection == 3) {
                    item {
                        Text("PAYOUT REQUESTS (${withdrawals.size})", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(withdrawals) { w ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${w.username} • ${w.paymentMethod}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$${w.withdrawalAmount} USD (${w.coinAmount} C)", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                }
                                Text("Details: ${w.paymentDetails}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("Status: ${w.status}", color = Color(0xFF38BDF8), fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (w.status == "Pending" || w.status == "Under Review") {
                                        Button(
                                            onClick = { viewModel.adminUpdateWithdrawal(w, "Approved", "Approved by administrator") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Approve", color = Color.White, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.adminUpdateWithdrawal(w, "Completed", "Payment disbursed successfully") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Complete", color = Color.White, fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.adminUpdateWithdrawal(w, "Rejected", "Declined: verification failed") },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reject & Refund", color = Color(0xFFEF4444), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TASKS & SUBMISSIONS
                else if (selectedSection == 4) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TASK MANAGEMENT", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Button(
                                onClick = { showAddTaskDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ New Task", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        Text("PENDING USER SUBMISSIONS (${submissions.count { it.status == "pending" }})", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val pendingSubs = submissions.filter { it.status == "pending" }
                    if (pendingSubs.isEmpty()) {
                        item {
                            Text("No pending task submissions to review.", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(pendingSubs) { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F30)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${sub.username} on ${sub.taskTitle}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Proof: ${sub.proofText}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    if (sub.proofUrl.isNotBlank()) {
                                        Text("URL: ${sub.proofUrl}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.adminReviewSubmission(sub, approve = true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Approve (+${sub.rewardCoins}C)", color = Color.White, fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.adminReviewSubmission(sub, approve = false) },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Reject", color = Color(0xFFEF4444), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ACTIVE TASKS IN APP", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(tasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("+${task.rewardCoins} Coins • Category: ${task.category}", color = Color(0xFFFFD700), fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.adminDeleteTask(task.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // ANNOUNCEMENTS
                else if (selectedSection == 5) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ANNOUNCEMENT BANNERS", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Button(
                                onClick = { showAddAnnounceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Post Banner", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(announcements) { a ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(a.heading, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(a.description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                                IconButton(onClick = {
                                    viewModel.adminDeleteAnnouncement(a.id)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
