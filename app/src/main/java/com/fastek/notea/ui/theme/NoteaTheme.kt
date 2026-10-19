package com.fastek.notea.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Identité FASTEK : violet nuit (#1B1040 → #4C1D95), blanc cassé et cyan. */
private val VioletNuit = Color(0xFF1B1040)
private val Violet = Color(0xFF4C1D95)
private val VioletPale = Color(0xFFEDE9FE)
private val Lavande = Color(0xFFB9AEDD)
private val BlancCasse = Color(0xFFF3F6FC)
private val Cyan = Color(0xFF0E7490)
private val CyanPale = Color(0xFFCFFAFE)
private val CyanClair = Color(0xFF22D3EE)

private val SchemaClair = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletPale,
    onPrimaryContainer = VioletNuit,
    secondary = Cyan,
    onSecondary = Color.White,
    secondaryContainer = CyanPale,
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Cyan,
    background = BlancCasse,
    onBackground = VioletNuit,
    surface = BlancCasse,
    onSurface = VioletNuit,
    surfaceVariant = Color(0xFFE9E6F5),
    onSurfaceVariant = Color(0xFF4A4660),
    outline = Color(0xFF8B87A3)
)

private val SchemaSombre = darkColorScheme(
    primary = Lavande,
    onPrimary = VioletNuit,
    primaryContainer = Violet,
    onPrimaryContainer = VioletPale,
    secondary = CyanClair,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = CyanPale,
    tertiary = CyanClair,
    background = VioletNuit,
    onBackground = BlancCasse,
    surface = VioletNuit,
    onSurface = BlancCasse,
    surfaceVariant = Color(0xFF2B1F5C),
    onSurfaceVariant = Lavande,
    outline = Color(0xFF8B87A3)
)

@Composable
fun NoteaTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (sombre) SchemaSombre else SchemaClair,
        content = contenu
    )
}
