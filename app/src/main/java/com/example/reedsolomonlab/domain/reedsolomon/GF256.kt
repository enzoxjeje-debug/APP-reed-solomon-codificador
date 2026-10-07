package com.example.reedsolomonlab.domain.reedsolomon

/**
 * Aritmética del campo finito GF(2^8) = GF(256).
 *
 * Polinomio primitivo: x⁸ + x⁴ + x³ + x² + 1  (0x11D).
 * Elemento primitivo:  α = 2.
 *
 * La suma y la resta son el XOR bit a bit. La multiplicación, división
 * e inverso usan las tablas de logaritmos y exponenciales en base α,
 * lo cual es matemáticamente exacto para este campo.
 */
object GF256 {

    const val PRIMITIVE_POLY = 0x11D
    private const val SIZE_MINUS_ONE = 255

    /** exp[i] = α^i (extendida a 512 para evitar módulos en la multiplicación). */
    val exp = IntArray(512)

    /** log[v] = i tal que α^i = v. log[0] no está definido. */
    val log = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor PRIMITIVE_POLY
        }
        for (i in 255 until 512) exp[i] = exp[i - 255]
    }

    /** α^e, con e reducido módulo 255 (α^255 = 1). */
    fun alphaPower(e: Int): Int {
        var r = e % SIZE_MINUS_ONE
        if (r < 0) r += SIZE_MINUS_ONE
        return exp[r]
    }

    fun add(a: Int, b: Int): Int = a xor b

    fun sub(a: Int, b: Int): Int = a xor b

    fun mul(a: Int, b: Int): Int =
        if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

    fun div(a: Int, b: Int): Int {
        require(b != 0) { "División entre cero en GF(256)" }
        if (a == 0) return 0
        return exp[log[a] - log[b] + SIZE_MINUS_ONE]
    }

    fun inv(a: Int): Int {
        require(a != 0) { "El cero no tiene inverso multiplicativo en GF(256)" }
        return exp[SIZE_MINUS_ONE - log[a]]
    }

    fun pow(base: Int, exponent: Int): Int {
        if (base == 0) return if (exponent == 0) 1 else 0
        var e = exponent % SIZE_MINUS_ONE
        if (e < 0) e += SIZE_MINUS_ONE
        // Se usa Long para la multiplicación de logaritmos sin desbordar.
        val r = (log[base].toLong() * e.toLong()) % SIZE_MINUS_ONE
        return exp[r.toInt()]
    }
}
