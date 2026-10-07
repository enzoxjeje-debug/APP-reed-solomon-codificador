package com.example.reedsolomonlab.domain.reedsolomon

import com.example.reedsolomonlab.data.EducationalStep
import com.example.reedsolomonlab.data.MessageCodec
import com.example.reedsolomonlab.data.RSConfig
import com.example.reedsolomonlab.data.StepType
import com.example.reedsolomonlab.data.SymbolCard
import com.example.reedsolomonlab.data.SymbolGroup
import com.example.reedsolomonlab.data.SymbolState

/**
 * Codificador y decodificador Reed-Solomon sobre GF(256).
 *
 * - Polinomio generador: g(x) = (x − α⁰)(x − α¹)…(x − α^(2t−1)).
 * - Codificación sistemática: palabra = datos ∥ paridad.
 * - Decodificación: síndromes → Berlekamp-Massey → Chien → Forney.
 *
 * Cada método *Traced genera, ADEMÁS de calcular, los pasos educativos
 * con los valores reales del algoritmo. La UI solo los renderiza.
 */
class ReedSolomon(val dataSymbols: Int, val paritySymbols: Int) {

    init {
        require(dataSymbols >= 1) { "k debe ser ≥ 1" }
        require(paritySymbols >= 1) { "nsym debe ser ≥ 1" }
        require(dataSymbols + paritySymbols <= 255) {
            "El bloque (datos + paridad) no puede superar 255 símbolos en GF(256)"
        }
    }

    val blockLength: Int get() = dataSymbols + paritySymbols
    val correctionCapacity: Int get() = paritySymbols / 2

    val generator: GFPoly by lazy { buildGeneratorTrace().generator }

    // ------------------------------------------------------------------
    // Generador
    // ------------------------------------------------------------------

    data class GeneratorBuild(
        val generator: GFPoly,
        /** Líneas intermedias reales: g₁(x), g₂(x), …, g_2t(x). */
        val intermediate: List<String>
    )

    fun buildGeneratorTrace(): GeneratorBuild {
        var g = GFPoly.ONE
        val lines = mutableListOf("g₀(x) = 1")
        for (i in 0 until paritySymbols) {
            val factor = GFPoly(intArrayOf(GF256.alphaPower(i), 1))
            g = g * factor
            lines.add("g${i + 1}(x) = g${i}(x)·(x + α^$i) = ${g.toDisplayString()}")
        }
        return GeneratorBuild(g, lines)
    }

    // ------------------------------------------------------------------
    // Codificación
    // ------------------------------------------------------------------

    data class DivisionStep(
        val iteration: Int,
        val eliminatedDegree: Int,
        val topCoefficient: Int,
        val factor: Int,
        /** Resto parcial REAL tras esta iteración de la división larga. */
        val partialRemainder: GFPoly
    )

    data class BlockEncoding(
        val blockIndex: Int,
        val data: IntArray,
        val paddedZeros: Int,
        val parity: IntArray,
        val codeword: IntArray,
        val messagePoly: GFPoly,
        val shiftedPoly: GFPoly,
        val remainder: GFPoly,
        val divisionSteps: List<DivisionStep>
    )

    fun encodeBlock(data: IntArray): BlockEncoding {
        require(data.size == dataSymbols) { "El bloque debe tener $dataSymbols símbolos" }
        val gen = generator
        val messagePoly = GFPoly.fromSymbols(data)
        val shifted = messagePoly.shiftLeft(paritySymbols)

        // División larga con traza real paso a paso.
        // Si el mensaje ya es cero (o de grado menor que g), el resto es él mismo.
        if (shifted.degree < gen.degree) {
            val parity = shifted.toSymbols(paritySymbols)
            return BlockEncoding(
                blockIndex = 0,
                data = data,
                paddedZeros = 0,
                parity = parity,
                codeword = data + parity,
                messagePoly = messagePoly,
                shiftedPoly = shifted,
                remainder = shifted,
                divisionSteps = emptyList()
            )
        }
        val rem = shifted.coeffs.copyOf()
        val qSize = shifted.degree - gen.degree + 1
        val quot = IntArray(qSize)
        val dLeadInv = GF256.inv(gen.leading)
        val steps = mutableListOf<DivisionStep>()
        for (i in qSize - 1 downTo 0) {
            val idx = i + gen.degree
            val top = rem[idx]
            val factor = GF256.mul(top, dLeadInv)
            quot[i] = factor
            if (factor != 0) {
                for (j in 0..gen.degree) {
                    rem[i + j] = rem[i + j] xor GF256.mul(factor, gen.coeffs[j])
                }
            }
            steps.add(
                DivisionStep(
                    iteration = qSize - i,
                    eliminatedDegree = idx,
                    topCoefficient = top,
                    factor = factor,
                    partialRemainder = GFPoly(rem.copyOf())
                )
            )
        }
        val remainder = GFPoly(rem)
        val parity = remainder.toSymbols(paritySymbols)
        val codeword = data + parity
        return BlockEncoding(
            blockIndex = 0,
            data = data,
            paddedZeros = 0,
            parity = parity,
            codeword = codeword,
            messagePoly = messagePoly,
            shiftedPoly = shifted,
            remainder = remainder,
            divisionSteps = steps
        )
    }

