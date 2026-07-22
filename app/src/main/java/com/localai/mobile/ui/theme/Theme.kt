package com.localai.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Multiple themes, matching the spirit of the original LocalAI:
 * Mono (deepest black / brightest white), Dark, Light, Midnight,
 * Terminal (green), Sakura, Rose.
 */
data class AppTheme(val id: String, val label: String, val dark: Boolean,
                    val bg: Color, val surface: Color, val primary: Color,
                    val onPrimary: Color, val text: Color, val accent: Color)

object Themes {
    val MONO = AppTheme("mono", "Monochrome", true,
        Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFFFFFFFF),
        Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFBFBFBF))
    val DARK = AppTheme("dark", "Dark", true,
        Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF7C9CFF),
        Color(0xFF000000), Color(0xFFEAEAEA), Color(0xFF7C9CFF))
    val LIGHT = AppTheme("light", "Light", false,
        Color(0xFFFAFAFA), Color(0xFFFFFFFF), Color(0xFF1A1A1A),
        Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFF3B5BFF))
    val MIDNIGHT = AppTheme("midnight", "Midnight", true,
        Color(0xFF0A0E27), Color(0xFF141A3C), Color(0xFF5CC8FF),
        Color(0xFF001018), Color(0xFFE6ECFF), Color(0xFF5CC8FF))
    val TERMINAL = AppTheme("terminal", "Terminal", true,
        Color(0xFF000A00), Color(0xFF031403), Color(0xFF33FF66),
        Color(0xFF001400), Color(0xFF9DFFB0), Color(0xFF33FF66))
    val SAKURA = AppTheme("sakura", "Sakura", false,
        Color(0xFFFFF3F7), Color(0xFFFFFFFF), Color(0xFFE0538A),
        Color(0xFFFFFFFF), Color(0xFF3B2530), Color(0xFFFF8FB6))
    val ROSE = AppTheme("rose", "Rose", true,
        Color(0xFF1A0E13), Color(0xFF2A141C), Color(0xFFFF6F91),
        Color(0xFF1A0006), Color(0xFFFFE1EA), Color(0xFFFF6F91))

    val all = listOf(MONO, DARK, LIGHT, MIDNIGHT, TERMINAL, SAKURA, ROSE)
    fun byId(id: String): AppTheme = all.firstOrNull { it.id == id } ?: MONO
}

@Composable
fun LocalAITheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = if (theme.dark)
        darkColorScheme(
            background = theme.bg, surface = theme.surface, primary = theme.primary,
            onPrimary = theme.onPrimary, onBackground = theme.text, onSurface = theme.text,
            secondary = theme.accent, surfaceVariant = theme.surface
        )
    else
        lightColorScheme(
            background = theme.bg, surface = theme.surface, primary = theme.primary,
            onPrimary = theme.onPrimary, onBackground = theme.text, onSurface = theme.text,
            secondary = theme.accent, surfaceVariant = theme.surface
        )
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
