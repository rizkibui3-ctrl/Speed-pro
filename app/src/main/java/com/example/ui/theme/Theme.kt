package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.model.CockpitAccent

private val SpeedProShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun SpeedProTheme(
    accent: CockpitAccent = CockpitAccent.CYAN_NEON,
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF041018),
        primaryContainer = accent.secondary.copy(alpha = 0.25f),
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = TextPrimary,
        secondaryContainer = CockpitSurfaceElevated,
        onSecondaryContainer = TextPrimary,
        tertiary = SpeedWarningAmber,
        background = CockpitBackground,
        onBackground = TextPrimary,
        surface = CockpitSurface,
        onSurface = TextPrimary,
        surfaceVariant = CockpitSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = CockpitBorder,
        outlineVariant = CockpitBorder.copy(alpha = 0.5f),
        error = SpeedDangerRed,
        onError = Color.White,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeedProTypography,
        shapes = SpeedProShapes,
        content = content,
    )
}
