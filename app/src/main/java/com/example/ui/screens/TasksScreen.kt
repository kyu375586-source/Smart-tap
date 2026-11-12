package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.data.model.TaskSubmission
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TasksScreen(
    viewModel: MainViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val submissions by viewModel.userSubmissions.collectAsState()

    var selectedCategory by remember { mutableStateOf("all") }
    var selectedTaskToSubmit by remember { mutableStateOf<TaskItem?>(null) }
    var proofText by remember { mutableStateOf("") }
    var proofUrl by remember { mutableStateOf("") }

    val categories = listOf("all", "daily", "social", "promo", "special", "referral")

    val filteredTasks = if (selectedCategory == "all") {
        tasks
    } else {
        tasks.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    if (selectedTaskToSubmit != null) {
        val targetTask = selectedTaskToSubmit!!
        AlertDialog(
            onDismissRequest = { selectedTaskToSubmit = null },
            title = {
                Text("Submit Proof: ${targetTask.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = targetTask.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reward: +${targetTask.rewardCoins} Coins",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = proofText,
                        onValueChange = { proofText = it },
                        label = { Text("Account handle or verification code") },
                        placeholder = { Text("e.g. @your_handle or proof info") },
                        modifier = Modifier.fillMaxWidth().testTag("proof_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF2E3B52)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = proofUrl,
                        onValueChange = { proofUrl = it },
                        label = { Text("Screenshot or Profile URL (optional)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth().testTag("proof_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF2E3B52)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (proofText.isNotBlank()) {
                            viewModel.submitTask(targetTask, proofText, proofUrl)
                            selectedTaskToSubmit = null
                            proofText = ""
                            proofUrl = ""
                        } else {
                            viewModel.showMessage("Please provide your username or verification details", isError = true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Submit for Review", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedTaskToSubmit = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
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
            .padding(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "REWARD TASKS",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Complete challenges and earn massive coin bonuses",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        color = if (isSelected) Color(0xFFFFD700) else Color(0xFF151C2C),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat.replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Default Sample Tasks if database is empty
        val displayTasks = if (filteredTasks.isEmpty()) {
            listOf(
                TaskItem(id = "sample_1", title = "Follow on X / Twitter", description = "Follow our official handle and like our pinned post", category = "social", rewardCoins = 500L),
                TaskItem(id = "sample_2", title = "Join Official Telegram", description = "Subscribe to our announcement channel for instant codes", category = "social", rewardCoins = 750L),
                TaskItem(id = "sample_3", title = "Invite 3 Friends", description = "Share your referral link with at least 3 active players", category = "referral", rewardCoins = 2500L),
                TaskItem(id = "sample_4", title = "Execute 500 Total Taps", description = "Hit the milestone of 500 taps in a single day", category = "daily", rewardCoins = 400L),
                TaskItem(id = "sample_5", title = "VIP Community Partner Challenge", description = "Explore partner services and verify your participation", category = "special", rewardCoins = 1500L)
            )
        } else {
            filteredTasks
        }

        items(displayTasks) { task ->
            val userSub = submissions.firstOrNull { it.taskId == task.id }
            val isSubmitted = userSub != null
            val subStatus = userSub?.status ?: "none"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF222C3E)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2338)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (task.category) {
                            "social" -> Icons.Default.Share
                            "referral" -> Icons.Default.GroupAdd
                            "daily" -> Icons.Default.Today
                            else -> Icons.Default.Star
                        }
                        Icon(icon, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(task.description, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+${task.rewardCoins} Coins", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    when (subStatus) {
                        "approved" -> {
                            Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("Claimed", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                        "pending" -> {
                            Surface(color = Color(0xFFF59E0B).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("In Review", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                        else -> {
                            Button(
                                onClick = { selectedTaskToSubmit = task },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Start", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
