package com.example.reedsolomonlab.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reedsolomonlab.data.EducationalStep
import com.example.reedsolomonlab.data.RSConfig
import com.example.reedsolomonlab.data.StepType
import com.example.reedsolomonlab.data.SymbolCard
import com.example.reedsolomonlab.data.SymbolGroup
import com.example.reedsolomonlab.data.SymbolState
import com.example.reedsolomonlab.data.toHex
import com.example.reedsolomonlab.ui.theme.MonoFamily
import com.example.reedsolomonlab.ui.theme.RsColors
import kotlinx.coroutines.delay

// ------------------------------------------------------------------
// Símbolos
// ------------------------------------------------------------------

private fun symbolColors(state: SymbolState): Triple<Color, Color, Color> {
    // (fondo, borde, texto)
    return when (state) {
        SymbolState.DATA -> Triple(RsColors.Cyan.copy(alpha = 0.14f), RsColors.Cyan, RsColors.TextPrimary)
        SymbolState.PARITY -> Triple(RsColors.Purple.copy(alpha = 0.18f), RsColors.Purple, RsColors.TextPrimary)
        SymbolState.ERROR -> Triple(RsColors.Error.copy(alpha = 0.20f), RsColors.Error, RsColors.TextPrimary)
        SymbolState.CORRECTED -> Triple(RsColors.Success.copy(alpha = 0.16f), RsColors.Success, RsColors.TextPrimary)
        SymbolState.CHECKED -> Triple(RsColors.Success.copy(alpha = 0.07f), RsColors.Success.copy(alpha = 0.5f), RsColors.TextPrimary)
        SymbolState.HIDDEN -> Triple(Color.Transparent, RsColors.TextDim.copy(alpha = 0.3f), RsColors.TextDim)
        SymbolState.NORMAL -> Triple(RsColors.SurfaceGlow.copy(alpha = 0.7f), RsColors.CyanDim.copy(alpha = 0.6f), RsColors.TextPrimary)
    }
}

