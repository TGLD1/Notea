package com.fastek.notea.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Identité FASTEK : violet profond et cyan. */
private val Violet = Color(0xFF6D28D9)
private val VioletProfond = Color(0xFF5B21B6)
private val VioletPale = Color(0xFFEDE9FE)
private val VioletTresSombre = Color(0xFF2E1065)
private val Cyan = Color(0xFF0891B2)
private val CyanPale = Color(0xFFCFFAFE)
private val CyanClair = Color(0xFF22D3EE)

private val SchemaClair = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletPale,
    onPrimaryContainer = VioletTresSombre,
    secondary = Cyan,
    onSecondary = Color.White,
    secondaryContainer = CyanPale,
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Cyan,
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xFFEFEDFA),
    onSurfaceVariant = Color(0xFF4A4660),
    outline = Color(0xFF8B87A3)
)

private val SchemaSombre = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = VioletTresSombre,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = VioletPale,
    secondary = CyanClair,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = CyanPale,
    tertiary = CyanClair,
    background = Color(0xFF0F0B1E),
    onBackground = Color(0xFFECE9FA),
    surface = Color(0xFF0F0B1E),
    onSurface = Color(0xFFECE9FA),
    surfaceVariant = Color(0xFF2A2342),
    onSurfaceVariant = Color(0xFFCBC6E0),
    outline = Color(0xFF8B87A3)
)

@Composable
fun NoteaTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (sombre) SchemaSombre else SchemaClair,
        content = contenu
    )
}
