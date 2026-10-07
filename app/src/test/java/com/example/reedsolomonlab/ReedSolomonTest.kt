package com.example.reedsolomonlab

import com.example.reedsolomonlab.data.MessageCodec
import com.example.reedsolomonlab.data.RSConfig
import com.example.reedsolomonlab.domain.reedsolomon.GFPoly
import com.example.reedsolomonlab.domain.reedsolomon.ReedSolomon
import com.example.reedsolomonlab.viewmodel.detectRsConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Corrección matemática del motor Reed-Solomon:
 * codificar/decodificar sin errores, con errores dentro de la capacidad,
 * con exceso de errores, varias longitudes, binario y casos borde.
 */
class ReedSolomonTest {

    private fun rs(k: Int = 8, nsym: Int = 4) = ReedSolomon(k, nsym)

    private fun corrupt(codeword: IntArray, vararg posToValue: Pair<Int, Int>): IntArray {
        val out = codeword.copyOf()
        for ((pos, value) in posToValue) out[pos] = value
        return out
    }

    // ---- 1. Sin errores ----

    @Test
    fun `codificar y decodificar sin errores recupera los datos`() {
        val codec = rs()
        val data = intArrayOf(72, 111, 108, 97, 32, 109, 117, 110)
        val codeword = codec.encodeBlock(data).codeword
        assertEquals(12, codeword.size)
        assertTrue(codec.computeSyndromes(codeword).all { it == 0 })
        val decoded = codec.decodeBlock(codeword)
        assertEquals(ReedSolomon.DecodeStatus.NO_ERROR, decoded.status)
        assertArrayEquals(data, decoded.data)
        assertArrayEquals(codeword, decoded.corrected)
    }

    @Test
    fun `palabra valida siempre tiene sindromes cero`() {
        val codec = rs()
        val rnd = Random(42)
        repeat(50) {
            val data = IntArray(8) { rnd.nextInt(256) }
            val codeword = codec.encodeBlock(data).codeword
            assertTrue(codec.computeSyndromes(codeword).all { it == 0 })
        }
    }

    // ---- 2. Un símbolo corrupto ----

    @Test
    fun `un error en cualquier posicion se corrige`() {
        val codec = rs()
        val data = intArrayOf(72, 111, 108, 97, 32, 82, 83, 33)
        val codeword = codec.encodeBlock(data).codeword
        for (pos in codeword.indices) {
            val bad = (codeword[pos] + 137) % 256
            val received = corrupt(codeword, pos to bad)
            val decoded = codec.decodeBlock(received)
            assertEquals("posición $pos", ReedSolomon.DecodeStatus.CORRECTED, decoded.status)
            assertArrayEquals("posición $pos", data, decoded.data)
            assertEquals(listOf(pos), decoded.errorPositions.sorted())
        }
    }

    @Test
    fun `un error tambien se corrige en la zona de paridad`() {
        val codec = rs()
        val data = IntArray(8) { it * 31 % 256 }
        val codeword = codec.encodeBlock(data).codeword
        // Las últimas 4 posiciones son paridad.
        for (pos in 8 until 12) {
            val received = corrupt(codeword, pos to ((codeword[pos] + 1) % 256))
            val decoded = codec.decodeBlock(received)
            assertEquals(ReedSolomon.DecodeStatus.CORRECTED, decoded.status)
            assertArrayEquals(data, decoded.data)
        }
    }

    // ---- 3. Múltiples errores dentro de la capacidad ----

    @Test
    fun `dos errores dentro de la capacidad se corrigen`() {
        val codec = rs()
        val rnd = Random(99)
        repeat(40) {
            val data = IntArray(8) { rnd.nextInt(256) }
            val codeword = codec.encodeBlock(data).codeword
            val p1 = rnd.nextInt(12)
            var p2 = rnd.nextInt(12)
            while (p2 == p1) p2 = rnd.nextInt(12)
            val v1 = (codeword[p1] + 1 + rnd.nextInt(255)) % 256
            val v2 = (codeword[p2] + 1 + rnd.nextInt(255)) % 256
            val decoded = codec.decodeBlock(corrupt(codeword, p1 to v1, p2 to v2))
            assertEquals(
                "iteración $it posiciones $p1,$p2",
                ReedSolomon.DecodeStatus.CORRECTED, decoded.status
            )
            assertArrayEquals(data, decoded.data)
            assertEquals(setOf(p1, p2), decoded.errorPositions.toSet())
        }
    }

