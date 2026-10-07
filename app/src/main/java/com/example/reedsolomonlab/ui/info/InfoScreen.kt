package com.example.reedsolomonlab.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reedsolomonlab.ui.components.AnimatedSection
import com.example.reedsolomonlab.ui.components.GlowCard
import com.example.reedsolomonlab.ui.components.SectionHeader
import com.example.reedsolomonlab.ui.theme.RsColors
import com.example.reedsolomonlab.viewmodel.AppViewModel

@Composable
fun InfoScreen(appViewModel: AppViewModel) {
    val soundEnabled by appViewModel.soundEnabled.collectAsState()
    var showGlossary by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "¿QUÉ ES REED-SOLOMON?",
            style = MaterialTheme.typography.headlineSmall,
            color = RsColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )

        GlowCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    icon = Icons.Filled.School,
                    title = "La idea en 30 segundos",
                    accent = RsColors.Cyan
                )
                InfoParagraph(
                    "Reed-Solomon es un código de corrección de errores inventado en 1960 " +
                        "por Irving Reed y Gustave Solomon. Su truco es añadir redundancia: " +
                        "unos símbolos extra calculados con matemáticas que permiten DETECTAR " +
                        "y CORREGIR errores sin pedir que se reenvíe el mensaje."
                )
                InfoParagraph(
                    "Imagina que envías una carta y temes que el cartero manche algunas palabras. " +
                        "Reed-Solomon es como añadir al final una «chuleta matemática» sobre el contenido: " +
                        "si algo llega manchado, el receptor usa la chuleta para reconstruir lo perdido."
                )
                InfoParagraph(
                    "Regla de oro: si añadimos 2t símbolos de paridad, el código puede corregir " +
                        "hasta t símbolos erróneos. Por eso el ajuste 8+4 corrige 2 errores."
                )
            }
        }

        GlowCard(accent = RsColors.Purple) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    icon = Icons.Filled.Info,
                    title = "Conceptos clave",
                    accent = RsColors.Purple
                )
                ConceptRow("🧱 Símbolo", "La unidad que se protege: un byte (0–255). Un símbolo dañado cuenta como un solo error, aunque fallen varios bits dentro de él.")
                ConceptRow("➕ Redundancia", "Información extra añadida a propósito. No es desperdicio: es el «seguro» que permite recuperar los datos.")
                ConceptRow("🛡 Paridad", "Los símbolos de redundancia que calcula el codificador con la división de polinomios.")
                ConceptRow("⚠ Error", "Un símbolo que cambió de valor sin avisar. Hay que descubrir dónde está y cuánto cambió.")
                ConceptRow("👻 Borradura (erasure)", "Un error del que SÍ sabemos la posición (por ejemplo, un paquete perdido marcado como tal). Como la posición ya se conoce, corregir borraduras cuesta la mitad: con 2t paridades se corrigen hasta 2t borraduras.")
                ConceptRow("🔢 Campo de Galois", "Un conjunto finito de números con suma, resta, multiplicación y división bien definidas. Reed-Solomon vive dentro de uno de estos campos.")
                ConceptRow("🌌 GF(256)", "El campo con 256 elementos (los bytes). Sumar es hacer XOR; multiplicar usa logaritmos en base α = 2 con el polinomio x⁸+x⁴+x³+x²+1.")
                ConceptRow("🩺 Síndrome", "El «chequeo de salud» del mensaje: evaluar el polinomio recibido en α⁰, α¹, … Si todos dan cero, no hay errores; si no, su valor es la huella del error.")
                ConceptRow("📍 Polinomio localizador", "La ecuación que construye Berlekamp-Massey a partir de los síndromes. Sus raíces dicen DÓNDE están los errores.")
                ConceptRow("📐 Fórmula de Forney", "Con el polinomio evaluador Ω(x) calcula CUÁNTO cambió cada símbolo: Y = X·Ω(X⁻¹)/Λ′(X⁻¹).")
            }
        }

        GlowCard(accent = RsColors.Magenta) {
            Column {
                SectionHeader(
                    icon = Icons.Filled.MenuBook,
                    title = "Diagrama interactivo",
                    subtitle = "El viaje de tus datos",
                    accent = RsColors.Magenta
                )
                Spacer(Modifier.height(10.dp))
                FlowDiagram()
            }
        }

        AnimatedSection(visible = true) {
            GlowCard(accent = RsColors.Success) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        icon = Icons.Filled.Info,
                        title = "¿DÓNDE SE UTILIZA?",
                        subtitle = "Reed-Solomon está en todas partes",
                        accent = RsColors.Success
                    )
                    AppRow("📷", "Códigos QR", "Tu móvil lee QR rotos, sucios o parcialmente tapados gracias a Reed-Solomon: hasta un 30 % del código puede dañarse y seguir leyéndose.")
                    AppRow("💿", "CD, DVD y Blu-ray", "Un arañazo destruye miles de bits seguidos. Como los errores caen dentro de pocos símbolos, el lector los corrige y la música sigue sonando.")
                    AppRow("🛰", "Satélites y espacio", "A millones de kilómetros no se puede pedir «¿me lo repites?». Las sondas (Voyager, Marte) protegen sus datos con Reed-Solomon.")
                    AppRow("📺", "TV digital (DVB)", "La señal de antena sufre interferencias; el receptor corrige los errores en tiempo real para que no se congele la imagen.")
                    AppRow("💾", "Discos y almacenamiento", "RAID, discos duros y memorias flash usan códigos Reed-Solomon para sobrevivir a sectores defectuosos.")
                    AppRow("☁", "Nube distribuida", "Servicios de almacenamiento reparten fragmentos con paridad entre servidores: aunque varios fallen, tus archivos se reconstruyen.")
                    AppRow("📡", "Móviles y wifi", "Las llamadas y los datos viajan con protección contra el ruido de la radio.")
                    AppRow("🏷", "Códigos de barras", "Muchos sistemas de etiquetado industrial resisten manchas y roturas parciales.")
                }
            }
        }

        GlowCard(accent = RsColors.Warning) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGlossary = !showGlossary; appViewModel.play(com.example.reedsolomonlab.audio.SoundManager.Sound.CLICK) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "📖 GLOSARIO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = RsColors.Warning
                    )
                    Icon(
                        Icons.Filled.ExpandMore, contentDescription = null, tint = RsColors.Warning,
                        modifier = Modifier.rotate(if (showGlossary) 180f else 0f)
                    )
                }
                AnimatedSection(visible = showGlossary) {
                    Glossary.entries.forEach { (term, definition) ->
                        var expanded by remember(term) { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712), RoundedCornerShape(12.dp))
                                .clickable { expanded = !expanded }
                                .padding(12.dp)
                        ) {
                            Text(term, fontWeight = FontWeight.Bold, color = RsColors.Cyan)
                            if (expanded) {
                                Spacer(Modifier.height(4.dp))
                                Text(definition, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
                            } else {
                                Text("Toca para ver la definición…", style = MaterialTheme.typography.labelSmall, color = RsColors.TextDim)
                            }
                        }
                    }
                }
            }
        }

        GlowCard(accent = RsColors.Purple) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    icon = Icons.Filled.Settings,
                    title = "Ajustes",
                    subtitle = "Sonido",
                    accent = RsColors.Purple
                )
                SettingsRow(
                    title = "🔊 Sonido",
                    subtitle = "Efectos sutiles al codificar, detectar y corregir",
                    checked = soundEnabled,
                    onCheckedChange = { appViewModel.setSoundEnabled(it) }
                )
            }
        }

        GlowCard(accent = RsColors.Cyan) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "RS-COM2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RsColors.Cyan
                )
                Text(
                    "App realizada por:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RsColors.TextSecondary
                )
                Text(
                    "Gutierrez Enzo Leonel\nRecalde Lisandro Facundo\nAylan Mauricio Gaston",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = RsColors.TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

private val Glossary = mapOf(
    "Símbolo" to "Unidad de datos que el código protege. Aquí, un byte (0–255), elemento de GF(256).",
    "Código" to "Conjunto de reglas matemáticas que transforma datos en palabras protegidas con redundancia.",
    "Redundancia" to "Información extra añadida para poder recuperar los datos si parte del mensaje se daña.",
    "Paridad" to "Símbolos de redundancia calculados por el codificador (el resto de la división entre polinomios).",
    "Polinomio" to "Expresión como 3x² + x + 7. Reed-Solomon representa mensajes y códigos como polinomios.",
    "Campo de Galois" to "Conjunto finito de números donde sumar, restar, multiplicar y dividir siempre funciona.",
    "GF(256)" to "El campo de Galois con 256 elementos: exactamente los valores de un byte. Sumar es XOR.",
    "Síndrome" to "Valor de comprobación Sⱼ = R(αʲ). Si todos son cero, el mensaje está intacto.",
    "Error" to "Símbolo cuyo valor cambió durante la transmisión sin que se sepa dónde.",
    "Erasure (borradura)" to "Error con posición conocida (p. ej. paquete perdido). Cuesta la mitad corregirla.",
    "Corrección" to "Proceso de localizar los errores, calcular su magnitud y restaurar los valores originales.",
    "Polinomio localizador" to "Λ(x): ecuación cuyas raíces indican las posiciones de los símbolos erróneos."
)

@Composable
private fun InfoParagraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = RsColors.TextSecondary, lineHeight = 22.sp)
}