    /** Divide los símbolos en bloques de [dataSymbols], rellenando con ceros el último. */
    fun encodeAll(symbols: IntArray): List<BlockEncoding> {
        require(symbols.isNotEmpty()) { "No hay símbolos que codificar" }
        val blocks = mutableListOf<BlockEncoding>()
        val count = (symbols.size + dataSymbols - 1) / dataSymbols
        for (b in 0 until count) {
            val from = b * dataSymbols
            val to = minOf(from + dataSymbols, symbols.size)
            val chunk = symbols.copyOfRange(from, to)
            val padded = if (chunk.size < dataSymbols) {
                chunk + IntArray(dataSymbols - chunk.size)
            } else chunk
            val encoded = encodeBlock(padded)
            blocks.add(encoded.copy(blockIndex = b, paddedZeros = dataSymbols - chunk.size))
        }
        return blocks
    }

    // ------------------------------------------------------------------
    // Síndromes
    // ------------------------------------------------------------------

    /**
     * S_j = R(α^j) para j = 0 … 2t−1.
     * Una palabra válida cumple S_j = 0 para todo j.
     */
    fun computeSyndromes(codeword: IntArray): IntArray {
        val poly = GFPoly.fromSymbols(codeword)
        return IntArray(paritySymbols) { j -> poly.evaluate(GF256.alphaPower(j)) }
    }

    // ------------------------------------------------------------------
    // Berlekamp-Massey
    // ------------------------------------------------------------------

    data class BMIteration(
        val n: Int,
        val discrepancy: Int,
        val lengthBefore: Int,
        val lengthAfter: Int,
        val action: String
    )

    data class BMResult(val lambda: GFPoly, val iterations: List<BMIteration>)

    /** Encuentra el polinomio localizador de errores Λ(x) a partir de los síndromes. */
    fun berlekampMassey(syndromes: IntArray): BMResult {
        var lambda = GFPoly.ONE
        var b = GFPoly.ONE
        var l = 0
        var m = 1
        var bScale = 1
        val trace = mutableListOf<BMIteration>()
        for (n in syndromes.indices) {
            var delta = syndromes[n]
            for (j in 1..l) {
                // Acceso seguro: el polinomio normalizado puede ser más corto que l+1
                // (los coeficientes ausentes son cero).
                val lambdaJ = if (j < lambda.coeffs.size) lambda.coeffs[j] else 0
                delta = delta xor GF256.mul(lambdaJ, syndromes[n - j])
            }
            val lBefore = l
            val action: String
            if (delta == 0) {
                m++
                action = "Δ = 0: el polinomio actual ya explica el síndrome $n; se mantiene Λ(x)."
            } else if (2 * l <= n) {
                val t = lambda
                val factor = GF256.div(delta, bScale)
                lambda = polyAddShifted(lambda, b, factor, m)
                l = n + 1 - l
                b = t
                bScale = delta
                m = 1
                action = "Δ ≠ 0 y 2L ≤ n: se corrige Λ(x), la longitud pasa de $lBefore a $l y se guarda la copia anterior."
            } else {
                val factor = GF256.div(delta, bScale)
                lambda = polyAddShifted(lambda, b, factor, m)
                m++
                action = "Δ ≠ 0 pero 2L > n: se ajusta Λ(x) sin aumentar su longitud."
            }
            trace.add(BMIteration(n, delta, lBefore, l, action))
        }
        return BMResult(lambda, trace)
    }

    private fun polyAddShifted(a: GFPoly, b: GFPoly, factor: Int, shift: Int): GFPoly {
        val out = IntArray(maxOf(a.coeffs.size, b.coeffs.size + shift))
        a.coeffs.copyInto(out)
        if (factor != 0) {
            for (j in b.coeffs.indices) {
                if (b.coeffs[j] == 0) continue
                out[j + shift] = out[j + shift] xor GF256.mul(factor, b.coeffs[j])
            }
        }
        return GFPoly(out)
    }

    // ------------------------------------------------------------------
    // Chien + evaluador + Forney
    // ------------------------------------------------------------------

    data class ChienCheck(
        val position: Int,
        val degree: Int,
        val evaluation: Int,
        val isRoot: Boolean
    )

    /** Busca las raíces de Λ(x): Λ(α^(−m)) = 0 ⇒ error en el término de grado m. */
    fun chienSearch(lambda: GFPoly, codewordLength: Int): List<ChienCheck> =
        (0 until codewordLength).map { pos ->
            val degree = codewordLength - 1 - pos
            val xInv = GF256.alphaPower(-degree)
            val eval = lambda.evaluate(xInv)
            ChienCheck(pos, degree, eval, eval == 0)
        }

    /** Polinomio evaluador de errores: Ω(x) = S(x)·Λ(x) mod x^(2t). */
    fun errorEvaluator(syndromes: IntArray, lambda: GFPoly): GFPoly {
        val sPoly = GFPoly(syndromes.copyOf())
        val product = sPoly * lambda
        val kept = IntArray(minOf(paritySymbols, product.coeffs.size)) { product.coeffs[it] }
        return GFPoly(if (kept.isEmpty()) intArrayOf(0) else kept)
    }

    data class ForneyDetail(
        val position: Int,
        val degree: Int,
        val locatorValueX: Int,
        val omegaEval: Int,
        val lambdaPrimeEval: Int,
        val magnitude: Int,
        val received: Int,
        val corrected: Int
    )