    @Test
    fun `error con valor cero y 255`() {
        val codec = rs()
        val data = intArrayOf(0, 255, 0, 255, 1, 2, 3, 4)
        val codeword = codec.encodeBlock(data).codeword
        val received = corrupt(codeword, 0 to 200, 1 to 0)
        val decoded = codec.decodeBlock(received)
        assertEquals(ReedSolomon.DecodeStatus.CORRECTED, decoded.status)
        assertArrayEquals(data, decoded.data)
    }

    // ---- 4. Demasiados errores ----

    @Test
    fun `tres errores superan la capacidad 2 y se rechazan`() {
        val codec = rs() // capacidad 2
        val rnd = Random(1234)
        var detected = 0
        var miscorrected = 0
        repeat(60) {
            val data = IntArray(8) { rnd.nextInt(256) }
            val codeword = codec.encodeBlock(data).codeword
            val positions = (codeword.indices).shuffled(rnd).take(3)
            val received = codeword.copyOf()
            for (p in positions) received[p] = (received[p] + 1 + rnd.nextInt(255)) % 256
            val decoded = codec.decodeBlock(received)
            if (decoded.status == ReedSolomon.DecodeStatus.TOO_MANY_ERRORS) {
                detected++
            } else {
                // Si el decodificador cree haber corregido, los síndromes deben ser cero
                // (incorrección matemática grave si no lo son).
                assertTrue(codec.computeSyndromes(decoded.corrected).all { it == 0 })
                miscorrected++
            }
        }
        // La inmensa mayoría de los patrones de 3 errores debe detectarse como incorregible.
        assertTrue("detectados=$detected, mal corregidos=$miscorrected", detected >= 55)
    }

    @Test
    fun `mensaje totalmente aleatorio no se acepta como valido`() {
        val codec = rs()
        val rnd = Random(555)
        var rejected = 0
        repeat(30) {
            val received = IntArray(12) { rnd.nextInt(256) }
            val decoded = codec.decodeBlock(received)
            if (decoded.status != ReedSolomon.DecodeStatus.NO_ERROR) rejected++
            // Un mensaje aleatorio casi nunca es una palabra válida.
            if (decoded.status == ReedSolomon.DecodeStatus.CORRECTED) {
                assertTrue(codec.computeSyndromes(decoded.corrected).all { it == 0 })
            }
        }
        assertTrue(rejected >= 29)
    }

    // ---- 5. Distintas longitudes y configuraciones ----

    @Test
    fun `varias configuraciones corrigen hasta su capacidad`() {
        val configs = listOf(4 to 2, 8 to 4, 12 to 4, 16 to 8, 10 to 6)
        val rnd = Random(2024)
        for ((k, nsym) in configs) {
            val codec = ReedSolomon(k, nsym)
            val t = nsym / 2
            repeat(15) {
                val data = IntArray(k) { rnd.nextInt(256) }
                val codeword = codec.encodeBlock(data).codeword
                val positions = (codeword.indices).shuffled(rnd).take(t)
                val received = codeword.copyOf()
                for (p in positions) received[p] = (received[p] + 1 + rnd.nextInt(255)) % 256
                val decoded = codec.decodeBlock(received)
                assertEquals(
                    "config ($k,$nsym) iteración $it",
                    if (t == 0) ReedSolomon.DecodeStatus.NO_ERROR else ReedSolomon.DecodeStatus.CORRECTED,
                    if (t == 0) decoded.status else decoded.status
                )
                assertArrayEquals(data, decoded.data.take(k).toIntArray())
            }
        }
    }

    @Test
    fun `mensajes largos se dividen en bloques`() {
        val codec = rs() // k=8
        val text = "Hola Reed-Solomon"
        val symbols = MessageCodec.textToSymbols(text)
        assertEquals(17, symbols.size)
        val blocks = codec.encodeAll(symbols)
        assertEquals(3, blocks.size) // 8 + 8 + 1 (con relleno)
        assertEquals(7, blocks.last().paddedZeros)
        // Cada bloque es una palabra válida.
        for (b in blocks) assertTrue(codec.computeSyndromes(b.codeword).all { it == 0 })
    }

