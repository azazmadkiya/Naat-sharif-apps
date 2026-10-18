package naatsharif.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import naatsharif.data.model.NaatItem
import naatsharif.data.model.UserRole
import naatsharif.ui.theme.DarkBackground
import naatsharif.ui.theme.IslamicEmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: naatsharif.ui.viewmodel.NaatViewModel,
    onNaatClick: (NaatItem) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAdminPortalClick: () -> Unit,
    onSuperAdminPortalClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val naats by viewModel.naats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    val categories = listOf("All", "Classic", "Ramzan", "Madani", "Coke Studio")

    val filteredNaats = naats.filter { !it.isDeleted && (profile?.role == UserRole.SUPER_ADMIN || profile?.role == UserRole.ADMIN || it.isApproved) }
        .filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Naat Sharif", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicEmeraldPrimary),
                actions = {
                    if (profile?.role == UserRole.SUPER_ADMIN || profile?.role == UserRole.ADMIN) {
                        IconButton(onClick = onAdminPortalClick) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Portal", tint = Color.White)
                        }
                    }
                    if (profile?.role == UserRole.SUPER_ADMIN) {
                        IconButton(onClick = onSuperAdminPortalClick) {
                            Icon(Icons.Default.Security, contentDescription = "Super Admin Portal", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        containerColor = if (isDark) DarkBackground else Color(0xFFF6F9F7)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search Naats by title, artist...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = searchQuery.equals(cat, ignoreCase = true),
                            onClick = { onCategoryClick(cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured Naats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            items(filteredNaats) { naat ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF18241E) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNaatClick(naat) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = IslamicEmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = IslamicEmeraldPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = naat.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${naat.artist} • ${naat.category}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
