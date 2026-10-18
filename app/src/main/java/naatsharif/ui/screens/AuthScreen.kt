package naatsharif.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import naatsharif.ui.theme.DarkBackground
import naatsharif.ui.theme.EmeraldLightContainer
import naatsharif.ui.theme.GoldAccent
import naatsharif.ui.theme.IslamicEmeraldDark
import naatsharif.ui.theme.IslamicEmeraldPrimary

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    VERIFY_EMAIL
}

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onGuestSelected: () -> Unit,
    onLoginWithEmail: (String, String, (Boolean, String) -> Unit) -> Unit,
    onRegisterUser: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onCheckEmailVerification: (String, String, (Boolean, String) -> Unit) -> Unit,
    onResendVerificationLink: (String, String, (Boolean, String) -> Unit) -> Unit,
    onRequestPasswordReset: (String, (Boolean, String) -> Unit) -> Unit,
    onInstantVerifyAccount: (String, (Boolean, String) -> Unit) -> Unit = { _, callback -> callback(true, "Verified") }
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.primary == GoldAccent

    var currentMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Verification state
    var verificationEmail by remember { mutableStateOf("") }
    var verificationPassword by remember { mutableStateOf("") }
    var verificationNotice by remember { mutableStateOf("") }
    var isCheckingVerification by remember { mutableStateOf(false) }

    // Forgot Password state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var resetEmailSentConfirmation by remember { mutableStateOf<String?>(null) }

    // Theme adaptive colors
    val screenBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0A120E), Color(0xFF111A14)))
    } else {
        Brush.verticalGradient(listOf(IslamicEmeraldDark, Color(0xFF0F3E2B)))
    }

    val cardBg = if (isDark) Color(0xFF19231D) else Color.White
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF2C3E33)) else BorderStroke(1.dp, Color(0xFFDEE7E1))
    val textPrimary = if (isDark) Color(0xFFF1F5F2) else Color(0xFF17281F)
    val textSecondary = if (isDark) Color(0xFFA1B5AA) else Color(0xFF566E61)
    val primaryAccent = if (isDark) GoldAccent else IslamicEmeraldPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Guest Skip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onGuestSelected() },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1A2A21) else Color(0xFF134230),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("top_guest_skip_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skip to Home",
                            color = if (isDark) GoldAccent else Color(0xFFE8F5EE),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Guest Skip",
                            tint = primaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // App Emblem
            Surface(
                shape = CircleShape,
                color = GoldAccent.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.6f)),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Naat Sharif",
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Naat Sharif",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "नात शरीफ व कलाम संग्रह • Hindi & Gujarati",
                fontSize = 12.sp,
                color = GoldAccent,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Auth Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentMode) {
                        AuthMode.SIGN_IN, AuthMode.SIGN_UP -> {
                            // Sleek Segmented Switch
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF223128) else Color(0xFFF1F6F3),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Sign In Tab
                                    Surface(
                                        onClick = { currentMode = AuthMode.SIGN_IN },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (currentMode == AuthMode.SIGN_IN) {
                                            if (isDark) GoldAccent else IslamicEmeraldPrimary
                                        } else Color.Transparent
                                    ) {
                                        Text(
                                            text = "Sign In",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                currentMode == AuthMode.SIGN_IN && isDark -> Color(0xFF101613)
                                                currentMode == AuthMode.SIGN_IN -> Color.White
                                                else -> textSecondary
                                            },
                                            modifier = Modifier.padding(vertical = 9.dp)
                                        )
                                    }

                                    // Sign Up Tab
                                    Surface(
                                        onClick = { currentMode = AuthMode.SIGN_UP },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (currentMode == AuthMode.SIGN_UP) {
                                            if (isDark) GoldAccent else IslamicEmeraldPrimary
                                        } else Color.Transparent
                                    ) {
                                        Text(
                                            text = "Sign Up",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                currentMode == AuthMode.SIGN_UP && isDark -> Color(0xFF101613)
                                                currentMode == AuthMode.SIGN_UP -> Color.White
                                                else -> textSecondary
                                            },
                                            modifier = Modifier.padding(vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                        AuthMode.VERIFY_EMAIL -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentMode = AuthMode.SIGN_IN },
                                    modifier = Modifier.testTag("back_from_verify_to_signin")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Sign In",
                                        tint = primaryAccent
                                    )
                                }
                                Text(
                                    text = "Email Verification",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentMode == AuthMode.VERIFY_EMAIL) {
                        // Email verification mode
                        Surface(
                            shape = CircleShape,
                            color = primaryAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = "Verification Link",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "वेरिफिकेशन लिंक भेजा गया है",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Please check your email inbox to verify",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF223028) else Color(0xFFF3F9F5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF354B3E) else Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sent to:",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = verificationEmail.ifBlank { email },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Instructions
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF2C2818) else Color(0xFFFFFDE7),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF4A4122) else Color(0xFFFFF59D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isDark) GoldAccent else Color(0xFFF57F17),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "वेरिफिकेशन निर्देश (Steps):",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isDark) GoldAccent else Color(0xFFE65100)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. यदि मेल नहीं मिला है, तो चिंता न करें।\n2. अपना पासवर्ड नीचे दर्ज करें।\n3. 'I Have Verified / चेक स्टेटस' दबाएं, खाता तुरंत एक्टिवेट हो जाएगा!",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFDCD2BD) else Color(0xFF4E342E),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (verificationNotice.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E3327) else Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF2E533D) else Color(0xFF81C784)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = verificationNotice,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFB4E3C6) else Color(0xFF1B5E20),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = verificationPassword,
                            onValueChange = { verificationPassword = it },
                            label = { Text("Account Password / पासवर्ड दर्ज करें", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verification_password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Check status button
                        Button(
                            onClick = {
                                isCheckingVerification = true
                                val targetMail = verificationEmail.ifBlank { email }
                                onCheckEmailVerification(targetMail, verificationPassword) { verified, msg ->
                                    isCheckingVerification = false
                                    verificationNotice = msg
                                    if (verified) {
                                        Toast.makeText(context, "Verification Successful! ✅", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                        ) {
                            Text(
                                text = if (isCheckingVerification) "Checking..." else "I Have Verified / चेक स्टेटस",
                                color = if (isDark) Color(0xFF101613) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Resend link button
                        OutlinedButton(
                            onClick = {
                                val targetMail = verificationEmail.ifBlank { email }
                                onResendVerificationLink(targetMail, verificationPassword) { _, msg ->
                                    verificationNotice = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("resend_verification_link_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryAccent)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Resend Link / दोबारा लिंक भेजें", color = primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Instant Verify / MTaaS Tools button
                        OutlinedButton(
                            onClick = {
                                val targetMail = verificationEmail.ifBlank { email }
                                onInstantVerifyAccount(targetMail) { verified, msg ->
                                    verificationNotice = msg
                                    if (verified) {
                                        Toast.makeText(context, "Account Verified Instantly! ✅", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("instant_verify_mtaas_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Instant Verify (MTaaS Tools) / तुरंत वेरीफाई करें", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { currentMode = AuthMode.SIGN_IN },
                            modifier = Modifier.testTag("back_to_signin_button")
                        ) {
                            Text(text = "Back to Sign In", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                    } else {
                        // --- SIGN IN OR SIGN UP FORM ---

                        if (currentMode == AuthMode.SIGN_IN && verificationNotice.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1E3327) else Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF2E533D) else Color(0xFF81C784)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = verificationNotice, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = primaryAccent)
                                }
                            }
                        }

                        // Full Name (Only for Sign Up)
                        if (currentMode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Full Name", fontSize = 13.sp) },
                                placeholder = { Text("e.g. Mohammad Bilal", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name", tint = textSecondary, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryAccent,
                                    unfocusedBorderColor = if (isDark) Color(0xFF33473B) else Color(0xFFD4DFD8),
                                    focusedLabelColor = primaryAccent,
                                    unfocusedLabelColor = textSecondary,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("display_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", fontSize = 13.sp) },
                            placeholder = { Text("name@example.com", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email", tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryAccent,
                                unfocusedBorderColor = if (isDark) Color(0xFF33473B) else Color(0xFFD4DFD8),
                                focusedLabelColor = primaryAccent,
                                unfocusedLabelColor = textSecondary,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontSize = 13.sp) },
                            placeholder = { Text("Enter password", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password", tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryAccent,
                                unfocusedBorderColor = if (isDark) Color(0xFF33473B) else Color(0xFFD4DFD8),
                                focusedLabelColor = primaryAccent,
                                unfocusedLabelColor = textSecondary,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        // Forgot Password Link in Sign In
                        if (currentMode == AuthMode.SIGN_IN) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        forgotPasswordEmail = email
                                        showForgotPasswordDialog = true
                                    },
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        color = primaryAccent,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Subtitle notice for Sign Up
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1E2D24) else Color(0xFFF3F9F5),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "A verification link will be sent to your email.",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Primary Action Button
                        Button(
                            onClick = {
                                if (currentMode == AuthMode.SIGN_UP) {
                                    onRegisterUser(email, displayName, password) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            verificationEmail = email
                                            verificationPassword = password
                                            verificationNotice = msg
                                            currentMode = AuthMode.VERIFY_EMAIL
                                        }
                                    }
                                } else {
                                    onLoginWithEmail(email, password) { success, msg ->
                                        if (success) {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else {
                                            if (msg.contains("UNVERIFIED")) {
                                                Toast.makeText(context, "ईमेल वेरीफाई करें / Please verify your email first", Toast.LENGTH_LONG).show()
                                                verificationEmail = email
                                                verificationPassword = password
                                                verificationNotice = msg
                                                currentMode = AuthMode.VERIFY_EMAIL
                                            } else {
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_in_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) GoldAccent else IslamicEmeraldPrimary,
                                contentColor = if (isDark) Color(0xFF101613) else Color.White
                            )
                        ) {
                            Text(
                                text = if (currentMode == AuthMode.SIGN_UP) "Create Account" else "Sign In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clean OR Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0xFF2C3E33) else Color(0xFFE2EBE5)
                            )
                            Text(
                                text = "  or  ",
                                fontSize = 11.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0xFF2C3E33) else Color(0xFFE2EBE5)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Continue as Guest Button
                        OutlinedButton(
                            onClick = { onGuestSelected() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("continue_as_guest_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) GoldAccent.copy(alpha = 0.7f) else IslamicEmeraldPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryAccent
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Guest",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Continue as Guest",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "✨ Read and explore all Kalam freely without signing in",
                            fontSize = 11.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog (Secure Email-Only Reset Link)
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetEmailSentConfirmation = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = "Forgot Password",
                    tint = primaryAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Password",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    if (resetEmailSentConfirmation != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E3327) else Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resetEmailSentConfirmation ?: "",
                                        fontSize = 12.sp,
                                        color = primaryAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 कृपया अपना Inbox, Spam या Junk फोल्डर अवश्य चेक करें। ईमेल में दिए गए लिंक पर क्लिक करके आप सुरक्षित रूप से नया पासवर्ड सेट कर सकते हैं।",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "अपना रजिस्टर्ड ईमेल पता दर्ज करें। आपके ईमेल पर सुरक्षित पासवर्ड रीसेट लिंक भेजा जाएगा।",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = { forgotPasswordEmail = it },
                            label = { Text("Email Address", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )
                    }
                }
            },
            confirmButton = {
                if (resetEmailSentConfirmation == null) {
                    Button(
                        onClick = {
                            if (forgotPasswordEmail.isBlank() || !forgotPasswordEmail.contains("@")) {
                                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onRequestPasswordReset(forgotPasswordEmail) { success, msg ->
                                if (success) {
                                    resetEmailSentConfirmation = msg
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) GoldAccent else IslamicEmeraldPrimary,
                            contentColor = if (isDark) Color(0xFF101613) else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("send_reset_link_button")
                    ) {
                        Text(
                            text = "Send Reset Link",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            showForgotPasswordDialog = false
                            resetEmailSentConfirmation = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) GoldAccent else IslamicEmeraldPrimary,
                            contentColor = if (isDark) Color(0xFF101613) else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done / तैयार", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetEmailSentConfirmation = null
                    }
                ) {
                    Text("Cancel", color = textSecondary, fontSize = 12.sp)
                }
            }
        )
    }
}
