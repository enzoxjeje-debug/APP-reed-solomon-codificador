package com.example.reedsolomonlab

import com.example.reedsolomonlab.domain.reedsolomon.GF256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Axiomas y propiedades del campo finito GF(256). */
class GF256Test {

    @Test
    fun `tabla exponencial cubre todo el campo`() {
        val seen = mutableSetOf<Int>()
        for (i in 0 until 255) {
            val v = GF256.exp[i]
            assertTrue("α^$i = $v fuera de rango", v in 1..255)
            seen.add(v)
        }
        assertEquals("α debe generar los 255 elementos no nulos", 255, seen.size)
    }

    @Test
    fun `alpha elevado a 255 es 1`() {
        assertEquals(1, GF256.alphaPower(255))
        assertEquals(1, GF256.alphaPower(0))
        assertEquals(2, GF256.alphaPower(1))
    }

    @Test
    fun `suma es XOR y todo elemento es su propio opuesto`() {
        val rnd = Random(7)
        repeat(200) {
            val a = rnd.nextInt(256)
            val b = rnd.nextInt(256)
            assertEquals(a xor b, GF256.add(a, b))
            assertEquals(0, GF256.add(a, a))
        }
    }

    @Test
    fun `multiplicacion por cero e identidad`() {
        val rnd = Random(11)
        repeat(200) {
            val a = rnd.nextInt(256)
            assertEquals(0, GF256.mul(a, 0))
            assertEquals(0, GF256.mul(0, a))
            assertEquals(a, GF256.mul(a, 1))
        }
    }

    @Test
    fun `multiplicacion coincide con suma de logaritmos`() {
        val rnd = Random(13)
        repeat(300) {
            val a = rnd.nextInt(1, 256)
            val b = rnd.nextInt(1, 256)
            val expected = GF256.exp[(GF256.log[a] + GF256.log[b]) % 255]
            assertEquals("mul($a, $b)", expected, GF256.mul(a, b))
        }
    }

    @Test
    fun `division e inverso son coherentes`() {
        val rnd = Random(17)
        repeat(300) {
            val a = rnd.nextInt(256)
            val b = rnd.nextInt(1, 256)
            val q = GF256.div(a, b)
            assertEquals("($a / $b) * $b debe ser $a", a, GF256.mul(q, b))
            assertEquals(1, GF256.mul(b, GF256.inv(b)))
        }
    }

    @Test
    fun `potencia es multiplicacion repetida`() {
        val rnd = Random(19)
        repeat(100) {
            val a = rnd.nextInt(256)
            val e = rnd.nextInt(0, 20)
            var acc = 1
            repeat(e) { acc = GF256.mul(acc, a) }
            assertEquals("pow($a, $e)", acc, GF256.pow(a, e))
        }
    }

    @Test
    fun `propiedad distributiva`() {
        val rnd = Random(23)
        repeat(200) {
            val a = rnd.nextInt(256)
            val b = rnd.nextInt(256)
            val c = rnd.nextInt(256)
            assertEquals(
                GF256.mul(a, GF256.add(b, c)),
                GF256.add(GF256.mul(a, b), GF256.mul(a, c))
            )
        }
    }
}