    /**
     * Magnitud del error (fórmula de Forney, primera raíz consecutiva α⁰):
     * Y = X · Ω(X⁻¹) / Λ′(X⁻¹), con X = α^m.
     */
    fun forneyMagnitudes(
        received: IntArray,
        errorPositions: List<Int>,
        omega: GFPoly,
        lambdaPrime: GFPoly
    ): List<ForneyDetail> {
        val n = received.size
        return errorPositions.map { pos ->
            val degree = n - 1 - pos
            val x = GF256.alphaPower(degree)
            val xInv = GF256.alphaPower(-degree)
            val omegaEval = omega.evaluate(xInv)
            val primeEval = lambdaPrime.evaluate(xInv)
            val magnitude = if (primeEval == 0) 0
            else GF256.mul(x, GF256.div(omegaEval, primeEval))
            ForneyDetail(
                position = pos,
                degree = degree,
                locatorValueX = x,
                omegaEval = omegaEval,
                lambdaPrimeEval = primeEval,
                magnitude = magnitude,
                received = received[pos],
                corrected = received[pos] xor magnitude
            )
        }
    }

    // ------------------------------------------------------------------
    // Decodificación de un bloque
    // ------------------------------------------------------------------

    enum class DecodeStatus { NO_ERROR, CORRECTED, TOO_MANY_ERRORS }

    data class BlockDecoding(
        val blockIndex: Int,
        val received: IntArray,
        val status: DecodeStatus,
        val syndromes: IntArray,
        val allSyndromesZero: Boolean,
        val bm: BMResult?,
        val chien: List<ChienCheck>,
        val errorPositions: List<Int>,
        val omega: GFPoly?,
        val forney: List<ForneyDetail>,
        val corrected: IntArray,
        val data: IntArray,
        val verificationSyndromes: IntArray?,
        val note: String?
    )

    fun decodeBlock(received: IntArray, blockIndex: Int = 0): BlockDecoding {
        require(received.size == blockLength) {
            "Cada bloque debe tener $blockLength símbolos (datos $dataSymbols + paridad $paritySymbols)"
        }
        val syndromes = computeSyndromes(received)
        if (syndromes.all { it == 0 }) {
            return BlockDecoding(
                blockIndex = blockIndex,
                received = received.copyOf(),
                status = DecodeStatus.NO_ERROR,
                syndromes = syndromes,
                allSyndromesZero = true,
                bm = null,
                chien = emptyList(),
                errorPositions = emptyList(),
                omega = null,
                forney = emptyList(),
                corrected = received.copyOf(),
                data = received.copyOfRange(0, dataSymbols),
                verificationSyndromes = null,
                note = null
            )
        }

        val bm = berlekampMassey(syndromes)
        val lambda = bm.lambda
        if (lambda.degree > correctionCapacity || lambda.degree <= 0) {
            return BlockDecoding(
                blockIndex = blockIndex,
                received = received.copyOf(),
                status = DecodeStatus.TOO_MANY_ERRORS,
                syndromes = syndromes,
                allSyndromesZero = false,
                bm = bm,
                chien = emptyList(),
                errorPositions = emptyList(),
                omega = null,
                forney = emptyList(),
                corrected = received.copyOf(),
                data = received.copyOfRange(0, dataSymbols),
                verificationSyndromes = null,
                note = "El polinomio localizador tiene grado ${lambda.degree}, " +
                    "pero este código solo puede corregir hasta $correctionCapacity errores por bloque."
            )
        }

        val chien = chienSearch(lambda, blockLength)
        val positions = chien.filter { it.isRoot }.map { it.position }
        if (positions.size != lambda.degree) {
            return BlockDecoding(
                blockIndex = blockIndex,
                received = received.copyOf(),
                status = DecodeStatus.TOO_MANY_ERRORS,
                syndromes = syndromes,
                allSyndromesZero = false,
                bm = bm,
                chien = chien,
                errorPositions = positions,
                omega = null,
                forney = emptyList(),
                corrected = received.copyOf(),
                data = received.copyOfRange(0, dataSymbols),
                verificationSyndromes = null,
                note = "La búsqueda de Chien encontró ${positions.size} raíces pero el " +
                    "polinomio localizador predice ${lambda.degree} errores: hay demasiados errores."
            )
        }

        val omega = errorEvaluator(syndromes, lambda)
        val lambdaPrime = lambda.derivative()
        val forney = forneyMagnitudes(received, positions, omega, lambdaPrime)
        val corrected = received.copyOf()
        for (f in forney) corrected[f.position] = f.corrected

        val verification = computeSyndromes(corrected)
        return if (verification.all { it == 0 }) {
            BlockDecoding(
                blockIndex = blockIndex,
                received = received.copyOf(),
                status = DecodeStatus.CORRECTED,
                syndromes = syndromes,
                allSyndromesZero = false,
                bm = bm,
                chien = chien,
                errorPositions = positions,
                omega = omega,
                forney = forney,
                corrected = corrected,
                data = corrected.copyOfRange(0, dataSymbols),
                verificationSyndromes = verification,
                note = null
            )
        } else {
            BlockDecoding(
                blockIndex = blockIndex,
                received = received.copyOf(),
                status = DecodeStatus.TOO_MANY_ERRORS,
                syndromes = syndromes,
                allSyndromesZero = false,
                bm = bm,
                chien = chien,
                errorPositions = positions,
                omega = omega,
                forney = forney,
                corrected = corrected,
                data = corrected.copyOfRange(0, dataSymbols),
                verificationSyndromes = verification,
                note = "Tras intentar corregir, los síndromes siguen sin ser cero: " +
                    "los errores superan la capacidad del código ($correctionCapacity por bloque)."
            )
        }
    }

    // ------------------------------------------------------------------
    // Trazas educativas de codificación
    // ------------------------------------------------------------------

