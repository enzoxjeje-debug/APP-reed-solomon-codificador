package com.example.reedsolomonlab

import com.example.reedsolomonlab.data.MessageCodec
import com.example.reedsolomonlab.data.RSConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validación de entradas: texto, binario y listas de símbolos. */
class CodecTest {

    // ---- Texto ----

    @Test
    fun `texto hola mundo a simbolos`() {
        val symbols = MessageCodec.textToSymbols("Hola mundo")
        assertArrayEquals(
            intArrayOf(72, 111, 108, 97, 32, 109, 117, 110, 100, 111),
            symbols
        )
    }

    @Test
    fun `simbolos a texto inverso`() {
        val symbols = intArrayOf(72, 111, 108, 97, 32, 109, 117, 110, 100, 111)
        assertEquals("Hola mundo", MessageCodec.symbolsToText(symbols))
    }

    @Test
    fun `texto vacio da cero simbolos`() {
        assertEquals(0, MessageCodec.textToSymbols("").size)
    }

    // ---- Binario ----

    @Test
    fun `binario con espacios se interpreta byte a byte`() {
        val r = MessageCodec.parseBinary("01001000 01101111 01101100 01100001")
        assertTrue(r is MessageCodec.ParseResult.Ok)
        assertArrayEquals(intArrayOf(72, 111, 108, 97), (r as MessageCodec.ParseResult.Ok).symbols)
    }

    @Test
    fun `binario continuo multiplo de 8`() {
        val r = MessageCodec.parseBinary("0100100001101111")
        assertTrue(r is MessageCodec.ParseResult.Ok)
        assertArrayEquals(intArrayOf(72, 111), (r as MessageCodec.ParseResult.Ok).symbols)
    }

    @Test
    fun `binario rechaza caracteres invalidos`() {
        val r = MessageCodec.parseBinary("01001002 0110111X")
        assertTrue(r is MessageCodec.ParseResult.Err)
        assertTrue((r as MessageCodec.ParseResult.Err).message.contains("0 y 1"))
    }

    @Test
    fun `binario rechaza grupos que no son de 8 bits`() {
        val r = MessageCodec.parseBinary("0100100 01101111")
        assertTrue(r is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `binario rechaza tira no multiplo de 8`() {
        val r = MessageCodec.parseBinary("0100100001")
        assertTrue(r is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `binario rechaza entrada vacia`() {
        assertTrue(MessageCodec.parseBinary("   ") is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `simbolos a binario`() {
        assertEquals("01001000 01101111", MessageCodec.symbolsToBinary(intArrayOf(72, 111)))
    }

    // ---- Listas de símbolos ----

    @Test
    fun `lista de simbolos con espacios y comas`() {
        val r = MessageCodec.parseSymbolList("72 111, 108; 97", RSConfig())
        val list = MessageCodec.extractSymbolList(r)
        assertNotNull(list)
        assertArrayEquals(intArrayOf(72, 111, 108, 97), list!!.symbols)
        assertNull(list.config)
    }

    @Test
    fun `lista de simbolos rechaza texto`() {
        val r = MessageCodec.parseSymbolList("Hola mundo", RSConfig())
        assertTrue(r is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `lista de simbolos rechaza valores fuera de rango`() {
        val neg = MessageCodec.parseSymbolList("72 -1 108", RSConfig())
        assertTrue(neg is MessageCodec.ParseResult.Err)
        val big = MessageCodec.parseSymbolList("72 256 108", RSConfig())
        assertTrue(big is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `mensaje copiable es solo numeros sin cabecera`() {
        val config = RSConfig(8, 4)
        val shareable = MessageCodec.toShareableString(intArrayOf(72, 111, 1, 2), config, 2)
        assertEquals("72 111 1 2", shareable)
        val r = MessageCodec.parseSymbolList(shareable, RSConfig())
        val list = MessageCodec.extractSymbolList(r)
        assertNotNull(list)
        assertNull(list!!.config)
        assertArrayEquals(intArrayOf(72, 111, 1, 2), list.symbols)
    }

    @Test
    fun `cabecera antigua RS1 sigue aceptandose por compatibilidad`() {
        val r = MessageCodec.parseSymbolList("RS1 k=8 nsym=4 len=2 | 72 111 1 2", RSConfig())
        val list = MessageCodec.extractSymbolList(r)
        assertNotNull(list)
        assertEquals(8, list!!.config!!.dataSymbols)
        assertEquals(4, list.config!!.paritySymbols)
        assertEquals(2, list.dataLength)
        assertArrayEquals(intArrayOf(72, 111, 1, 2), list.symbols)
    }

    @Test
    fun `cabecera con configuracion invalida se rechaza`() {
        val r = MessageCodec.parseSymbolList("RS1 k=8 nsym=3 | 1 2 3", RSConfig())
        assertTrue(r is MessageCodec.ParseResult.Err)
    }

    @Test
    fun `detector de listas de simbolos`() {
        assertTrue(MessageCodec.looksLikeSymbolList("72 111 108 97"))
        assertTrue(MessageCodec.looksLikeSymbolList("RS1 k=8 nsym=4 | 1 2"))
        assertFalse(MessageCodec.looksLikeSymbolList("Hola mundo"))
        assertFalse(MessageCodec.looksLikeSymbolList(""))
    }

    // ---- Configuración ----

    @Test
    fun `configuracion por defecto 8 mas 4 corrige 2`() {
        val c = RSConfig()
        assertEquals(8, c.dataSymbols)
        assertEquals(4, c.paritySymbols)
        assertEquals(2, c.correctionCapacity)
        assertNull(c.validationError())
    }

    @Test
    fun `configuracion rechaza paridad impar`() {
        assertNotNull(RSConfig(8, 3).validationError())
    }

    @Test
    fun `configuracion rechaza bloque mayor de 255`() {
        assertNotNull(RSConfig(250, 6).validationError())
    }

    @Test
    fun `configuracion rechaza datos cero`() {
        assertNotNull(RSConfig(0, 4).validationError())
    }
}
