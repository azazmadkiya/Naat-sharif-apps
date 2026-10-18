package naatsharif.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import naatsharif.data.model.NaatItem
import naatsharif.ui.theme.DarkBackground
import naatsharif.ui.theme.IslamicEmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaatDetailScreen(
    naat: NaatItem,
    viewModel: naatsharif.ui.viewmodel.NaatViewModel,
    onBack: () -> Unit
) {
    val fontSize by viewModel.fontSizeSp.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(naat.title, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicEmeraldPrimary)
            )
        },
        containerColor = if (isDark) DarkBackground else Color(0xFFF6F9F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF18241E) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = naat.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Artist: ${naat.artist}", fontSize = 14.sp, color = IslamicEmeraldPrimary, fontWeight = FontWeight.Medium)
                    Text(text = "Category: ${naat.category}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = naat.lyrics,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF2C2C2C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { viewModel.setFontSize(fontSize - 2f) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Font")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("A-")
                }
                Text("Font Size: ${fontSize.toInt()}sp", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { viewModel.setFontSize(fontSize + 2f) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Font")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("A+")
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
