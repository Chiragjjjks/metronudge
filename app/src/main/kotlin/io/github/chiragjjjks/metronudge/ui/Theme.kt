package io.github.chiragjjjks.metronudge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** shadcn/ui "zinc" palette, plus Namma Metro purple as the brand accent. */
@Immutable
data class Palette(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val border: Color,
    val muted: Color,
    val mutedForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
)

private val Light = Palette(
    background = Color(0xFFFAFAFA),
    foreground = Color(0xFF09090B),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFE4E4E7),
    muted = Color(0xFFF4F4F5),
    mutedForeground = Color(0xFF71717A),
    primary = Color(0xFF18181B),
    primaryForeground = Color(0xFFFAFAFA),
    accent = Color(0xFF8E3A96),
    accentSoft = Color(0xFFF5E8F6),
    success = Color(0xFF16A34A),
    successSoft = Color(0xFFDCFCE7),
    warning = Color(0xFFD97706),
)

private val Dark = Palette(
    background = Color(0xFF09090B),
    foreground = Color(0xFFFAFAFA),
    card = Color(0xFF111113),
    border = Color(0xFF27272A),
    muted = Color(0xFF18181B),
    mutedForeground = Color(0xFFA1A1AA),
    primary = Color(0xFFFAFAFA),
    primaryForeground = Color(0xFF18181B),
    accent = Color(0xFFD08BD6),
    accentSoft = Color(0xFF2A1A2C),
    success = Color(0xFF4ADE80),
    successSoft = Color(0xFF052E16),
    warning = Color(0xFFFBBF24),
)

val LocalPalette = staticCompositionLocalOf { Light }

object Mn {
    val colors: Palette @Composable get() = LocalPalette.current
}

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

@Composable
fun MetroNudgeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Dark else Light
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.primary,
        onPrimary = p.primaryForeground,
        background = p.background,
        onBackground = p.foreground,
        surface = p.card,
        onSurface = p.foreground,
        surfaceVariant = p.muted,
        onSurfaceVariant = p.mutedForeground,
        outline = p.border,
        outlineVariant = p.border,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