    data class CharBreakdown(val char: String, val bytes: List<Int>)

    data class EncodingTrace(
        val config: RSConfig,
        val inputSymbols: IntArray,
        val inputText: String?,
        val charBreakdown: List<CharBreakdown>,
        val blocks: List<BlockEncoding>,
        val codeword: IntArray,
        val shareable: String,
        val steps: List<EducationalStep>
    )

    /**
     * Codifica [symbols] y genera los pasos educativos con los cálculos reales.
     * @param inputText texto original, si el origen es texto.
     * @param breakdown desglose carácter → bytes (solo modo texto).
     * @param beginnerMode si está activo, las explicaciones son más simples.
     */
    fun buildEncodingTrace(
        symbols: IntArray,
        config: RSConfig,
        inputText: String?,
        breakdown: List<CharBreakdown>,
        beginnerMode: Boolean
    ): EncodingTrace {
        val blocks = encodeAll(symbols)
        val codeword = blocks.flatMap { it.codeword.toList() }.toIntArray()
        val shareable = MessageCodec.toShareableString(codeword, config, symbols.size)
        val steps = mutableListOf<EducationalStep>()

        // PASO 1 — Conversión del mensaje.
        steps.add(
            EducationalStep(
                id = "enc-1-conversion",
                title = "Paso 1 · Conversión del mensaje",
                explanation = if (inputText != null) {
                    if (beginnerMode) {
                        "Tu texto «$inputText» se convierte en números (símbolos). " +
                            "Cada letra se transforma en su código de ordenador: por ejemplo, la «H» es el 72. " +
                            "Reed-Solomon trabaja con estos números, no con letras."
                    } else {
                        "El mensaje «$inputText» se convierte a bytes UTF-8. " +
                            "Cada byte (0–255) es un símbolo del campo finito GF(256): " +
                            "Reed-Solomon opera con símbolos del campo, no con caracteres."
                    }
                } else {
                    if (beginnerMode) {
                        "Tu mensaje ya son números, así que se usan directamente como símbolos. " +
                            "Cada número entre 0 y 255 es un símbolo que el código puede proteger."
                    } else {
                        "Entrada numérica directa: cada valor 0–255 se interpreta como un símbolo de GF(256)."
                    }
                },
                values = breakdown.flatMap { cb ->
                    cb.bytes.map { b -> "${cb.char} → $b" }
                }.take(64),
                symbolGroups = listOf(
                    SymbolGroup(
                        label = "Símbolos de entrada (${symbols.size})",
                        symbols = symbols.take(64).map { SymbolCard(it, state = SymbolState.DATA) },
                        explanation = if (symbols.size > 64) "Se muestran los primeros 64 de ${symbols.size}." else null
                    )
                ),
                type = StepType.DATA
            )
        )

        // PASO 2 — Representación de los símbolos.
        steps.add(
            EducationalStep(
                id = "enc-2-simbolos",
                title = "Paso 2 · Representación de los símbolos",
                explanation = if (beginnerMode) {
                    "Cada número viaja dentro de una «caja» llamada símbolo. " +
                        "Reed-Solomon protege símbolos enteros: si un símbolo se daña, " +
                        "da igual cuántos bits cambien dentro de él, cuenta como un solo error."
                } else {
                    "Cada símbolo es un elemento de GF(256) (un byte). " +
                        "Reed-Solomon es un código no binario: corrige símbolos completos, " +
                        "por lo que una ráfaga de errores dentro del mismo byte cuesta un solo error."
                },
                symbolGroups = listOf(
                    SymbolGroup(
                        label = "Tus símbolos",
                        symbols = symbols.take(32).mapIndexed { i, s ->
                            SymbolCard(s, label = "s$i", state = SymbolState.DATA)
                        },
                        explanation = if (symbols.size > 32) "Se muestran los primeros 32 de ${symbols.size}." else null
                    )
                ),
                type = StepType.SYMBOLS
            )
        )

        // PASO 3 — Construcción del código / parámetros.
        val n = config.dataSymbols + config.paritySymbols
        steps.add(
            EducationalStep(
                id = "enc-3-parametros",
                title = "Paso 3 · Construcción del código",
                explanation = if (beginnerMode) {
                    "Agrupamos tus símbolos en bloques de ${config.dataSymbols} y a cada bloque le añadimos " +
                        "${config.paritySymbols} símbolos extra de «paridad» (redundancia). " +
                        "Con ${config.paritySymbols} símbolos extra, el código puede corregir hasta " +
                        "${config.correctionCapacity} símbolos dañados por bloque. Regla: con 2t extras se corrigen t errores."
                } else {
                    "Código RS($n, ${config.dataSymbols}) sobre GF(256): cada bloque de ${config.dataSymbols} " +
                        "símbolos de datos se amplía con ${config.paritySymbols} símbolos de paridad (2t). " +
                        "Distancia mínima d = 2t+1 = ${config.paritySymbols + 1}: corrige hasta " +
                        "t = ${config.correctionCapacity} errores por bloque."
                },
                formula = "datos (${config.dataSymbols}) + paridad (${config.paritySymbols}) = bloque ($n)",
                values = listOf(
                    "Bloques necesarios: ${blocks.size}",
                    "Paridad por bloque: ${config.paritySymbols} símbolos",
                    "Capacidad de corrección: ${config.correctionCapacity} errores por bloque"
                ) + if (blocks.size > 1) listOf(
                    "Tu mensaje ocupa ${symbols.size} símbolos: se divide en ${blocks.size} bloques " +
                        "(el último se rellena con ceros hasta ${config.dataSymbols})."
                ) else emptyList(),
                type = StepType.INFO
            )
        )

        // PASOS 4–5 por bloque.
        blocks.forEachIndexed { bi, block ->
            val tag = if (blocks.size > 1) " (bloque ${bi + 1} de ${blocks.size})" else ""
            val simpleHeader = blocks.size > 1 && bi > 0
            // Generador (solo detallado en el primer bloque o si hay uno solo).
            val genLines = if (!simpleHeader) {
                buildGeneratorTrace().intermediate
            } else emptyList()

            steps.add(
                EducationalStep(
                    id = "enc-4-redundancia-$bi",
                    title = "Paso 4 · Generación de redundancia$tag",
                    explanation = if (beginnerMode) {
                        "Ahora calculamos los ${config.paritySymbols} símbolos extra con una receta matemática " +
                            "llamada polinomio generador. Es una división de polinomios: lo que «sobra» " +
                            "(el resto) son los símbolos de paridad. Abre el detalle para ver la división real."
                    } else {
                        "Codificación sistemática: se forma m(x) = D(x)·x^${config.paritySymbols} y se divide " +
                            "entre el generador g(x). El resto r(x) = m(x) mod g(x) son los símbolos de paridad. " +
                            "La palabra válida es c(x) = m(x) − r(x), múltiplo de g(x)."
                    },
                    formula = "paridad = resto de [ D(x)·x^${config.paritySymbols} ÷ g(x) ]",
                    values = (if (!simpleHeader) {
                        listOf("Polinomio generador: g(x) = ${generator.toDisplayString()}") + genLines
                    } else {
                        listOf("g(x) = ${generator.toDisplayString()}")
                    }) + listOf(
                        "Polinomio del mensaje: D(x) = ${block.messagePoly.toDisplayString().take(220)}",
                        "Resto (paridad): r(x) = ${block.remainder.toDisplayString()}"
                    ),
                    symbolGroups = listOf(
                        SymbolGroup(
                            label = "Símbolos de paridad calculados",
                            symbols = block.parity.mapIndexed { i, p ->
                                SymbolCard(p, label = "p$i", state = SymbolState.PARITY)
                            }
                        )
                    ),
                    type = StepType.POLYNOMIAL,
                    collapsible = true,
                    defaultExpanded = !beginnerMode
                )
            )

            // Detalle de la división larga (paso matemático expandible).
            steps.add(
                EducationalStep(
                    id = "enc-4b-division-$bi",
                    title = "Detalle · División larga de polinomios$tag",
                    explanation = if (beginnerMode) {
                        "Así se hizo la división, fila a fila, con números reales. " +
                            "En cada fila se elimina el término de mayor grado restando " +
                            "un múltiplo del generador. Al final, lo que queda es la paridad."
                    } else {
                        "Traza exacta de la división larga en GF(256): en cada iteración se elimina " +
                            "el término de mayor grado con el factor c/gₗ y se actualiza el resto parcial. " +
                            "El resto final es la paridad."
                    },
                    values = block.divisionSteps.map { s ->
                        if (s.factor == 0) {
                            "Fila ${s.iteration}: el coeficiente de x^${s.eliminatedDegree} ya es 0, no se resta nada."
                        } else {
                            "Fila ${s.iteration}: término x^${s.eliminatedDegree} con coeficiente ${s.topCoefficient}; " +
                                "factor = ${s.topCoefficient} ÷ ${generator.leading} = ${s.factor}; " +
                                "resto parcial = ${s.partialRemainder.toDisplayString().take(160)}"
                        }
                    },
                    type = StepType.MATH,
                    collapsible = true,
                    defaultExpanded = false
                )
            )

            steps.add(
                EducationalStep(
                    id = "enc-5-codificado-$bi",
                    title = "Paso 5 · Mensaje codificado$tag",
                    explanation = if (beginnerMode) {
                        "Unimos tus datos con la paridad que acabamos de calcular. " +
                            "Este es el mensaje protegido que viajaría por el canal: " +
                            "aunque algunos números se dañen, la paridad permitirá recuperarlos."
                    } else {
                        "Palabra sistemática c = [datos | paridad], múltiplo del generador: " +
                            "sus síndromes serán cero en el receptor si no hay errores."
                    },
                    symbolGroups = listOf(
                        SymbolGroup(
                            label = "Datos",
                            symbols = block.data.mapIndexed { i, d ->
                                SymbolCard(
                                    d, label = "d$i", state = SymbolState.DATA,
                                    note = if (block.paddedZeros > 0 && i >= block.data.size - block.paddedZeros) "relleno" else null
                                )
                            }
                        ),
                        SymbolGroup(
                            label = "Paridad",
                            symbols = block.parity.mapIndexed { i, p ->
                                SymbolCard(p, label = "p$i", state = SymbolState.PARITY)
                            }
                        )
                    ),
                    values = listOf(
                        "Decimal: ${block.codeword.joinToString(" ")}",
                        "Hexadecimal: ${block.codeword.joinToString(" ") { it.toString(16).uppercase().padStart(2, '0') }}"
                    ),
                    type = StepType.SUCCESS
                )
            )
        }

        return EncodingTrace(
            config = config,
            inputSymbols = symbols.copyOf(),
            inputText = inputText,
            charBreakdown = breakdown,
            blocks = blocks,
            codeword = codeword,
            shareable = shareable,
            steps = steps
        )
    }

