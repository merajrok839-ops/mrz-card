package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  secondary = EmeraldSecondaryDark,
  tertiary = MatteGoldDark,
  background = Color(0xFF0F172A),
  surface = Color(0xFF1E293B),
  onPrimary = Color(0xFF064E3B),
  onSecondary = Color(0xFF064E3B),
  onTertiary = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  secondary = EmeraldSecondary,
  tertiary = MatteGold,
  background = SlateBackground,
  surface = Color.White,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color(0xFF0F172A),
  onBackground = DarkSlateText,
  onSurface = DarkSlateText
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
