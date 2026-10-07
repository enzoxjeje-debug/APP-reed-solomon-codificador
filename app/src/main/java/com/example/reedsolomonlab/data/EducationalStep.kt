package com.example.reedsolomonlab.data

/**
 * Un paso educativo generado POR el motor Reed-Solomon mientras calcula.
 *
 * REGLA DE ORO: ningún paso se inventa. Cada valor mostrado proviene del
 * algoritmo real (síndromes, polinomios, divisiones, correcciones).
 * La interfaz solo renderiza esta lista.
 */
data class EducationalStep(
    val id: String,
    val title: String,
    val explanation: String,
    val formula: String? = null,
    val values: List<String> = emptyList(),
    val symbolGroups: List<SymbolGroup> = emptyList(),
    val type: StepType = StepType.INFO,
    /** Los pasos matemáticos extensos pueden colapsarse para no abrumar. */
    val collapsible: Boolean = false,
    val defaultExpanded: Boolean = true
)

enum class StepType {
    INFO, DATA, MATH, POLYNOMIAL, SYMBOLS, SUCCESS, WARNING, ERROR, ERROR_LOCATION, CORRECTION
}

data class SymbolGroup(
    val label: String,
    val symbols: List<SymbolCard>,
    val explanation: String? = null
)

data class SymbolCard(
    /** Valor del símbolo (0..255). */
    val value: Int,
    /** Etiqueta pequeña, p. ej. la posición o el carácter. */
    val label: String? = null,
    val state: SymbolState = SymbolState.NORMAL,
    val note: String? = null
)

enum class SymbolState {
    NORMAL, DATA, PARITY, ERROR, CORRECTED, CHECKED, HIDDEN
}

fun Int.toHex(): String = this.toString(16).uppercase().padStart(2, '0')

fun Int.toBinary(): String = this.toString(2).padStart(8, '0')
