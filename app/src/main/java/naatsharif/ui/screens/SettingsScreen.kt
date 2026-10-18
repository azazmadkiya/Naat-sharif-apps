package naatsharif.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import naatsharif.notification.NotificationHelper
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import naatsharif.data.model.UserRole
import naatsharif.ui.theme.EmeraldLightContainer
import naatsharif.ui.theme.GoldAccent
import naatsharif.ui.theme.IslamicEmeraldPrimary
import naatsharif.ui.viewmodel.NaatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NaatViewModel,
    onBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDataSafetyDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }

    val isSuperAdmin = userProfile != null && !userProfile!!.isGuest && userProfile!!.role == UserRole.SUPER_ADMIN
    val canAccessPortal = userProfile != null && !userProfile!!.isGuest && userProfile!!.role.canAccessAdminPortal()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.sendNotification(
                context,
                "Naat Sharif Alert",
                "🔔 Notifications enabled! You will receive new Kalam updates."
            )
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicEmeraldPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldLightContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(IslamicEmeraldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userProfile?.displayName ?: "Guest User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicEmeraldPrimary
                        )

                        Text(
                            text = if (userProfile?.isGuest == true) "Viewer / Guest Mode" else "Role: ${userProfile?.role?.label} (${userProfile?.email})",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Super Admin Features Section (Only for Super Admin: azazmadkiya@gmail.com)
            if (canAccessPortal) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = IslamicEmeraldPrimary),
                    border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.padding(end = 6.dp)) {
                                    Text(
                                        text = if (isSuperAdmin) "Super Admin Controls" else "Contributor & Add Naat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isSuperAdmin) "Exclusive Tools • /Azazmadkiya" else "Add Naat Sharif & manage submissions",
                                        fontSize = 11.sp,
                                        color = GoldAccent,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    text = if (isSuperAdmin) "SUPER ADMIN" else if (userProfile?.role == UserRole.ADMIN) "ADMIN" else "CONTRIBUTOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicEmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Feature 1: Instagram Card Generator (Super Admin)


                        // Feature 2: Admin Portal & Management Dashboard
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAdmin() }
                                .testTag("super_admin_portal_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSuperAdmin) "Super Admin Portal & Roles Dashboard" else "Add Naat Sharif & Contributor Portal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isSuperAdmin) "Category management, pending approvals & trash bin" else "Tap here to add, view & manage Naat Sharif submissions",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                            }
                        }
                    }
                }
            }

            // Donation Banner Card (Matching Screenshot 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IslamicEmeraldPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Naat Sharif / Kalam Sharif Hindi - Gujarati",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { showDonationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("donate_button")
                    ) {
                        Text(
                            text = "डोनेशन करे",
                            color = IslamicEmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Reading Text Size Slider Card (Matching Screenshot 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = IslamicEmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reading Text Size (${fontSizeSp.toInt()} sp)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Adjust font size for reading lyrics comfortably",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = fontSizeSp,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 12f..32f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = IslamicEmeraldPrimary,
                            activeTrackColor = IslamicEmeraldPrimary
                        ),
                        modifier = Modifier.testTag("font_size_slider")
                    )
                }
            }

            // Theme Mode Selection Card with Switch Option (Light vs Dark Mode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (themeMode == "dark_slate") GoldAccent.copy(alpha = 0.2f) else EmeraldLightContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (themeMode == "dark_slate") Icons.Default.Nightlight else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (themeMode == "dark_slate") GoldAccent else IslamicEmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Theme / ऐप थीम",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (themeMode == "dark_slate") "🌙 Dark Mode On / डार्क मोड चालू" else "☀️ Light Mode On / लाइट मोड चालू",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Switch Option for Light / Dark Mode
                        Switch(
                            checked = themeMode == "dark_slate",
                            onCheckedChange = { isDark ->
                                viewModel.setThemeMode(if (isDark) "dark_slate" else "emerald_light")
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (themeMode == "dark_slate") Icons.Default.Nightlight else Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldAccent,
                                checkedTrackColor = IslamicEmeraldPrimary,
                                checkedIconColor = Color(0xFF101412),
                                uncheckedThumbColor = IslamicEmeraldPrimary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.setThemeMode("emerald_light") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emerald_light_theme_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (themeMode != "dark_slate") IslamicEmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (themeMode != "dark_slate") BorderStroke(1.dp, IslamicEmeraldPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (themeMode != "dark_slate") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Light / लाइट",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (themeMode != "dark_slate") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { viewModel.setThemeMode("dark_slate") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dark_slate_theme_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (themeMode == "dark_slate") IslamicEmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (themeMode == "dark_slate") BorderStroke(1.dp, GoldAccent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nightlight,
                                    contentDescription = null,
                                    tint = if (themeMode == "dark_slate") GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dark / डार्क",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (themeMode == "dark_slate") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
                        // Share App Tile
            SettingsTile(
                icon = Icons.Default.Share,
                title = "Share App",
                subtitle = "Share with friends & family",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Naat Sharif App")
                        putExtra(Intent.EXTRA_TEXT, "Download Naat Sharif - Hindi & Gujarati Lyrics Collection App: https://play.google.com/store/apps/details?id=com.aistudio.naatsharif")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Naat Sharif App"))
                }
            )

            // Test Notifications Tile
            SettingsTile(
                icon = Icons.Default.Notifications,
                title = "Test Notifications / नोटिफिकेशन टेस्ट",
                subtitle = "Tap to enable & test app notifications",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationHelper.sendNotification(
                            context,
                            "Naat Sharif Alert",
                            "🔔 Test Notification: New Naat Sharif added by Azazmadkiya!"
                        )
                        Toast.makeText(context, "🔔 Test notification sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Rate Us Tile
            SettingsTile(
                icon = Icons.Default.Star,
                title = "Rate Us",
                subtitle = "Give 5 stars on Play Store",
                onClick = {
                    Toast.makeText(context, "Thank you for rating 5 stars!", Toast.LENGTH_SHORT).show()
                }
            )

            // Legal & Compliance Section (Matching Screenshots 4 & 5)
            Text(
                text = "Legal, Policies & Compliance",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = IslamicEmeraldPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            SettingsTile(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy / प्रायवेसी पॉलिसी",
                subtitle = "User data, Firebase storage & zero tracking",
                onClick = { showPrivacyDialog = true }
            )

            SettingsTile(
                icon = Icons.Default.Description,
                title = "Terms & Conditions / नियम व शर्ते",
                subtitle = "Spiritual usage, poetry rights & moderation",
                onClick = { showTermsDialog = true }
            )

            SettingsTile(
                icon = Icons.Default.Delete,
                title = "Data Safety & Account Deletion",
                subtitle = "Google Play compliance & permanent deletion request",
                onClick = { showDataSafetyDialog = true }
            )

            SettingsTile(
                icon = Icons.Default.Login,
                title = if (userProfile?.isGuest == true) "Login / Switch Account" else "Logout / Switch Account (${userProfile?.displayName})",
                subtitle = "Sign in or switch to another account",
                onClick = {
                    if (userProfile?.isGuest == false) {
                        viewModel.logout()
                    }
                    onNavigateToAuth()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Footer Card (Matching Screenshot 5)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldLightContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Developed By ",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Azazmadkiya",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicEmeraldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPrivacyDialog) PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    if (showTermsDialog) TermsConditionsDialog(onDismiss = { showTermsDialog = false })
    if (showDataSafetyDialog) DataSafetyDialog(onDismiss = { showDataSafetyDialog = false })
    if (showDonationDialog) DonationDialog(onDismiss = { showDonationDialog = false })
}

@Composable
fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IslamicEmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
