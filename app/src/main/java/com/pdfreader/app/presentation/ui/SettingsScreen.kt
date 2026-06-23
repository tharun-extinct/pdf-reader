package com.pdfreader.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pdfreader.app.presentation.mvi.PdfReaderViewModel
import com.pdfreader.app.presentation.theme.DisplayTitleStyle
import com.pdfreader.app.presentation.theme.LabelCapsStyle
import com.pdfreader.app.presentation.theme.UiMainStyle
import com.pdfreader.app.presentation.theme.UiSmStyle
import com.pdfreader.app.presentation.theme.LibroTheme

/**
 * Settings screen matching the Stitch "Libro Settings" design.
 *
 * Sectioned layout with:
 * - Reading Preferences (font size, line height, reading font)
 * - Theme (appearance mode, accent color)
 * - Sync & Storage (Google Drive sync, storage usage, clear cache)
 * - About (version, licenses)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PdfReaderViewModel,
    navController: NavController
) {
    val spacing = LibroTheme.spacing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = {
                    Text(
                        text = "Settings",
                        style = DisplayTitleStyle.copy(fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.marginMobile, vertical = spacing.gutter)
        ) {
            // ── Reading Preferences ─────────────────────────────────
            SettingsSection(title = "READING PREFERENCES") {
                // Font size slider
                var fontSize by remember { mutableFloatStateOf(18f) }
                SettingsRow(
                    icon = Icons.Outlined.FormatSize,
                    title = "Font Size",
                    subtitle = "${fontSize.toInt()}sp"
                ) {
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 12f..32f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                SettingsDivider()

                // Line height toggle
                var compactLines by remember { mutableStateOf(false) }
                SettingsToggleRow(
                    icon = Icons.Outlined.FormatLineSpacing,
                    title = "Compact Line Height",
                    subtitle = "Reduce spacing between lines",
                    checked = compactLines,
                    onCheckedChange = { compactLines = it }
                )

                SettingsDivider()

                // Reading font selector
                var useSerif by remember { mutableStateOf(true) }
                SettingsToggleRow(
                    icon = Icons.Outlined.FontDownload,
                    title = "Serif Font",
                    subtitle = if (useSerif) "Source Serif 4" else "Inter (Sans-serif)",
                    checked = useSerif,
                    onCheckedChange = { useSerif = it }
                )
            }

            Spacer(Modifier.height(spacing.gutter))

            // ── Theme ───────────────────────────────────────────────
            SettingsSection(title = "THEME") {
                // Appearance mode
                var darkMode by remember { mutableStateOf(false) }
                SettingsToggleRow(
                    icon = Icons.Outlined.Brightness6,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark themes",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )

                SettingsDivider()

                // Accent color
                var accentHex by remember { mutableStateOf("03192E") }
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = "Accent Color",
                    subtitle = "Customise the primary theme color"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Live color swatch
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor("#$accentHex"))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )

                        // Hex input with # prefix
                        OutlinedTextField(
                            value = accentHex,
                            onValueChange = { newValue ->
                                // Allow only valid hex characters, max 8 chars (AARRGGBB)
                                val filtered = newValue.filter { it.isLetterOrDigit() }.take(8)
                                accentHex = filtered
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            singleLine = true,
                            prefix = {
                                Text(
                                    "#",
                                    style = UiMainStyle,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            textStyle = UiMainStyle.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.gutter))

            // ── Sync & Storage ──────────────────────────────────────
            SettingsSection(title = "SYNC & STORAGE") {
                var syncEnabled by remember { mutableStateOf(true) }
                SettingsToggleRow(
                    icon = Icons.Outlined.Cloud,
                    title = "Google Drive Sync",
                    subtitle = "Automatically back up annotations",
                    checked = syncEnabled,
                    onCheckedChange = { syncEnabled = it }
                )

                SettingsDivider()

                SettingsRow(
                    icon = Icons.Outlined.Storage,
                    title = "Local Storage",
                    subtitle = "128 MB used"
                )

                SettingsDivider()

                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Clear Cache",
                    subtitle = "Free up temporary storage"
                )
            }

            Spacer(Modifier.height(spacing.gutter))

            // ── About ───────────────────────────────────────────────
            SettingsSection(title = "ABOUT") {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Libro",
                    subtitle = "Version 1.0 · Cloud PDF Reader"
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Settings Section Container ──────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = LabelCapsStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

// ── Settings Row (info / action) ────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = UiMainStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = UiSmStyle.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        trailingContent?.invoke()
    }
}

// ── Settings Toggle Row ─────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = UiMainStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = UiSmStyle.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ── Divider ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 1.dp
    )
}
