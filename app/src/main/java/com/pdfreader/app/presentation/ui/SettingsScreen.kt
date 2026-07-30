package com.pdfreader.app.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pdfreader.app.domain.model.ThemeMode
import com.pdfreader.app.presentation.mvi.PdfReaderIntent
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.theme.DisplayTitleStyle
import com.pdfreader.app.presentation.theme.LabelCapsStyle
import com.pdfreader.app.presentation.theme.NoxReaderTheme
import com.pdfreader.app.presentation.theme.UiMainStyle
import com.pdfreader.app.presentation.theme.UiSmStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    val preferences = state.preferences
    val spacing = NoxReaderTheme.spacing
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Settings",
                        style = DisplayTitleStyle.copy(fontSize = 24.sp, lineHeight = 28.sp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(
                    start = spacing.marginMobile,
                    top = 12.dp,
                    end = spacing.marginMobile,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SettingsSection(
                        title = "APPEARANCE",
                        description = "Choose an interface that stays comfortable in any light."
                    ) {
                        SettingTitleRow(
                            icon = Icons.Outlined.Brightness6,
                            title = "Theme",
                            subtitle = "Applied across the library and reader"
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = {
                                        viewModel.processIntent(
                                            PdfReaderIntent.SetThemeMode(mode)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = when (mode) {
                                                ThemeMode.System -> "System"
                                                ThemeMode.Light -> "Light"
                                                ThemeMode.Dark -> "Dark"
                                            },
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsSection(
                        title = "READING",
                        description = "Preferences here affect the active reading session."
                    ) {
                        SettingToggleRow(
                            icon = Icons.Outlined.ScreenLockPortrait,
                            title = "Keep screen awake",
                            subtitle = "Prevent the display from sleeping while NoxReader is open",
                            checked = preferences.keepScreenOn,
                            onCheckedChange = {
                                viewModel.processIntent(PdfReaderIntent.SetKeepScreenOn(it))
                            }
                        )
                        SettingsDivider()
                        SettingTitleRow(
                            icon = Icons.Outlined.Headphones,
                            title = "Read-aloud speed",
                            subtitle = "${formatSpeechRate(preferences.speechRate)} speed"
                        )
                        Slider(
                            value = preferences.speechRate,
                            onValueChange = {
                                viewModel.processIntent(PdfReaderIntent.SetSpeechRate(it))
                            },
                            valueRange = 0.6f..1.6f,
                            steps = 9,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Slower", style = UiSmStyle.copy(fontSize = 11.sp))
                            Text("Faster", style = UiSmStyle.copy(fontSize = 11.sp))
                        }
                    }
                }

                item {
                    SettingsSection(
                        title = "PRIVACY & STORAGE",
                        description = "NoxReader keeps your files where you chose them."
                    ) {
                        SettingTitleRow(
                            icon = Icons.Outlined.Lock,
                            title = "On-device history",
                            subtitle = "Recent document names, progress, and bookmarks are stored only on this device"
                        )
                        SettingsDivider()
                        SettingActionRow(
                            icon = Icons.Outlined.DeleteOutline,
                            title = "Clear recent history",
                            subtitle = if (state.recentDocuments.isEmpty()) {
                                "There is no saved history"
                            } else {
                                "Remove ${state.recentDocuments.size} saved document${if (state.recentDocuments.size == 1) "" else "s"}"
                            },
                            enabled = state.recentDocuments.isNotEmpty(),
                            onClick = { showClearHistoryDialog = true }
                        )
                    }
                }

                item {
                    SettingsSection(
                        title = "ABOUT",
                        description = null
                    ) {
                        SettingTitleRow(
                            icon = Icons.Outlined.Info,
                            title = "NoxReader",
                            subtitle = "Version 1.0 · A focused PDF reader"
                        )
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear recent history?") },
            text = {
                Text(
                    "This removes document names, reading progress, and bookmarks from NoxReader. Your PDF files will not be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.processIntent(PdfReaderIntent.ClearRecentDocuments)
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String?,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = LabelCapsStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        description?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = UiSmStyle.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingTitleRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = UiMainStyle.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = UiSmStyle.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = UiMainStyle.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = UiSmStyle.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, enabled)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = UiMainStyle.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
            Text(
                text = subtitle,
                style = UiSmStyle.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingIcon(
    icon: ImageVector,
    enabled: Boolean = true
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

private fun formatSpeechRate(rate: Float): String =
    String.format(Locale.getDefault(), "%.1f×", rate)