@Composable
fun SymbolChip(card: SymbolCard, modifier: Modifier = Modifier) {
    val (bg, border, text) = symbolColors(card.state)
    // Pulso de error con el reloj ambiental compartido (~8 fps): sin relojes
    // propios a 60 fps que mantengan la CPU despierta.
    val ambientTick = LocalAmbientTick.current
    val errorPulse = if (card.state == SymbolState.ERROR) {
        1f + 0.05f * kotlin.math.abs(kotlin.math.sin(ambientTick * 0.5f)).toFloat()
    } else 1f
    // Sombra solo en los estados destacados: decenas de sombras animadas
    // obligaban a la GPU a recomponer capas en cada fotograma.
    val highlighted = card.state == SymbolState.ERROR || card.state == SymbolState.CORRECTED
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = errorPulse
                scaleY = errorPulse
            }
            .then(
                if (highlighted) Modifier.shadow(
                    8.dp,
                    RoundedCornerShape(12.dp),
                    ambientColor = border, spotColor = border
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.2.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.state == SymbolState.ERROR) {
                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = RsColors.Error, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                }
                if (card.state == SymbolState.CORRECTED) {
                    Icon(Icons.Filled.Check, contentDescription = "Corregido", tint = RsColors.Success, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    text = card.value.toString(),
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = text
                )
            }
            Text(
                text = "0x${card.value.toHex()}",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                color = RsColors.TextSecondary
            )
            if (card.label != null) {
                Text(text = card.label, fontSize = 10.sp, color = RsColors.TextDim)
            }
            if (card.note != null) {
                Text(
                    text = card.note.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = border
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolGroupView(group: SymbolGroup, onSymbolClick: ((Int) -> Unit)? = null) {
    Column {
        Text(
            text = group.label,
            style = MaterialTheme.typography.labelLarge,
            color = RsColors.Cyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.symbols.forEachIndexed { index, card ->
                val chip = @Composable {
                    SymbolChip(card)
                }
                if (onSymbolClick != null) {
                    Box(Modifier.clickable { onSymbolClick(index) }) { chip() }
                } else {
                    chip()
                }
            }
        }
        if (group.explanation != null) {
            Spacer(Modifier.height(6.dp))
            Text(group.explanation, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
        }
    }
}

// ------------------------------------------------------------------
// Pasos educativos
// ------------------------------------------------------------------

private fun stepAccent(type: StepType): Color = when (type) {
    StepType.INFO -> RsColors.CyanDim
    StepType.DATA -> RsColors.Cyan
    StepType.MATH -> RsColors.Purple
    StepType.POLYNOMIAL -> RsColors.Magenta
    StepType.SYMBOLS -> RsColors.Cyan
    StepType.SUCCESS -> RsColors.Success
    StepType.WARNING -> RsColors.Warning
    StepType.ERROR -> RsColors.Error
    StepType.ERROR_LOCATION -> RsColors.Error
    StepType.CORRECTION -> RsColors.Success
}

@Composable
fun StepCard(step: EducationalStep, index: Int, total: Int) {
    var expanded by remember(step.id) { mutableStateOf(step.defaultExpanded) }
    val accent = stepAccent(step.type)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300), label = "chevron"
    )
    GlowCard(accent = accent) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.title, style = MaterialTheme.typography.titleSmall, color = RsColors.TextPrimary)
                    Text(
                        "Paso ${index + 1} de $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = RsColors.TextDim
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore, contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = accent, modifier = Modifier.rotate(chevron)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(step.explanation, style = MaterialTheme.typography.bodyMedium, color = RsColors.TextSecondary)
                    if (step.formula != null) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                step.formula,
                                fontFamily = MonoFamily,
                                fontSize = 13.sp,
                                color = RsColors.Cyan,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    if (step.values.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            step.values.forEach { v ->
                                Row {
                                    Text("▸ ", color = accent, fontFamily = MonoFamily, fontSize = 12.5.sp)
                                    Text(
                                        v, fontFamily = MonoFamily, fontSize = 12.5.sp,
                                        color = RsColors.TextPrimary, lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                    if (step.symbolGroups.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            step.symbolGroups.forEach { SymbolGroupView(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepProgressHeader(current: Int, total: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PROGRESO EDUCATIVO",
                style = MaterialTheme.typography.labelLarge,
                color = RsColors.TextDim,
                fontWeight = FontWeight.Bold
            )
            Text(
                "PASO $current / $total",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                color = RsColors.Cyan
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else current / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = RsColors.Cyan,
            trackColor = RsColors.SurfaceGlow
        )
    }
}

// ------------------------------------------------------------------
// Selector de modo y configuración avanzada
// ------------------------------------------------------------------

@Composable
fun ModeSelector(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(i) },
                label = {
                    Text(
                        label,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                    )
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RsColors.Cyan.copy(alpha = 0.22f),
                    selectedLabelColor = RsColors.Cyan,
                    containerColor = RsColors.SurfaceGlow.copy(alpha = 0.5f),
                    labelColor = RsColors.TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) RsColors.Cyan else RsColors.TextDim.copy(alpha = 0.4f),
                    selectedBorderColor = RsColors.Cyan,
                    borderWidth = if (isSelected) 1.6.dp else 1.dp,
                    selectedBorderWidth = 1.6.dp
                )
            )
        }
    }
}

@Composable
fun AdvancedConfigCard(
    draftData: String,
    draftParity: String,
    onDraftChange: (String, String) -> Unit,
    showAdvanced: Boolean,
    onToggleAdvanced: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleAdvanced(!showAdvanced) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "⚙ CONFIGURACIÓN AVANZADA",
                style = MaterialTheme.typography.labelLarge,
                color = RsColors.Purple,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                Icons.Filled.ExpandMore, contentDescription = null, tint = RsColors.Purple,
                modifier = Modifier.rotate(if (showAdvanced) 180f else 0f)
            )
        }
        AnimatedVisibility(
            visible = showAdvanced,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                val k = draftData.toIntOrNull()
                val nsym = draftParity.toIntOrNull()
                val candidate = if (k != null && nsym != null) RSConfig(k, nsym) else null
                val validation = candidate?.validationError()
                    ?: if (k == null || nsym == null) "Escribe números enteros en ambos campos." else null
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftData,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) onDraftChange(it, draftParity) },
                        label = { Text("Datos (k)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RsColors.Cyan,
                            unfocusedBorderColor = RsColors.TextDim,
                            focusedLabelColor = RsColors.Cyan,
                            cursorColor = RsColors.Cyan
                        )
                    )
                    OutlinedTextField(
                        value = draftParity,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) onDraftChange(draftData, it) },
                        label = { Text("Paridad (2t)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RsColors.Cyan,
                            unfocusedBorderColor = RsColors.TextDim,
                            focusedLabelColor = RsColors.Cyan,
                            cursorColor = RsColors.Cyan
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (validation != null) {
                    Text("⚠ $validation", color = RsColors.Warning, style = MaterialTheme.typography.bodySmall)
                } else if (candidate != null) {
                    Text(
                        "✓ Código RS(${candidate.blockLength}, ${candidate.dataSymbols}): " +
                            "corrige hasta ${candidate.correctionCapacity} errores por bloque.",
                        color = RsColors.Success, style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Si añadimos 2t símbolos de paridad, el código puede corregir en teoría hasta t símbolos erróneos por bloque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RsColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("4+2" to "Compacto", "8+4" to "Recomendado", "12+4" to "Amplio", "16+8" to "Potente").forEach { (cfg, name) ->
                        val parts = cfg.split("+")
                        FilterChip(
                            selected = draftData == parts[0] && draftParity == parts[1],
                            onClick = { onDraftChange(parts[0], parts[1]) },
                            label = { Text("$cfg · $name", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Animación de corrección (volteo)
// ------------------------------------------------------------------

@Composable
fun CorrectionFlip(received: Int, corrected: Int, position: Int) {
    // La animación de volteo se ejecuta una sola vez al aparecer y después
    // queda en estado final estático (sin relojes infinitos que consuman CPU).
    var flipped by remember(received, corrected) { mutableStateOf(false) }
    LaunchedEffect(received, corrected) {
        delay(700)
        flipped = true
    }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(650), label = "flip"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "POSICIÓN $position",
            style = MaterialTheme.typography.labelLarge,
            color = RsColors.TextDim,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            },
            contentAlignment = Alignment.Center
        ) {
            val showFront = rotation <= 90f
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (showFront) RsColors.Error.copy(alpha = 0.2f) else RsColors.Success.copy(alpha = 0.18f),
                border = BorderStroke(2.dp, if (showFront) RsColors.Error else RsColors.Success),
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { rotationY = if (showFront) 0f else 180f }
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (showFront) received.toString() else corrected.toString(),
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        color = RsColors.TextPrimary
                    )
                    Text(
                        if (showFront) "DAÑADO" else "CORREGIDO ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showFront) RsColors.Error else RsColors.Success
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (flipped) "↓ CORRECCIÓN ↓" else "analizando…",
            fontFamily = MonoFamily,
            color = RsColors.Cyan,
            fontSize = 12.sp
        )
    }
}
