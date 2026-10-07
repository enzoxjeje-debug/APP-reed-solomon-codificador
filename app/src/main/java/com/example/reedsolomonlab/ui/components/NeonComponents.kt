package com.example.reedsolomonlab.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reedsolomonlab.ui.theme.BackgroundGradient
import kotlin.math.sin
import com.example.reedsolomonlab.ui.theme.NeonGradient
import com.example.reedsolomonlab.ui.theme.RsColors

// ------------------------------------------------------------------
// Fondo
// ------------------------------------------------------------------

private fun pseudo(seed: Int): Float {
    val h = (seed * 1103515245 + 12345) and 0x7fffffff
    return h / Int.MAX_VALUE.toFloat()
}

/** Reloj ambiental compartido (~8 fps) para las animaciones de fondo. */
val LocalAmbientTick = compositionLocalOf { 0L }

/** Fondo oscuro futurista: degradado + rejilla tecnológica + partículas flotantes. */
@Composable
fun NeonBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // El contenido NO depende del reloj: queda totalmente estático en reposo.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        AmbientBackground(Modifier.fillMaxSize())
        content()
    }
}

/**
 * Fondo animado con ámbito propio: el tick solo invalida este subárbol
 * (partículas + lectores del reloj) y jamás el contenido de la pantalla.
 */
@Composable
private fun AmbientBackground(modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalAmbientTick provides 0L) {
        ParticleLayer(modifier)
    }
}

@Composable
private fun ParticleLayer(modifier: Modifier = Modifier) {
    // Fondo completamente estático: las partículas son un campo de estrellas
    // fijo. El movimiento ambiental continuo obligaba a redibujar toda la
    // pantalla varias veces por segundo con un coste de CPU inaceptable en
    // gamas medias; la vida la aportan las transiciones, expansiones y
    // animaciones de corrección.
    val tick = 0L
    val glowPulse = 0.55f + 0.45f * sin(tick * 0.18f).toFloat()
    Canvas(modifier = modifier) {
        // Rejilla sutil.
        val step = 72.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = RsColors.Cyan,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
                alpha = 0.05f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = RsColors.Cyan,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                alpha = 0.05f
            )
            y += step
        }
        // Partículas lentas y pequeñas.
        val palette = listOf(RsColors.Cyan, RsColors.Purple, RsColors.Magenta)
        for (i in 0 until 10) {
            val speed = 0.00035f * (1f + pseudo(i * 5 + 1) * 2f)
            val px = ((pseudo(i * 3 + 1) + tick * speed) % 1f) * size.width
            val py = ((pseudo(i * 3 + 2) + tick * speed * 0.6f) % 1f) * size.height
            drawCircle(
                color = palette[i % palette.size],
                radius = 2f + pseudo(i * 7 + 3) * 3.5f,
                center = Offset(px, py),
                alpha = (0.08f + 0.10f * glowPulse).coerceAtMost(0.22f)
            )
        }
    }
}

// ------------------------------------------------------------------
// Tarjetas y botones
// ------------------------------------------------------------------

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    accent: Color = RsColors.Cyan,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = accent.copy(alpha = 0.35f),
            spotColor = accent.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(20.dp),
        color = RsColors.Surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Color = RsColors.Cyan
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = RsColors.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
            }
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        // Altura mínima en vez de fija: los textos largos (2 líneas) crecen
        // en vez de recortarse o descentrarse.
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = RsColors.Background,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = RsColors.TextDim
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) NeonGradient
                    else androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF2A3550), Color(0xFF2A3550))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = RsColors.Cyan
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

// ------------------------------------------------------------------
// Banners de estado
// ------------------------------------------------------------------

enum class BannerType { SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusBanner(
    type: BannerType,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val accent = when (type) {
        BannerType.SUCCESS -> RsColors.Success
        BannerType.WARNING -> RsColors.Warning
        BannerType.ERROR -> RsColors.Error
        BannerType.INFO -> RsColors.Cyan
    }
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(initialScale = 0.9f) + fadeIn(),
        modifier = modifier
    ) {
        GlowCard(accent = accent) {
            Row(verticalAlignment = Alignment.Top) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = RsColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
fun AnimatedSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(350)) + expandVertically(animationSpec = tween(350)),
        exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}