@Composable
private fun ConceptRow(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, color = RsColors.TextPrimary, fontSize = 14.sp)
        Text(body, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AppRow(emoji: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = RsColors.TextPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = RsColors.TextPrimary, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RsColors.TextSecondary)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RsColors.Background,
                checkedTrackColor = RsColors.Cyan,
                uncheckedThumbColor = RsColors.TextDim,
                uncheckedTrackColor = RsColors.SurfaceGlow
            )
        )
    }
}

@Composable
private fun FlowDiagram() {
    val nodes = listOf(
        "MENSAJE ORIGINAL" to RsColors.Cyan,
        "DATOS" to RsColors.Cyan,
        "REED-SOLOMON" to RsColors.Purple,
        "REDUNDANCIA" to RsColors.Purple,
        "MENSAJE CODIFICADO" to RsColors.Cyan,
        "TRANSMISIÓN" to RsColors.TextSecondary,
        "⚠ POSIBLE ERROR" to RsColors.Warning,
        "DECODIFICADOR" to RsColors.Purple,
        "DETECCIÓN" to RsColors.Warning,
        "CORRECCIÓN" to RsColors.Success,
        "MENSAJE ORIGINAL ✓" to RsColors.Success
    )
    // Flechas con brillo fijo: el diagrama es estático para no consumir CPU en reposo.
    val glow = 0.85f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        nodes.forEachIndexed { i, (label, color) ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(label, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 13.sp)
            }
            if (i < nodes.lastIndex) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = RsColors.Cyan,
                    modifier = Modifier
                        .size(22.dp)
                        .alpha(glow)
                )
            }
        }
    }
}