    @Test
    fun `traza completa recupera el texto con bloques y relleno`() {
        val codec = rs()
        val config = RSConfig(8, 4)
        val text = "Hola Reed-Solomon"
        val symbols = MessageCodec.textToSymbols(text)
        val trace = codec.buildEncodingTrace(
            symbols, config, text,
            text.map { c ->
                ReedSolomon.CharBreakdown(
                    c.toString(),
                    c.toString().toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }
                )
            },
            beginnerMode = true
        )
        assertTrue(trace.steps.isNotEmpty())
        // El formato copiable son solo números (36 = 3 bloques de 12).
        assertEquals(36, trace.shareable.trim().split(Regex("\\s+")).size)
        // Ida y vuelta: pegar solo números, deducir la configuración y decodificar.
        val parsed = MessageCodec.extractSymbolList(
            MessageCodec.parseSymbolList(trace.shareable, config)
        )!!
        assertNull(parsed.config)
        val detected = detectRsConfig(parsed.symbols, RSConfig(4, 2))
        assertEquals(config, detected)
        val decoding = codec.buildDecodingTrace(parsed.symbols, detected!!, 17, true)
        assertEquals(ReedSolomon.DecodeStatus.NO_ERROR, decoding.status)
        assertEquals(text, decoding.recoveredText)
    }

    // ---- 6. Binario ----

    @Test
    fun `mensaje binario se codifica y decodifica`() {
        val codec = rs()
        val config = RSConfig(8, 4)
        val r = MessageCodec.parseBinary("01001000 01101111 01101100 01100001")
        val symbols = (r as MessageCodec.ParseResult.Ok).symbols
        val trace = codec.buildEncodingTrace(symbols, config, null, emptyList(), false)
        val decoding = codec.buildDecodingTrace(trace.codeword, config, symbols.size, false)
        assertEquals(ReedSolomon.DecodeStatus.NO_ERROR, decoding.status)
        assertEquals("Hola", decoding.recoveredText)
        assertEquals(
            "01001000 01101111 01101100 01100001",
            MessageCodec.symbolsToBinary(decoding.recoveredSymbols)
        )
    }

    // ---- 7. Casos borde ----

    @Test
    fun `mensaje de un solo simbolo`() {
        val codec = rs()
        val data = intArrayOf(65)
        val blocks = codec.encodeAll(data)
        assertEquals(1, blocks.size)
        val decoding = codec.buildDecodingTrace(blocks[0].codeword, RSConfig(), 1, true)
        assertEquals("A", decoding.recoveredText)
    }

    @Test
    fun `mensaje de todo ceros`() {
        val codec = rs()
        val data = IntArray(8)
        val codeword = codec.encodeBlock(data).codeword
        assertTrue(codec.computeSyndromes(codeword).all { it == 0 })
        val decoded = codec.decodeBlock(corrupt(codeword, 5 to 99))
        assertEquals(ReedSolomon.DecodeStatus.CORRECTED, decoded.status)
        assertArrayEquals(data, decoded.data)
    }

    @Test
    fun `mensaje de todo 255`() {
        val codec = rs()
        val data = IntArray(8) { 255 }
        val codeword = codec.encodeBlock(data).codeword
        val decoded = codec.decodeBlock(corrupt(codeword, 2 to 0, 9 to 7))
        assertEquals(ReedSolomon.DecodeStatus.CORRECTED, decoded.status)
        assertArrayEquals(data, decoded.data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bloque con tamano incorrecto se rechaza`() {
        rs().encodeBlock(intArrayOf(1, 2, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decodificar bloque con tamano incorrecto se rechaza`() {
        rs().decodeBlock(intArrayOf(1, 2, 3))
    }

    // ---- 8/9. Validación de entrada del decodificador ----

    @Test
    fun `decodificar vacio devuelve mensaje de error en espanol`() {
        val trace = rs().buildDecodingTrace(IntArray(0), RSConfig(), null, true)
        assertEquals(ReedSolomon.DecodeStatus.TOO_MANY_ERRORS, trace.status)
        assertTrue(trace.errorMessage!!.contains("ningún símbolo"))
    }

    @Test
    fun `decodificar longitud no multiplo del bloque devuelve error`() {
        val trace = rs().buildDecodingTrace(IntArray(10) { it }, RSConfig(), null, true)
        assertEquals(ReedSolomon.DecodeStatus.TOO_MANY_ERRORS, trace.status)
        assertTrue(trace.errorMessage!!.contains("múltiplo"))
    }

    // ---- Los pasos mostrados son los cálculos reales ----

    @Test
    fun `los sindromes mostrados coinciden con los calculados`() {
        val codec = rs()
        val config = RSConfig(8, 4)
        val codeword = codec.encodeBlock(intArrayOf(72, 111, 108, 97, 32, 82, 83, 33)).codeword
        val received = corrupt(codeword, 2 to 250)
        val trace = codec.buildDecodingTrace(received, config, 8, false)
        val step = trace.steps.first { it.id.startsWith("dec-2-sindromes") }
        val real = codec.computeSyndromes(received)
        real.forEachIndexed { j, s ->
            assertEquals("S${j + 1} = R(α^$j) = $s", step.values[j])
        }
    }

    @Test
    fun `la posicion de error mostrada es la real`() {
        val codec = rs()
        val config = RSConfig(8, 4)
        val codeword = codec.encodeBlock(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)).codeword
        val received = corrupt(codeword, 5 to 200)
        val trace = codec.buildDecodingTrace(received, config, 8, true)
        assertEquals(ReedSolomon.DecodeStatus.CORRECTED, trace.status)
        assertEquals(listOf(5), trace.blocks[0].errorPositions)
        val chienStep = trace.steps.first { it.id.startsWith("dec-4-chien") }
        assertTrue(chienStep.explanation.contains("5"))
        val forneyStep = trace.steps.first { it.id.startsWith("dec-5-forney") }
        val detail = trace.blocks[0].forney.single()
        assertEquals(200, detail.received)
        assertEquals(codeword[5], detail.corrected)
        val forneyLine = forneyStep.values.single()
        assertTrue(forneyLine.contains("recibido 200"))
        assertTrue(
            forneyLine.contains("corregido = 200 ⊕ ${detail.magnitude} = ${codeword[5]}")
        )
    }

    @Test
    fun `simbolos ida y vuelta conservan el orden`() {
        val poly = GFPoly.fromSymbols(intArrayOf(1, 2, 3, 4, 5))
        assertArrayEquals(intArrayOf(1, 2, 3, 4, 5), poly.toSymbols(5))
        // Un resto de grado menor se rellena con ceros a la izquierda.
        assertArrayEquals(intArrayOf(0, 0, 1, 2), GFPoly(intArrayOf(2, 1)).toSymbols(4))
    }

    @Test
    fun `generador de 4 paridades coincide con el valor conocido`() {
        // g(x) = (x+1)(x+α)(x+α²)(x+α³) = x⁴ + 15x³ + 54x² + 120x + 64.
        val g = rs().generator
        assertArrayEquals(intArrayOf(1, 15, 54, 120, 64), g.toSymbols(5))
    }

    // ---- Detección automática de configuración (solo números, sin cabecera) ----

    @Test
    fun `detectar configuracion por defecto desde solo numeros`() {
        val codec = rs()
        val codeword = codec.encodeBlock(intArrayOf(72, 111, 108, 97, 32, 82, 83, 33)).codeword
        assertEquals(RSConfig(8, 4), detectRsConfig(codeword, RSConfig()))
    }

    @Test
    fun `detectar configuracion no estandar desde solo numeros`() {
        val config = RSConfig(12, 4)
        val codec = ReedSolomon(12, 4)
        val codeword = codec.encodeBlock(IntArray(12) { it + 1 }).codeword
        assertEquals(config, detectRsConfig(codeword, RSConfig()))
    }

    @Test
    fun `detectar devuelve null para numeros aleatorios`() {
        val rnd = Random(777)
        val garbage = IntArray(24) { rnd.nextInt(256) }
        assertEquals(null, detectRsConfig(garbage, RSConfig()))
    }

    @Test
    fun `el generador mostrado es producto real de raices`() {
        val codec = rs()
        val expected = GFPoly.fromRoots(IntArray(4) { i -> com.example.reedsolomonlab.domain.reedsolomon.GF256.alphaPower(i) })
        assertEquals(expected, codec.generator)
        val lines = codec.buildGeneratorTrace().intermediate
        assertEquals(5, lines.size)
        assertTrue(lines.last().contains(codec.generator.toDisplayString()))
    }
}
