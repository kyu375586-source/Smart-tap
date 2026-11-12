package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AuthManager
import com.example.supabase.SupabaseConfig
import com.example.supabase.SupabaseSessionManager

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }

    val isSupabaseConfigured = remember { SupabaseConfig.isConfigured(context) }

    // Attempt auto sign in on mount
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            onAuthSuccess = { onAuthSuccess() },
            onUnauthenticated = { /* stay on screen */ }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF06090F),
                        Color(0xFF0F1523),
                        Color(0xFF06090F)
                    )
                )
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Glowing 3D Emblem
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.35f),
                                Color(0xFFFF6D00).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFFFD700).copy(alpha = 0.5f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.smart_tap_icon),
                    contentDescription = "SMART TAP Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SMART TAP",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "Online Coin Tapping & Digital Rewards",
                color = Color(0xFFFFD700),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Selector: Sign In / Create Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131B2E))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isRegisterMode) Color(0xFFFFD700) else Color.Transparent)
                        .clickable {
                            isRegisterMode = false
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_sign_in"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (!isRegisterMode) Color.Black else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isRegisterMode) Color(0xFFFFD700) else Color.Transparent)
                        .clickable {
                            isRegisterMode = true
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_create_account"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        color = if (isRegisterMode) Color.Black else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Username input (if register)
            if (isRegisterMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Player Username", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFD700))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF243048),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F1523),
                        unfocusedContainerColor = Color(0xFF0F1523)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("username_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFFFD700))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF243048),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F1523),
                    unfocusedContainerColor = Color(0xFF0F1523)
                ),
                modifier = Modifier.fillMaxWidth().testTag("email_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFD700))
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF243048),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F1523),
                    unfocusedContainerColor = Color(0xFF0F1523)
                ),
                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Optional Referral Code on Register
            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = { Text("Referral Code (Optional - +500 Coins)", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFD700))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF243048),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F1523),
                        unfocusedContainerColor = Color(0xFF0F1523)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("referral_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Error display
            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1313)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = {
                    if (email.isBlank() || !email.contains("@")) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (isRegisterMode && username.isBlank()) {
                        errorMessage = "Please enter your player username."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    if (isRegisterMode) {
                        AuthManager.signUpWithEmail(
                            context = context,
                            email = email,
                            pass = password,
                            username = username,
                            referralCode = referralCode,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope
                        )
                    } else {
                        AuthManager.signInWithEmail(
                            context = context,
                            email = email,
                            pass = password,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRegisterMode) "Create Account" else "Sign In with Email",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Fill for Admin Test Account
            TextButton(
                onClick = {
                    email = "kyu375586@gmail.com"
                    password = "SmartTapAdmin2026!"
                    username = "AdminKyu"
                },
                modifier = Modifier.testTag("quick_fill_admin_button")
            ) {
                Text(
                    text = "Fill Admin Credentials (${SupabaseSessionManager.ADMIN_EMAIL})",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supabase Status & Config Trigger
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1523))
                    .clickable { showConfigDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (isSupabaseConfigured) Color(0xFF10B981) else Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSupabaseConfigured) "Supabase: Connected" else "Supabase: Local / Offline Mode (Tap to configure)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Supabase Settings Dialog
        if (showConfigDialog) {
            SupabaseConfigDialog(
                onDismiss = { showConfigDialog = false }
            )
        }
    }
}

@Composable
fun SupabaseConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(SupabaseConfig.getSupabaseUrl(context)) }
    var anonKey by remember { mutableStateOf(SupabaseConfig.getAnonKey(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supabase Configuration", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "You can enter your custom Supabase Project URL and Anon Public Key below (or configure them via Secrets / .env):",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase URL", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Supabase Anon Key", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    SupabaseConfig.setCustomConfig(context, url, anonKey)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF111726)
    )
}
