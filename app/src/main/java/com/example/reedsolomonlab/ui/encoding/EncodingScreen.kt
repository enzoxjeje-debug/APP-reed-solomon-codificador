package com.example.reedsolomonlab.ui.encoding

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reedsolomonlab.data.MessageType
import com.example.reedsolomonlab.ui.components.AdvancedConfigCard
import com.example.reedsolomonlab.ui.components.AnimatedSection
import com.example.reedsolomonlab.ui.components.BannerType
import com.example.reedsolomonlab.ui.components.GhostButton
import com.example.reedsolomonlab.ui.components.GlowCard
import com.example.reedsolomonlab.ui.components.ModeSelector
import com.example.reedsolomonlab.ui.components.NeonButton
import com.example.reedsolomonlab.ui.components.SectionHeader
import com.example.reedsolomonlab.ui.components.StatusBanner
import com.example.reedsolomonlab.ui.components.StepCard
import com.example.reedsolomonlab.ui.components.StepProgressHeader
import com.example.reedsolomonlab.ui.theme.MonoFamily
import com.example.reedsolomonlab.ui.theme.RsColors
import com.example.reedsolomonlab.viewmodel.AppViewModel
import com.example.reedsolomonlab.viewmodel.EncodingViewModel

@Composable
fun EncodingScreen(
    appViewModel: AppViewModel,
    onNavigateToDecoder: () -> Unit,
    encodingViewModel: EncodingViewModel = viewModel()
) {
    val ui by encodingViewModel.ui.collectAsState()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val play = appViewModel::play

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera.
        Text(
            "RS-COM2026 · Laboratorio",
            style = MaterialTheme.typography.headlineSmall,
            color = RsColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Explora cómo los códigos de corrección de errores protegen tus datos.",
            style = MaterialTheme.typography.bodyMedium,
            color = RsColors.TextSecondary
        )

        // Tarjeta de entrada.
        GlowCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    icon = Icons.Filled.Memory,
                    title = "Mensaje de entrada",
                    subtitle = "Elige el tipo de mensaje que quieres proteger"
                )
                Text(
                    "TIPO DE MENSAJE",
                    style = MaterialTheme.typography.labelLarge,
                    color = RsColors.TextDim,
                    fontWeight = FontWeight.Bold
                )
                ModeSelector(
                    options = listOf("📝 TEXTO", "🔢 BINARIO"),
                    selected = if (ui.messageType == MessageType.TEXTO) 0 else 1,
                    onSelect = {
                        encodingViewModel.setMessageType(
                            if (it == 0) MessageType.TEXTO else MessageType.BINARIO, play
                        )
                    }
                )
                if (ui.messageType == MessageType.TEXTO) {
                    OutlinedTextField(
                        value = ui.textInput,
                        onValueChange = encodingViewModel::setText,
                        label = { Text("Escribe tu mensaje") },
                        placeholder = { Text("Hola mundo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RsColors.Cyan,
                            unfocusedBorderColor = RsColors.TextDim,
                            focusedLabelColor = RsColors.Cyan,
                            cursorColor = RsColors.Cyan
                        )
                    )
                    Text(
                        "Mensaje original:\n${ui.textInput.ifEmpty { "(vacío)" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RsColors.TextPrimary
                    )
                } else {
                    OutlinedTextField(
                        value = ui.binaryInput,
                        onValueChange = encodingViewModel::setBinary,
                        label = { Text("Datos binarios (bytes de 8 bits)") },
                        placeholder = { Text("01001000 01101111 …") },
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
                    Text(
                        "La entrada binaria se interpreta byte a byte: cada grupo de 8 bits (0 y 1) es un símbolo. " +
                            "Separa los bytes con espacios, por ejemplo «01001000 01101111».",
                        style = MaterialTheme.typography.bodySmall,
                        color = RsColors.TextSecondary
                    )
                }

                AdvancedConfigCard(
                    draftData = ui.configDraftData,
                    draftParity = ui.configDraftParity,
                    onDraftChange = encodingViewModel::setConfigDraft,
                    showAdvanced = ui.showAdvanced,
                    onToggleAdvanced = { encodingViewModel.setShowAdvanced(it); play(com.example.reedsolomonlab.audio.SoundManager.Sound.CLICK) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    NeonButton(
                        text = "CODIFICAR MENSAJE",
                        onClick = { encodingViewModel.encode(true, play) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.PlayArrow
                    )
                }
                GhostButton(
                    text = "Cargar ejemplo",
                    onClick = { encodingViewModel.loadExample(play) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Science,
                    accent = RsColors.Purple
                )
            }
        }

        if (ui.error != null) {
            StatusBanner(
                type = BannerType.ERROR,
                title = "Revisa la entrada",
                message = ui.error!!
            )
        }

        // Resultado paso a paso.
        val result = ui.result
        AnimatedSection(visible = result != null) {
            if (result != null) {
                StatusBanner(
                    type = BannerType.SUCCESS,
                    title = "✓ Mensaje codificado",
                    message = "El mensaje se protegió con ${result.blocks.size} " +
                        if (result.blocks.size == 1) "bloque." else "bloques. " +
                            "Recorre los pasos para entender cada cálculo.",
                    icon = Icons.Filled.Memory
                )
                StepProgressHeader(current = result.steps.size, total = result.steps.size)
                result.steps.forEachIndexed { i, step ->
                    StepCard(step = step, index = i, total = result.steps.size)
                }
                GlowCard(accent = RsColors.Success) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Mensaje codificado listo",
                            style = MaterialTheme.typography.titleMedium,
                            color = RsColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            result.shareable,
                            fontFamily = MonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = RsColors.Cyan
                        )
                        Text(
                            "Código RS(${result.config.blockLength}, ${result.config.dataSymbols}) · " +
                                "corrige hasta ${result.config.correctionCapacity} errores por bloque. " +
                                "Copia solo los números: el decodificador deduce solo el resto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RsColors.TextSecondary
                        )
                        // Botones apilados a ancho completo: los textos largos
                        // ya no se recortan en pantallas angostas.
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GhostButton(
                                text = "COPIAR",
                                onClick = {
                                    clipboard.setText(AnnotatedString(result.shareable))
                                    play(com.example.reedsolomonlab.audio.SoundManager.Sound.CLICK)
                                    Toast.makeText(context, "Mensaje codificado copiado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Filled.ContentCopy
                            )
                            NeonButton(
                                text = "PROBAR EN DECODIFICADOR",
                                onClick = {
                                    appViewModel.sendToDecoder(
                                        result.shareable,
                                        result.config,
                                        result.inputSymbols.size
                                    )
                                    play(com.example.reedsolomonlab.audio.SoundManager.Sound.NAVIGATE)
                                    onNavigateToDecoder()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
