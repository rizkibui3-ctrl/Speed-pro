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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EcuTelemetrySample
import com.example.data.model.JukenMapSlot
import com.example.ui.theme.ChakraPetchFamily
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GpsLockedGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpeedDangerRed
import com.example.ui.theme.SpeedWarningAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RpmTachometerGauge(
    rpm: Int,
    revLimiterRpm: Int,
    activeMap: JukenMapSlot,
    isConnected: Boolean,
    hasLivePacket: Boolean,
    modifier: Modifier = Modifier,
) {
    val maxScaleRpm = maxOf(12000, ((revLimiterRpm + 1999) / 2000) * 2000)
    val clampedRpm = if (isConnected) rpm.coerceIn(0, maxScaleRpm).toFloat() else 0f
    val shiftThreshold = (revLimiterRpm * 0.88f)
    val isRedline = isConnected && rpm >= shiftThreshold

    val animatedRpm by animateFloatAsState(
        targetValue = clampedRpm,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ecu_rpm_needle"
    )

    val accent = MaterialTheme.colorScheme.primary
    val activeColor by animateColorAsState(
        targetValue = if (isRedline) SpeedDangerRed else accent,
        label = "ecu_rpm_color"
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier.testTag("juken_rpm_gauge"),
        contentAlignment = Alignment.Center
    ) {
        val dialSize = minOf(maxWidth, maxHeight).coerceAtLeast(170.dp)
        val rpmFontSp = (dialSize.value * 0.165f).coerceIn(26f, 52f).sp
        val unitFontSp = (dialSize.value * 0.042f).coerceIn(10f, 14f).sp

        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = size.minDimension
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = diameter * 0.47f
                val arcRadius = diameter * 0.415f
                val arcStroke = diameter * 0.044f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CockpitSurfaceElevated,
                            CockpitSurface,
                            CockpitBackground,
                        ),
                        center = center,
                        radius = outerRadius * 1.05f
                    ),
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = CockpitBorder.copy(alpha = 0.75f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = diameter * 0.008f)
                )

                val startAngle = 135f
                val totalSweep = 270f
                val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

                // Track background
                drawArc(
                    color = CockpitBorder.copy(alpha = 0.45f),
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                )

                // Redline zone
                val redStartFrac = (shiftThreshold / maxScaleRpm).coerceIn(0f, 1f)
                drawArc(
                    color = SpeedDangerRed.copy(alpha = 0.35f),
                    startAngle = startAngle + totalSweep * redStartFrac,
                    sweepAngle = totalSweep * (1f - redStartFrac),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStroke * 0.45f, cap = StrokeCap.Butt)
                )

                // Active RPM sweep
                val sweepFrac = (animatedRpm / maxScaleRpm).coerceIn(0f, 1f)
                val activeSweep = totalSweep * sweepFrac
                if (activeSweep > 0.5f) {
                    drawArc(
                        color = activeColor.copy(alpha = 0.24f),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke * 1.6f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                    )
                }

                // Ticks & x1000 labels
                val tickOuter = arcRadius - arcStroke * 0.85f
                val majorTickLen = diameter * 0.036f
                val minorTickLen = diameter * 0.018f
                val labelRadius = tickOuter - majorTickLen - diameter * 0.055f

                for (r in 0..maxScaleRpm step 500) {
                    val frac = r.toFloat() / maxScaleRpm
                    val angleDeg = startAngle + totalSweep * frac
                    val angleRad = angleDeg * PI / 180.0
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()
                    val isMajor = r % 2000 == 0
                    val inRed = r >= shiftThreshold
                    val isPassed = r <= animatedRpm

                    val tickLen = if (isMajor) majorTickLen else minorTickLen
                    val tickColor = when {
                        inRed -> SpeedDangerRed.copy(alpha = if (isPassed) 1f else 0.6f)
                        isPassed -> activeColor
                        isMajor -> TextSecondary
                        else -> TextMuted.copy(alpha = 0.5f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(center.x + tickOuter * cosA, center.y + tickOuter * sinA),
                        end = Offset(
                            center.x + (tickOuter - tickLen) * cosA,
                            center.y + (tickOuter - tickLen) * sinA
                        ),
                        strokeWidth = if (isMajor) diameter * 0.008f else diameter * 0.004f,
                        cap = StrokeCap.Round
                    )

                    if (isMajor) {
                        val labelText = (r / 1000).toString()
                        val layout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = (diameter * 0.042f).toSp(),
                                fontWeight = FontWeight.Bold,
                                color = if (inRed) SpeedDangerRed else if (isPassed) TextPrimary else TextSecondary
                            )
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                center.x + labelRadius * cosA - layout.size.width / 2f,
                                center.y + labelRadius * sinA - layout.size.height / 2f
                            )
                        )
                    }
                }

                // Needle
                val needleAngle = startAngle + totalSweep * sweepFrac
                val nRad = needleAngle * PI / 180.0
                val nCos = cos(nRad).toFloat()
                val nSin = sin(nRad).toFloat()
                val tipR = arcRadius - arcStroke * 0.45f
                val tailR = diameter * 0.055f

                drawLine(
                    color = activeColor.copy(alpha = 0.45f),
                    start = Offset(center.x - tailR * nCos, center.y - tailR * nSin),
                    end = Offset(center.x + tipR * nCos, center.y + tipR * nSin),
                    strokeWidth = diameter * 0.02f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = if (isRedline) SpeedDangerRed else Color.White,
                    start = Offset(center.x - tailR * nCos, center.y - tailR * nSin),
                    end = Offset(center.x + tipR * nCos, center.y + tipR * nSin),
                    strokeWidth = diameter * 0.01f,
                    cap = StrokeCap.Round
                )

                // Inner readout badge
                drawCircle(
                    color = CockpitBackground.copy(alpha = 0.85f),
                    radius = diameter * 0.24f,
                    center = center
                )
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = diameter * 0.24f,
                    center = center,
                    style = Stroke(width = diameter * 0.005f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isConnected && hasLivePacket) rpm.toString() else "0",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = ChakraPetchFamily,
                        fontSize = rpmFontSp,
                        lineHeight = rpmFontSp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isRedline) SpeedDangerRed else TextPrimary,
                    modifier = Modifier.testTag("juken_rpm_value")
                )
                Text(
                    text = "RPM x1000",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = unitFontSp),
                    color = activeColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isRedline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(SpeedDangerRed.copy(alpha = 0.22f), RoundedCornerShape(50))
                            .border(1.dp, SpeedDangerRed, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Mendekati Limit RPM",
                            tint = SpeedDangerRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SHIFT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SpeedDangerRed
                        )
                    }
                } else {
                    Text(
                        text = activeMap.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .background(CockpitSurfaceVariant, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CoolantTempGaugeCard(
    tempC: Int,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    val clampedTemp = if (isLive) tempC.coerceIn(0, 140) else 0
    val animatedTemp by animateFloatAsState(
        targetValue = clampedTemp.toFloat(),
        label = "coolant_temp"
    )

    val (tempColor, tempStatus) = when {
        !isLive || tempC <= 0 -> TextMuted to "Menunggu"
        tempC < 60 -> CyanNeon to "Dingin"
        tempC <= 102 -> GpsLockedGreen to "Optimal"
        tempC <= 112 -> SpeedWarningAmber to "Hangat"
        else -> SpeedDangerRed to "OVERHEAT"
    }

    Column(
        modifier = modifier
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (tempC >= 113 && isLive) SpeedDangerRed else CockpitBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .testTag("juken_coolant_gauge"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = "Suhu Coolant / Mesin",
                    tint = tempColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "COOLANT / MESIN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            Text(
                text = tempStatus,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tempColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val pad = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = CockpitSurfaceVariant,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val frac = (animatedTemp / 130f).coerceIn(0f, 1f)
                    if (frac > 0.01f) {
                        drawArc(
                            color = tempColor,
                            startAngle = 140f,
                            sweepAngle = 260f * frac,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = if (isLive && tempC > 0) "${tempC}°" else "--°",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ChakraPetchFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isLive && tempC > 0) "$tempC °C" else "-- °C",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tempColor,
                    modifier = Modifier.testTag("juken_coolant_value")
                )
                Text(
                    text = "Ideal: 65–102 °C",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ThrottlePositionGaugeCard(
    tpsPercent: Float,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetTps = if (isLive) tpsPercent.coerceIn(0f, 100f) else 0f
    val animatedTps by animateFloatAsState(
        targetValue = targetTps,
        label = "tps_gauge"
    )

    val tpsColor = when {
        !isLive -> TextMuted
        animatedTps >= 85f -> SpeedDangerRed
        animatedTps >= 45f -> SpeedWarningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    val tpsZone = when {
        !isLive -> "Menunggu"
        animatedTps < 2f -> "Idle / Langsam"
        animatedTps <= 40f -> "Cruise / JJS"
        animatedTps < 85f -> "Akselerasi"
        else -> "Full Gas (WOT)"
    }

    Column(
        modifier = modifier
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("juken_tps_gauge"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = "Throttle Position Sensor",
                    tint = tpsColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "THROTTLE (TPS)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            Text(
                text = tpsZone,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tpsColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val pad = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = CockpitSurfaceVariant,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val frac = (animatedTps / 100f).coerceIn(0f, 1f)
                    if (frac > 0.01f) {
                        drawArc(
                            color = tpsColor,
                            startAngle = 140f,
                            sweepAngle = 260f * frac,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = if (isLive) "${tpsPercent.roundToInt()}%" else "--%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ChakraPetchFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isLive) String.format(Locale.US, "%.1f%%", tpsPercent) else "--.-%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tpsColor,
                    modifier = Modifier.testTag("juken_tps_value")
                )
                Text(
                    text = "Bukaan Skep / Gas",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun AfrSpectrumCard(
    afr: Float,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasValidAfr = isLive && afr in 8.0f..22.0f
    val (afrColor, afrLabel) = when {
        !hasValidAfr -> TextMuted to "Menunggu Sensor AFR"
        afr < 12.2f -> SpeedWarningAmber to "Kaya (Rich / Dingin)"
        afr <= 13.6f -> GpsLockedGreen to "Tenaga Optimal"
        afr <= 14.9f -> CyanNeon to "Stoikiometri / Irit"
        else -> SpeedDangerRed to "Terlalu Kering (Lean)"
    }

    val frac = if (hasValidAfr) ((afr - 10f) / 8f).coerceIn(0f, 1f) else 0.5f
    val animatedFrac by animateFloatAsState(targetValue = frac, label = "afr_pos")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("juken_afr_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AIR-FUEL RATIO (AFR)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Text(
                text = afrLabel,
                style = MaterialTheme.typography.labelMedium,
                color = afrColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasValidAfr) String.format(Locale.US, "%.1f", afr) else "--.-",
                style = MaterialTheme.typography.headlineMedium,
                color = afrColor
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barY = size.height / 2f
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                SpeedWarningAmber,
                                GpsLockedGreen,
                                CyanNeon,
                                SpeedDangerRed
                            )
                        ),
                        start = Offset(0f, barY),
                        end = Offset(size.width, barY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    if (hasValidAfr) {
                        val markerX = size.width * animatedFrac
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(markerX, barY)
                        )
                        drawCircle(
                            color = afrColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(markerX, barY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EcuLiveTelemetryChart(
    samples: List<EcuTelemetrySample>,
    revLimiterRpm: Int,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val maxRpm = maxOf(12000, revLimiterRpm).toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("juken_live_chart"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timeline,
                    contentDescription = "Grafik RPM dan TPS",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GRAFIK LIVE RPM & TPS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "● RPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Text(
                    text = "● TPS %",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpeedWarningAmber
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (samples.size < 2) {
                Text(
                    text = "Grafik akan bergerak otomatis saat data ECU Juken 5+ diterima",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Horizontal grid lines
                    for (i in 1..3) {
                        val y = size.height * (i / 4f)
                        drawLine(
                            color = CockpitBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                    val rpmPath = Path()
                    val tpsPath = Path()

                    samples.forEachIndexed { idx, s ->
                        val x = idx * stepX
                        val rpmY = size.height * (1f - (s.rpm / maxRpm).coerceIn(0f, 1f))
                        val tpsY = size.height * (1f - (s.tpsPercent / 100f).coerceIn(0f, 1f))
                        if (idx == 0) {
                            rpmPath.moveTo(x, rpmY)
                            tpsPath.moveTo(x, tpsY)
                        } else {
                            rpmPath.lineTo(x, rpmY)
                            tpsPath.lineTo(x, tpsY)
                        }
                    }

                    drawPath(
                        path = tpsPath,
                        color = SpeedWarningAmber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = rpmPath,
                        color = accent,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
