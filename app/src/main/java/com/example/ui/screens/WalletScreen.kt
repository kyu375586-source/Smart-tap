package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionItem
import com.example.data.model.WithdrawalRequest
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WalletScreen(
    viewModel: MainViewModel
) {
    val balance by viewModel.userBalance.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val withdrawals by viewModel.withdrawals.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showWithdrawModal by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Withdrawals, 1: Ledger

    var withdrawCoinsInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("PayPal") }
    var paymentDetailsInput by remember { mutableStateOf("") }

    val methods = listOf("PayPal", "USDT", "Bank Transfer", "Gift Card")

    if (showWithdrawModal) {
        val coinsVal = withdrawCoinsInput.toLongOrNull() ?: 0L
        val usdEstimated = coinsVal * config.coinToUsdRate

        AlertDialog(
            onDismissRequest = { showWithdrawModal = false },
            title = {
                Text("Request Payout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Available Balance: ${balance.coinBalance} Coins",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Min withdrawal: ${config.minWithdrawalCoins} coins (Rate: 1,000 coins = $1.00)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = withdrawCoinsInput,
                        onValueChange = { withdrawCoinsInput = it },
                        label = { Text("Coins to withdraw") },
                        placeholder = { Text("Min ${config.minWithdrawalCoins}") },
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_coins_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF2E3B52)
                        )
                    )
                    if (coinsVal > 0) {
                        Text(
                            text = "Estimated: $${String.format(Locale.US, "%.2f", usdEstimated)} USD",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Payment Channel:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        methods.forEach { method ->
                            val isSel = selectedMethod == method
                            Surface(
                                color = if (isSel) Color(0xFFFFD700) else Color(0xFF1E283D),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMethod = method }
                            ) {
                                Text(
                                    text = method,
                                    color = if (isSel) Color.Black else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = paymentDetailsInput,
                        onValueChange = { paymentDetailsInput = it },
                        label = { Text("Account Details ($selectedMethod)") },
                        placeholder = { Text("e.g. Email address or wallet ID") },
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_details_input"),
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
                        val amount = withdrawCoinsInput.toLongOrNull() ?: 0L
                        if (amount >= config.minWithdrawalCoins && paymentDetailsInput.isNotBlank()) {
                            viewModel.requestWithdrawal(amount, selectedMethod, paymentDetailsInput)
                            showWithdrawModal = false
                            withdrawCoinsInput = ""
                            paymentDetailsInput = ""
                        } else {
                            viewModel.showMessage("Please enter valid coins amount and payment details", isError = true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Submit Request", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWithdrawModal = false }) {
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
                    text = "REWARDS WALLET",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Authoritative financial-style balance and payouts ledger",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Wallet Balance Cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF243048)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WITHDRAWABLE REWARD BALANCE",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${balance.withdrawableBalance} Coins",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cash Value: $${String.format(Locale.US, "%.2f", balance.withdrawableBalance * config.coinToUsdRate)} USD",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showWithdrawModal = true },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("withdraw_action_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Lifetime Coins", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${balance.lifetimeCoins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Conversion Rate", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("1k = $1.00 USD", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Tabs: Payout Requests vs Transaction Ledger
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF090C12),
                contentColor = Color(0xFFFFD700),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Payout Requests (${withdrawals.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Activity Ledger", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            if (withdrawals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No withdrawal requests submitted yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else {
                items(withdrawals) { w ->
                    WithdrawalItemCard(w)
                }
            }
        } else {
            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transaction history recorded yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else {
                items(transactions) { tx ->
                    TransactionItemCard(tx)
                }
            }
        }
    }
}

@Composable
fun WithdrawalItemCard(w: WithdrawalRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E283D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${w.paymentMethod} ($${w.withdrawalAmount})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${w.coinAmount} Coins • ${w.paymentDetails}", color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1)
                if (w.adminNotes.isNotBlank()) {
                    Text("Note: ${w.adminNotes}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            val statusColor = when (w.status) {
                "Completed", "Approved" -> Color(0xFF10B981)
                "Processing", "Under Review" -> Color(0xFF38BDF8)
                "Pending" -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = w.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionItemCard(tx: TransactionItem) {
    val isPositive = tx.amount >= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description.ifBlank { tx.type }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(tx.type.replace("_", " ").uppercase(), color = Color(0xFF64748B), fontSize = 10.sp)
            }
            Text(
                text = "${if (isPositive) "+" else ""}${tx.amount}",
                color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