    // ------------------------------------------------------------------
    // Trazas educativas de decodificación
    // ------------------------------------------------------------------

    data class DecodingTrace(
        val config: RSConfig,
        val received: IntArray,
        val blocks: List<BlockDecoding>,
        val status: DecodeStatus,
        val errorMessage: String?,
        val recoveredSymbols: IntArray,
        val recoveredText: String,
        val steps: List<EducationalStep>
    )

    /**
     * Decodifica [received] (múltiplo del tamaño de bloque) y genera los pasos
     * educativos con los cálculos reales del decodificador.
     * @param dataLength longitud real de los datos (sin relleno), si se conoce.
     */
    fun buildDecodingTrace(
        received: IntArray,
        config: RSConfig,
        dataLength: Int?,
        beginnerMode: Boolean
    ): DecodingTrace {
        if (received.isEmpty()) {
            return DecodingTrace(
                config, received, emptyList(), DecodeStatus.TOO_MANY_ERRORS,
                "No se recibió ningún símbolo.", IntArray(0), "", emptyList()
            )
        }
        val n = config.dataSymbols + config.paritySymbols
        if (received.size % n != 0) {
            return DecodingTrace(
                config, received, emptyList(), DecodeStatus.TOO_MANY_ERRORS,
                "El mensaje recibido tiene ${received.size} símbolos, que no es múltiplo del " +
                    "tamaño de bloque ($n). Revisa la configuración o el mensaje pegado.",
                IntArray(0), "", emptyList()
            )
        }

        val blockCount = received.size / n
        val decoded = (0 until blockCount).map { b ->
            decodeBlock(received.copyOfRange(b * n, (b + 1) * n), b)
        }
        val status = when {
            decoded.any { it.status == DecodeStatus.TOO_MANY_ERRORS } -> DecodeStatus.TOO_MANY_ERRORS
            decoded.any { it.status == DecodeStatus.CORRECTED } -> DecodeStatus.CORRECTED
            else -> DecodeStatus.NO_ERROR
        }

        // Recupera los datos y recorta el relleno si se conoce la longitud real.
        val allData = decoded.flatMap { it.data.toList() }.toIntArray()
        val recovered = if (dataLength != null && dataLength in 1..allData.size) {
            allData.copyOfRange(0, dataLength)
        } else allData
        val recoveredText = MessageCodec.symbolsToText(recovered)

        val steps = mutableListOf<EducationalStep>()
        decoded.forEachIndexed { bi, block ->
            val tag = if (blockCount > 1) " (bloque ${bi + 1} de $blockCount)" else ""
            val firstBlock = bi == 0

            if (firstBlock) {
                steps.add(
                    EducationalStep(
                        id = "dec-1-recepcion-$bi",
                        title = "Paso 1 · Recepción del mensaje$tag",
                        explanation = if (beginnerMode) {
                            "Estos son los números que llegaron por el canal. " +
                                "Pueden haber cambiado durante el viaje: ahora toca comprobar " +
                                "si siguen formando un mensaje Reed-Solomon válido."
                        } else {
                            "Palabra recibida r (longitud $n). Se comprobará si r es múltiplo " +
                                "del generador evaluando sus síndromes."
                        },
                        symbolGroups = listOf(
                            SymbolGroup(
                                label = "Símbolos recibidos",
                                symbols = block.received.mapIndexed { i, s -> SymbolCard(s, label = "$i") }
                            )
                        ),
                        type = StepType.DATA
                    )
                )
            } else {
                steps.add(
                    EducationalStep(
                        id = "dec-1-recepcion-$bi",
                        title = "Bloque ${bi + 1} · Símbolos recibidos",
                        explanation = "El mensaje se procesa por bloques; este es el bloque ${bi + 1}.",
                        symbolGroups = listOf(
                            SymbolGroup(
                                label = "Símbolos recibidos",
                                symbols = block.received.mapIndexed { i, s -> SymbolCard(s, label = "$i") }
                            )
                        ),
                        type = StepType.DATA
                    )
                )
            }

            // PASO 2 — Síndromes.
            val syndromeLines = block.syndromes.mapIndexed { j, s ->
                "S${j + 1} = R(α^$j) = $s"
            }
            if (block.allSyndromesZero) {
                steps.add(
                    EducationalStep(
                        id = "dec-2-sindromes-ok-$bi",
                        title = "Paso 2 · Cálculo de síndromes$tag",
                        explanation = if (beginnerMode) {
                            "El decodificador hace ${config.paritySymbols} comprobaciones matemáticas " +
                                "llamadas síndromes. Es como un «chequeo de salud» del mensaje. " +
                                "Todos dieron cero: el mensaje está intacto, no se detectaron errores."
                        } else {
                            "Síndromes Sⱼ = R(αʲ), j = 0…${config.paritySymbols - 1}. " +
                                "Al ser todos cero, r es múltiplo de g(x): no hay errores."
                        },
                        values = syndromeLines,
                        type = StepType.SUCCESS
                    )
                )
                steps.add(
                    EducationalStep(
                        id = "dec-3-separar-$bi",
                        title = "Paso 3 · Separación de datos y redundancia$tag",
                        explanation = if (beginnerMode) {
                            "Como no hay errores, simplemente separamos tus datos originales " +
                                "de los símbolos extra de paridad que solo servían para proteger."
                        } else {
                            "Sin errores, la recuperación es directa: los primeros ${config.dataSymbols} " +
                                "símbolos son los datos (código sistemático)."
                        },
                        symbolGroups = listOf(
                            SymbolGroup(
                                label = "Datos originales",
                                symbols = block.data.mapIndexed { i, s ->
                                    SymbolCard(s, label = "d$i", state = SymbolState.CHECKED)
                                }
                            ),
                            SymbolGroup(
                                label = "Paridad (se descarta)",
                                symbols = block.corrected.copyOfRange(config.dataSymbols, n).mapIndexed { i, s ->
                                    SymbolCard(s, label = "p$i", state = SymbolState.PARITY)
                                }
                            )
                        ),
                        type = StepType.SYMBOLS
                    )
                )
                steps.add(
                    EducationalStep(
                        id = "dec-4-recuperar-ok-$bi",
                        title = "Paso 4 · Recuperación del mensaje$tag",
                        explanation = "Mensaje codificado → se retira la paridad → datos originales.",
                        values = listOf("Datos: ${block.data.joinToString(" ")}"),
                        type = StepType.SUCCESS
                    )
                )
            } else {
                steps.add(
                    EducationalStep(
                        id = "dec-2-sindromes-err-$bi",
                        title = "Paso 2 · Cálculo de síndromes$tag",
                        explanation = if (beginnerMode) {
                            "El «chequeo de salud» encontró valores distintos de cero. " +
                                "Eso significa que algún número cambió durante el viaje: HAY ERRORES. " +
                                "Los valores exactos de los síndromes son la «huella» que deja el error " +
                                "y servirán para localizarlo."
                        } else {
                            "Al menos un síndrome es distinto de cero: r ya no es múltiplo de g(x). " +
                                "Los síndromes codifican la posición y la magnitud de los errores " +
                                "y alimentan el algoritmo de Berlekamp-Massey."
                        },
                        formula = "Sⱼ = R(αʲ)",
                        values = syndromeLines,
                        type = StepType.WARNING
                    )
                )

                val bm = block.bm
                if (bm != null) {
                    steps.add(
                        EducationalStep(
                            id = "dec-3-localizador-$bi",
                            title = "Paso 3 · Polinomio localizador de errores$tag",
                            explanation = if (beginnerMode) {
                                "Con la «huella» de los síndromes, el algoritmo de Berlekamp-Massey " +
                                    "construye una ecuación especial (el polinomio localizador). " +
                                    "Sus soluciones indican DÓNDE están los símbolos dañados, sin saber " +
                                    "todavía cuáles eran sus valores."
                            } else {
                                "El algoritmo de Berlekamp-Massey encuentra el polinomio Λ(x) de grado " +
                                    "mínimo cuyas raíces codifican las posiciones de error " +
                                    "(relación de recurrencia de los síndromes)."
                            },
                            formula = "Λ(x) = ${bm.lambda.toDisplayString()}",
                            values = if (beginnerMode) {
                                listOf("Ecuación encontrada: Λ(x) = ${bm.lambda.toDisplayString()}")
                            } else {
                                bm.iterations.map { it ->
                                    "n=${it.n}: Δ=${it.discrepancy}, L: ${it.lengthBefore}→${it.lengthAfter}. ${it.action}"
                                }
                            },
                            type = StepType.POLYNOMIAL,
                            collapsible = !beginnerMode,
                            defaultExpanded = beginnerMode
                        )
                    )
                }

                if (block.chien.isNotEmpty()) {
                    val roots = block.chien.filter { it.isRoot }
                    steps.add(
                        EducationalStep(
                            id = "dec-4-chien-$bi",
                            title = "Paso 4 · Búsqueda de Chien: ¿dónde está el error?$tag",
                            explanation = if (beginnerMode) {
                                "Ahora se «prueba» la ecuación en cada posición del mensaje. " +
                                    "Donde la ecuación da cero, hay un símbolo dañado. " +
                                    "Se ${if (roots.size == 1) "encontró 1 error" else "encontraron ${roots.size} errores"} " +
                                    "en ${if (roots.size == 1) "la posición" else "las posiciones"} " +
                                    "${roots.joinToString(" y ") { "«${it.position}»" }}."
                            } else {
                                "Se evalúa Λ(α^(−m)) para cada grado m del bloque: un cero indica " +
                                    "un error en esa posición. Raíces encontradas: " +
                                    roots.joinToString(", ") { "m=${it.degree} (posición ${it.position})" } + "."
                            },
                            values = if (beginnerMode) {
                                roots.map { "Λ(α^(−${it.degree})) = 0 → error en la posición ${it.position}" }
                            } else {
                                block.chien.map {
                                    "posición ${it.position} (grado ${it.degree}): Λ(α^(−${it.degree})) = ${it.evaluation}" +
                                        if (it.isRoot) "  ✓ raíz: ERROR AQUÍ" else ""
                                }
                            },
                            symbolGroups = listOf(
                                SymbolGroup(
                                    label = "Mapa de errores",
                                    symbols = block.received.mapIndexed { i, s ->
                                        SymbolCard(
                                            s, label = "$i",
                                            state = if (i in block.errorPositions) SymbolState.ERROR else SymbolState.NORMAL,
                                            note = if (i in block.errorPositions) "ERROR" else null
                                        )
                                    },
                                    explanation = "Las tarjetas en rojo son los símbolos dañados."
                                )
                            ),
                            type = StepType.ERROR_LOCATION
                        )
                    )
                }

                if (block.omega != null && block.forney.isNotEmpty()) {
                    steps.add(
                        EducationalStep(
                            id = "dec-5-forney-$bi",
                            title = "Paso 5 · Magnitud del error (Forney)$tag",
                            explanation = if (beginnerMode) {
                                "Ya sabemos DÓNDE está el error; falta saber CUÁNTO cambió. " +
                                    "Con otro cálculo (polinomio evaluador y fórmula de Forney) " +
                                    "se obtiene la magnitud: el valor exacto que hay que sumar " +
                                    "para devolver cada símbolo a su valor original."
                            } else {
                                "Con Ω(x) = S(x)·Λ(x) mod x^${config.paritySymbols} y Λ′(x), " +
                                    "la fórmula de Forney da cada magnitud: Y = X·Ω(X⁻¹)/Λ′(X⁻¹), con X = αᵐ."
                            },
                            formula = "Ω(x) = ${block.omega.toDisplayString().take(200)}",
                            values = block.forney.map { f ->
                                "Posición ${f.position}: recibido ${f.received}, " +
                                    "magnitud ${f.magnitude} " +
                                    "(X=α^${f.degree}=${f.locatorValueX}, Ω(X⁻¹)=${f.omegaEval}, Λ′(X⁻¹)=${f.lambdaPrimeEval}); " +
                                    "corregido = ${f.received} ⊕ ${f.magnitude} = ${f.corrected}"
                            } + if (!beginnerMode) listOf(
                                "Λ′(x) = ${block.bm?.lambda?.derivative()?.toDisplayString()}"
                            ) else emptyList(),
                            symbolGroups = listOf(
                                SymbolGroup(
                                    label = "Corrección",
                                    symbols = block.forney.flatMap { f ->
                                        listOf(
                                            SymbolCard(f.received, label = "pos ${f.position}", state = SymbolState.ERROR, note = "recibido"),
                                            SymbolCard(f.corrected, label = "pos ${f.position}", state = SymbolState.CORRECTED, note = "corregido")
                                        )
                                    },
                                    explanation = "Cada valor dañado se combina con su magnitud (XOR) para recuperar el original."
                                )
                            ),
                            type = StepType.CORRECTION
                        )
                    )
                }

                if (block.status == DecodeStatus.CORRECTED) {
                    steps.add(
                        EducationalStep(
                            id = "dec-6-verificacion-$bi",
                            title = "Paso 6 · Verificación y mensaje recuperado$tag",
                            explanation = if (beginnerMode) {
                                "Tras corregir, repetimos el «chequeo de salud»: todos los síndromes " +
                                    "vuelven a ser cero, así que el mensaje es válido. " +
                                    "Separamos los datos de la paridad y recuperamos el original."
                            } else {
                                "Verificación: los síndromes de la palabra corregida son todos cero. " +
                                    "Se extraen los ${config.dataSymbols} símbolos de datos."
                            },
                            values = (block.verificationSyndromes?.mapIndexed { j, s -> "S${j + 1}′ = $s" }
                                ?: emptyList()) + listOf("Datos: ${block.data.joinToString(" ")}"),
                            symbolGroups = listOf(
                                SymbolGroup(
                                    label = "Mensaje corregido",
                                    symbols = block.corrected.mapIndexed { i, s ->
                                        SymbolCard(
                                            s, label = "$i",
                                            state = if (i in block.errorPositions) SymbolState.CORRECTED else SymbolState.CHECKED,
                                            note = if (i in block.errorPositions) "corregido" else null
                                        )
                                    }
                                )
                            ),
                            type = StepType.SUCCESS
                        )
                    )
                } else {
                    steps.add(
                        EducationalStep(
                            id = "dec-6-imposible-$bi",
                            title = "No es posible corregir el mensaje$tag",
                            explanation = "Este código, con ${config.paritySymbols} símbolos de paridad, " +
                                "puede corregir hasta ${config.correctionCapacity} errores por bloque, " +
                                "pero el daño es mayor. ${block.note ?: ""} " +
                                "El decodificador se niega a inventar datos: es mejor avisar que devolver " +
                                "un mensaje falso.",
                            values = block.note?.let { listOf(it) } ?: emptyList(),
                            type = StepType.ERROR
                        )
                    )
                }
            }
        }

        if (blockCount >= 1) {
            steps.add(
                EducationalStep(
                    id = "dec-final",
                    title = if (status == DecodeStatus.TOO_MANY_ERRORS) "Resultado final" else "Mensaje recuperado",
                    explanation = when (status) {
                        DecodeStatus.NO_ERROR -> "El mensaje llegó intacto. Estos son los datos recuperados."
                        DecodeStatus.CORRECTED -> "Errores detectados y corregidos con éxito. Este es el mensaje original."
                        DecodeStatus.TOO_MANY_ERRORS -> "Algunos bloques no se pudieron corregir. No se puede garantizar el mensaje original."
                    },
                    values = listOf(
                        "Símbolos: ${recovered.joinToString(" ")}",
                        "Texto: $recoveredText"
                    ),
                    type = when (status) {
                        DecodeStatus.TOO_MANY_ERRORS -> StepType.ERROR
                        DecodeStatus.CORRECTED -> StepType.SUCCESS
                        DecodeStatus.NO_ERROR -> StepType.SUCCESS
                    }
                )
            )
        }

        return DecodingTrace(
            config = config,
            received = received.copyOf(),
            blocks = decoded,
            status = status,
            errorMessage = null,
            recoveredSymbols = recovered,
            recoveredText = recoveredText,
            steps = steps
        )
    }
}
