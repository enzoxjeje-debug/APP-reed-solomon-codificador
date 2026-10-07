package com.example.reedsolomonlab.ui.decoding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reedsolomonlab.audio.SoundManager
import com.example.reedsolomonlab.domain.reedsolomon.ReedSolomon
import com.example.reedsolomonlab.ui.components.AdvancedConfigCard
import com.example.reedsolomonlab.ui.components.AnimatedSection
import com.example.reedsolomonlab.ui.components.BannerType
import com.example.reedsolomonlab.ui.components.CorrectionFlip
import com.example.reedsolomonlab.ui.components.GhostButton
import com.example.reedsolomonlab.ui.components.GlowCard
import com.example.reedsolomonlab.ui.components.ModeSelector
import com.example.reedsolomonlab.ui.components.NeonButton
import com.example.reedsolomonlab.ui.components.SectionHeader
import com.example.reedsolomonlab.ui.components.StatusBanner
import com.example.reedsolomonlab.ui.components.StepCard
import com.example.reedsolomonlab.ui.components.StepProgressHeader
import com.example.reedsolomonlab.ui.components.SymbolChip
import com.example.reedsolomonlab.ui.theme.MonoFamily
import com.example.reedsolomonlab.ui.theme.RsColors
import com.example.reedsolomonlab.viewmodel.AppViewModel
import com.example.reedsolomonlab.viewmodel.DecoderInputMode
import com.example.reedsolomonlab.viewmodel.DecodingViewModel
import com.example.reedsolomonlab.viewmodel.InterpretedAs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecodingScreen(
    appViewModel: AppViewModel,
    decodingViewModel: DecodingViewModel = viewModel()
) {
    val ui by decodingViewModel.ui.collectAsState()

    val handoff by appViewModel.decoderHandoff.collectAsState()
    val play = appViewModel::play

    // Aplica el traspaso del codificador una sola vez.
    LaunchedEffect(handoff) {
        handoff?.let {
            decodingViewModel.loadHandoff(it)
            appViewModel.consumeHandoff()
        }
    }

    var editPosition by remember { mutableStateOf<Int?>(null) }
    var editValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "DECODIFICADOR",
            style = MaterialTheme.typography.headlineSmall,
            color = RsColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Pega solo los números del mensaje codificado. La app deduce sola la configuración.",
            style = MaterialTheme.typography.bodyMedium,
            color = RsColors.TextSecondary
        )

        GlowCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    icon = Icons.Filled.Shield,
                    title = "Mensaje recibido",
                    subtitle = "Pega solo los números, sin datos extra"
                )
                ModeSelector(
                    options = listOf("🔤 SÍMBOLOS", "🔢 BINARIO"),
                    selected = if (ui.inputMode == DecoderInputMode.SIMBOLOS) 0 else 1,
                    onSelect = {
                        decodingViewModel.setInputMode(
                            if (it == 0) DecoderInputMode.SIMBOLOS else DecoderInputMode.BINARIO, play
                        )
                    }
                )
                OutlinedTextField(
                    value = ui.rawInput,
                    onValueChange = decodingViewModel::setRawInput,
                    label = { Text("Mensaje codificado (solo números)") },
                    placeholder = { Text("72 111 108 97 …") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RsColors.Cyan,
                        unfocusedBorderColor = RsColors.TextDim,
                        focusedLabelColor = RsColors.Cyan,
                        cursorColor = RsColors.Cyan
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    NeonButton(
                        text = "LEER MENSAJE",
                        onClick = { decodingViewModel.parseInput(); play(SoundManager.Sound.CLICK) },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Ejemplo con error",
                        onClick = { decodingViewModel.loadCorruptedExample(play) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Science,
                        accent = RsColors.Purple
                    )
                }

                if (ui.interpretedAs != null) {
                    Text(
                        when (ui.interpretedAs) {
                            InterpretedAs.SIMBOLOS -> when (ui.configSource) {
                                com.example.reedsolomonlab.viewmodel.ConfigSource.IMPORTED ->
                                    "✓ Configuración importada del codificador: " +
                                        "RS(${ui.config.blockLength}, ${ui.config.dataSymbols}) · " +
                                        "corrige hasta ${ui.config.correctionCapacity} errores por bloque" +
                                        (if (ui.dataLength != null) " (longitud real: ${ui.dataLength})" else "")
                                com.example.reedsolomonlab.viewmodel.ConfigSource.HEADER ->
                                    "✓ Configuración leída de la cabecera: " +
                                        "RS(${ui.config.blockLength}, ${ui.config.dataSymbols}) · " +
                                        "corrige hasta ${ui.config.correctionCapacity} errores por bloque" +
                                        (if (ui.dataLength != null) " (longitud real: ${ui.dataLength})" else "")
                                else -> "✓ Parámetros detectados automáticamente: " +
                                    "RS(${ui.config.blockLength}, ${ui.config.dataSymbols}) · " +
                                    "corrige hasta ${ui.config.correctionCapacity} errores por bloque" +
                                    (if (ui.dataLength != null) " (longitud real: ${ui.dataLength})" else "")
                            }
                            InterpretedAs.TEXTO -> "✓ Interpretado como: texto (bytes UTF-8). " +
                                "Nota: un texto sin codificar casi nunca es una palabra válida; " +
                                "verás cómo el decodificador detecta el error."
                            else -> ""
                        },
                        color = RsColors.Success,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Símbolos recibidos + simulación de errores.
        val working = ui.workingSymbols
        AnimatedSection(visible = working != null) {
            if (working != null) {
                GlowCard(accent = RsColors.Warning) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            icon = Icons.Filled.BugReport,
                            title = "Simulador de errores",
                            subtitle = "Toca un símbolo para cambiar su valor y simular un error de transmisión",
                            accent = RsColors.Warning
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            working.forEachIndexed { i, s ->
                                val isModified = i in ui.modifiedPositions
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.clickableNoRipple {
                                        editPosition = i
                                        editValue = s.toString()
                                    }
                                ) {
                                    SymbolChip(
                                        com.example.reedsolomonlab.data.SymbolCard(
                                            s,
                                            label = "$i",
                                            state = if (isModified)
                                                com.example.reedsolomonlab.data.SymbolState.ERROR
                                            else com.example.reedsolomonlab.data.SymbolState.NORMAL,
                                            note = if (isModified) "simulado" else null
                                        )
                                    )
                                }
                            }
                        }
                        if (ui.hasSimulation) {
                            Text(
                                ui.modifiedPositions.sorted().joinToString("\n") { pos ->
                                    "⚠ Se modificó el símbolo de la posición $pos: " +
                                        "${ui.parsedSymbols?.get(pos)} → ${working[pos]}."
                                },
                                color = RsColors.Warning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            GhostButton(
                                text = "SIMULAR ERROR",
                                onClick = { decodingViewModel.simulateRandomError(play) },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.BugReport,
                                accent = RsColors.Warning
                            )
                            GhostButton(
                                text = "Deshacer",
                                onClick = { decodingViewModel.resetSimulation(play) },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.Refresh,
                                accent = RsColors.TextSecondary
                            )
                        }
                    }
                }

                GlowCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdvancedConfigCard(
                            draftData = ui.configDraftData,
                            draftParity = ui.configDraftParity,
                            onDraftChange = decodingViewModel::setConfigDraft,
                            showAdvanced = ui.showAdvanced,
                            onToggleAdvanced = {
                                decodingViewModel.setShowAdvanced(it)
                                play(SoundManager.Sound.CLICK)
                            }
                        )
                        NeonButton(
                            text = "DECODIFICAR MENSAJE",
                            onClick = { decodingViewModel.decode(true, play) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.AutoFixHigh
                        )
                    }
                }
            }
        }

        if (ui.error != null) {
            StatusBanner(type = BannerType.ERROR, title = "No se puede continuar", message = ui.error!!)
        }

        val result = ui.result
        AnimatedSection(visible = result != null) {
            if (result != null) {
                when (result.status) {
                    ReedSolomon.DecodeStatus.NO_ERROR -> StatusBanner(
                        type = BannerType.SUCCESS,
                        title = "✓ MENSAJE CORRECTO",
                        message = "Todos los síndromes son cero: no se detectaron errores. " +
                            "Recorre los pasos para ver cómo se recupera el original.",
                        icon = Icons.Filled.CheckCircle
                    )
                    ReedSolomon.DecodeStatus.CORRECTED -> {
                        val totalErrors = result.blocks.sumOf { it.errorPositions.size }
                        StatusBanner(
                            type = BannerType.WARNING,
                            title = "⚠ ERROR DETECTADO Y CORREGIDO",
                            message = "El mensaje recibido no coincide con un código Reed-Solomon válido, " +
                                "pero se ${if (totalErrors == 1) "localizó y corrigió 1 error" else "localizaron y corrigieron $totalErrors errores"}. " +
                                "Mira el proceso matemático paso a paso.",
                            icon = Icons.Filled.Warning
                        )
                    }
                    ReedSolomon.DecodeStatus.TOO_MANY_ERRORS -> StatusBanner(
                        type = BannerType.ERROR,
                        title = "✕ NO ES POSIBLE CORREGIR EL MENSAJE",
                        message = "El código configurado puede corregir hasta ${result.config.correctionCapacity} " +
                            "errores por bloque, pero se detectó un daño mayor. " +
                            "El decodificador no inventa datos: es mejor avisar que devolver un mensaje falso.",
                        icon = Icons.Filled.Warning
                    )
                }

                // Animaciones de corrección por cada error real.
                val corrections = result.blocks.flatMap { it.forney }
                if (corrections.isNotEmpty() && result.status == ReedSolomon.DecodeStatus.CORRECTED) {
                    GlowCard(accent = RsColors.Success) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Corrección símbolo a símbolo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            corrections.forEach { f ->
                                CorrectionFlip(
                                    received = f.received,
                                    corrected = f.corrected,
                                    position = f.position
                                )
                                Text(
                                    "Posición ${f.position}: valor recibido ${f.received} → " +
                                        "magnitud ${f.magnitude} → valor correcto ${f.corrected}.",
                                    fontFamily = MonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RsColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                StepProgressHeader(current = result.steps.size, total = result.steps.size)
                result.steps.forEachIndexed { i, step ->
                    StepCard(step = step, index = i, total = result.steps.size)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Diálogo para editar un símbolo manualmente.
    val pos = editPosition
    if (pos != null && ui.workingSymbols != null) {
        val current = ui.workingSymbols!![pos]
        AlertDialog(
            onDismissRequest = { editPosition = null },
            title = { Text("Modificar símbolo $pos", color = RsColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Valor actual: $current. Escribe el nuevo valor (0–255) para simular un error de transmisión.",
                        color = RsColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) editValue = it },
                        label = { Text("Nuevo valor") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RsColors.Warning,
                            cursorColor = RsColors.Warning
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = editValue.toIntOrNull()
                    if (v == null || v !in 0..255) return@TextButton
                    decodingViewModel.applySimulatedError(pos, v, play)
                    editPosition = null
                }) { Text("APLICAR", color = RsColors.Warning, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editPosition = null }) {
                    Text("Cancelar", color = RsColors.TextSecondary)
                }
            },
            containerColor = RsColors.Surface
        )
    }
}

/** Clic sin ripple para las tarjetas de símbolos. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick
    )
}
