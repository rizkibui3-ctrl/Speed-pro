package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpeedProfile
import com.example.data.model.SpeedUnit
import com.example.ui.theme.ChakraPetchFamily
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpeedDangerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    speedValue: Float,
    speedUnit: SpeedUnit,
    speedProfile: SpeedProfile,
    warningLimitInUnit: Int,
    warningEnabled: Boolean,
    isOverspeed: Boolean,
    modifier: Modifier = Modifier,
) {
    val maxScale = speedProfile.maxGaugeValue.toFloat()
    val clampedSpeed = speedValue.coerceIn(0f, maxScale)

    val animatedSpeed by animateFloatAsState(
        targetValue = clampedSpeed,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow
        ),
        label = "gauge_speed"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val activeGlowColor by animateColorAsState(
        targetValue = if (isOverspeed) SpeedDangerRed else accentColor,
        label = "gauge_glow"
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier.testTag("speedometer_gauge"),
        contentAlignment = Alignment.Center
    ) {
        val dialSize = minOf(maxWidth, maxHeight).coerceAtLeast(160.dp)
        val digitalFontSp = (dialSize.value * 0.21f).coerceIn(36f, 72f).sp
        val unitFontSp = (dialSize.value * 0.042f).coerceIn(10f, 15f).sp

        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = diameter * 0.47f
            val arcRadius = diameter * 0.415f
            val arcStroke = diameter * 0.042f

            // Outer carbon bevel ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CockpitSurfaceElevated,
                        CockpitSurface,
                        Color(0xFF04070D),
                    ),
                    center = center,
                    radius = outerRadius * 1.06f
                ),
                radius = outerRadius,
                center = center,
            )

            drawCircle(
                color = CockpitBorder.copy(alpha = 0.7f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = diameter * 0.008f)
            )

            val startAngle = 135f
            val totalSweep = 270f

            // Background track arc
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            drawArc(
                color = CockpitBorder.copy(alpha = 0.45f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )

            // Overspeed red-zone arc indicator
            if (warningEnabled && warningLimitInUnit in 1 until speedProfile.maxGaugeValue) {
                val warnRatio = (warningLimitInUnit.toFloat() / maxScale).coerceIn(0f, 1f)
                val warnStart = startAngle + totalSweep * warnRatio
                val warnSweep = totalSweep * (1f - warnRatio)
                drawArc(
                    color = SpeedDangerRed.copy(alpha = 0.35f),
                    startAngle = warnStart,
                    sweepAngle = warnSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStroke * 0.45f, cap = StrokeCap.Butt)
                )
            }

            // Active progress arc glow
            val sweepFraction = (animatedSpeed / maxScale).coerceIn(0f, 1f)
            val activeSweep = totalSweep * sweepFraction
            if (activeSweep > 0.5f) {
                drawArc(
                    color = activeGlowColor.copy(alpha = 0.22f),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStroke * 1.65f, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to accentColor.copy(alpha = 0.65f),
                        0.6f to activeGlowColor,
                        1.0f to activeGlowColor,
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                )
            }

            // Tick marks and major scale numbers
            val majorStep = speedProfile.majorStep
            val minorStep = majorStep / 4
            val tickOuter = arcRadius - arcStroke * 0.85f
            val majorTickLen = diameter * 0.036f
            val minorTickLen = diameter * 0.018f
            val labelRadius = tickOuter - majorTickLen - diameter * 0.055f

            for (v in 0..speedProfile.maxGaugeValue step minorStep) {
                val frac = v / maxScale
                val angleDeg = startAngle + totalSweep * frac
                val angleRad = (angleDeg * PI / 180.0)
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()
                val isMajor = v % majorStep == 0
                val isPassed = v <= animatedSpeed
                val inRedZone = warningEnabled && v >= warningLimitInUnit

                val tickLen = if (isMajor) majorTickLen else minorTickLen
                val tickColor = when {
                    inRedZone -> SpeedDangerRed.copy(alpha = if (isPassed) 1f else 0.6f)
                    isPassed -> activeGlowColor
                    isMajor -> TextSecondary
                    else -> TextMuted.copy(alpha = 0.5f)
                }

                val p1 = Offset(center.x + tickOuter * cosA, center.y + tickOuter * sinA)
                val p2 = Offset(
                    center.x + (tickOuter - tickLen) * cosA,
                    center.y + (tickOuter - tickLen) * sinA
                )
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) diameter * 0.008f else diameter * 0.004f,
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val labelLayout = textMeasurer.measure(
                        text = v.toString(),
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = (diameter * 0.040f).toSp(),
                            fontWeight = FontWeight.Bold,
                            color = if (inRedZone) SpeedDangerRed else if (isPassed) TextPrimary else TextSecondary
                        )
                    )
                    val lx = center.x + labelRadius * cosA - labelLayout.size.width / 2f
                    val ly = center.y + labelRadius * sinA - labelLayout.size.height / 2f
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(lx, ly)
                    )
                }
            }

            // Analog Needle
            val needleAngleDeg = startAngle + totalSweep * sweepFraction
            val needleRad = (needleAngleDeg * PI / 180.0)
            val nCos = cos(needleRad).toFloat()
            val nSin = sin(needleRad).toFloat()
            val needleTipRadius = arcRadius - arcStroke * 0.45f
            val needleTailRadius = diameter * 0.055f

            val needleTip = Offset(
                center.x + needleTipRadius * nCos,
                center.y + needleTipRadius * nSin
            )
            val needleTail = Offset(
                center.x - needleTailRadius * nCos,
                center.y - needleTailRadius * nSin
            )

            // Needle glow shadow
            drawLine(
                color = activeGlowColor.copy(alpha = 0.45f),
                start = needleTail,
                end = needleTip,
                strokeWidth = diameter * 0.022f,
                cap = StrokeCap.Round
            )
            // Needle core
            drawLine(
                color = if (isOverspeed) SpeedDangerRed else Color.White,
                start = needleTail,
                end = needleTip,
                strokeWidth = diameter * 0.011f,
                cap = StrokeCap.Round
            )

            // Center dark glass badge behind digital readout
            drawCircle(
                color = Color(0xCC070B12),
                radius = diameter * 0.22f,
                center = center
            )
            drawCircle(
                color = activeGlowColor.copy(alpha = 0.35f),
                radius = diameter * 0.22f,
                center = center,
                style = Stroke(width = diameter * 0.005f)
            )
        }

        // Digital Center Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = speedValue.roundToInt().toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = ChakraPetchFamily,
                    fontSize = digitalFontSp,
                    lineHeight = digitalFontSp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverspeed) SpeedDangerRed else TextPrimary
                ),
                modifier = Modifier.testTag("digital_speed_text")
            )
            Text(
                text = speedUnit.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = unitFontSp,
                    color = activeGlowColor,
                    letterSpacing = 2.sp
                )
            )
            if (isOverspeed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            SpeedDangerRed.copy(alpha = 0.22f),
                            RoundedCornerShape(50)
                        )
                        .border(1.dp, SpeedDangerRed, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Batas kecepatan terlampaui",
                        tint = SpeedDangerRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "LIMIT $warningLimitInUnit",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpeedDangerRed
                    )
                }
            } else if (warningEnabled) {
                Text(
                    text = "LIMIT $warningLimitInUnit",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        }
    }
}
