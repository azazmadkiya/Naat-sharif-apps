package naatsharif.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import naatsharif.ui.theme.DarkBackground
import naatsharif.ui.theme.IslamicEmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    viewModel: naatsharif.ui.viewmodel.NaatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val naats by viewModel.naats.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Classic") }
    var lyrics by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Portal", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Naat", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicEmeraldPrimary)
            )
        },
        containerColor = if (isDark) DarkBackground else Color(0xFFF6F9F7)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Manage Naat Submissions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(naats.filter { !it.isDeleted }) { naat ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF18241E) else Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = naat.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = "Artist: ${naat.artist} • ${naat.category}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (naat.isApproved) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (naat.isApproved) "Approved" else "Pending",
                                    fontSize = 11.sp,
                                    color = if (naat.isApproved) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!naat.isApproved) {
                                TextButton(onClick = { viewModel.approveNaat(naat.id) }) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve", color = Color(0xFF2E7D32), fontSize = 12.sp)
                                }
                            } else {
                                TextButton(onClick = { viewModel.rejectNaat(naat.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", color = Color(0xFFEF6C00), fontSize = 12.sp)
                                }
                            }
                            TextButton(onClick = { viewModel.softDeleteNaat(naat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Naat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = lyrics, onValueChange = { lyrics = it }, label = { Text("Lyrics") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addNaat(title, artist, category, lyrics, "") { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (success) {
                            showAddDialog = false
                            title = ""
                            artist = ""
                            lyrics = ""
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = IslamicEmeraldPrimary)) {
                    Text("Add Naat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
