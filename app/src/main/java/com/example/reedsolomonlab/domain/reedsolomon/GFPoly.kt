package com.example.reedsolomonlab.domain.reedsolomon

/**
 * Polinomio con coeficientes en GF(256).
 *
 * Convención: [coefs][i] es el coeficiente de x^i (índice = grado).
 * Ejemplo: el polinomio 3x² + x + 7 es GFPoly(intArrayOf(7, 1, 3)).
 *
 * Todas las operaciones (suma, producto, división, resto, evaluación)
 * usan la aritmética real del campo finito.
 */
class GFPoly(coeffs: IntArray) {

    /** Coeficientes normalizados (sin ceros de mayor grado). */
    val coeffs: IntArray

    init {
        var last = coeffs.size - 1
        while (last > 0 && coeffs[last] == 0) last--
        this.coeffs = coeffs.copyOf(last + 1)
    }

    val degree: Int get() = coeffs.size - 1

    val isZero: Boolean get() = coeffs.size == 1 && coeffs[0] == 0

    val leading: Int get() = coeffs[degree]

    operator fun plus(other: GFPoly): GFPoly {
        val out = IntArray(maxOf(coeffs.size, other.coeffs.size))
        for (i in out.indices) {
            val a = if (i < coeffs.size) coeffs[i] else 0
            val b = if (i < other.coeffs.size) other.coeffs[i] else 0
            out[i] = a xor b
        }
        return GFPoly(out)
    }

    operator fun times(other: GFPoly): GFPoly {
        if (isZero || other.isZero) return ZERO
        val out = IntArray(coeffs.size + other.coeffs.size - 1)
        for (i in coeffs.indices) {
            if (coeffs[i] == 0) continue
            for (j in other.coeffs.indices) {
                if (other.coeffs[j] == 0) continue
                out[i + j] = out[i + j] xor GF256.mul(coeffs[i], other.coeffs[j])
            }
        }
        return GFPoly(out)
    }

    fun timesScalar(s: Int): GFPoly {
        if (s == 0 || isZero) return ZERO
        if (s == 1) return this
        return GFPoly(IntArray(coeffs.size) { GF256.mul(coeffs[it], s) })
    }

    /** Multiplica por x^n (desplaza los coeficientes). */
    fun shiftLeft(n: Int): GFPoly {
        if (isZero || n <= 0) return this
        val out = IntArray(coeffs.size + n)
        coeffs.copyInto(out, n)
        return GFPoly(out)
    }

    /** Evaluación por el método de Horner: p(x). */
    fun evaluate(x: Int): Int {
        var result = 0
        for (i in coeffs.indices.reversed()) {
            result = GF256.mul(result, x) xor coeffs[i]
        }
        return result
    }

    /**
     * Derivada formal. En característica 2 solo sobreviven los términos
     * de grado impar: d/dx(x^i) = i·x^(i-1), y i es 0 o 1 módulo 2.
     */
    fun derivative(): GFPoly {
        if (coeffs.size <= 1) return ZERO
        val out = IntArray(coeffs.size - 1)
        for (i in 1 until coeffs.size) {
            if (i % 2 == 1) out[i - 1] = coeffs[i]
        }
        return GFPoly(out)
    }

    /**
     * División larga de polinomios. Devuelve (cociente, resto)
     * tales que this = divisor·cociente + resto.
     */
    fun divMod(divisor: GFPoly): Pair<GFPoly, GFPoly> {
        require(!divisor.isZero) { "División entre el polinomio cero" }
        if (degree < divisor.degree) return ZERO to this
        val rem = coeffs.copyOf()
        val qSize = degree - divisor.degree + 1
        val quot = IntArray(qSize)
        val dLeadInv = GF256.inv(divisor.leading)
        for (i in qSize - 1 downTo 0) {
            val idx = i + divisor.degree
            val factor = GF256.mul(rem[idx], dLeadInv)
            quot[i] = factor
            if (factor != 0) {
                for (j in 0..divisor.degree) {
                    rem[i + j] = rem[i + j] xor GF256.mul(factor, divisor.coeffs[j])
                }
            }
        }
        return GFPoly(quot) to GFPoly(rem.copyOf(divisor.degree))
    }

    fun mod(divisor: GFPoly): GFPoly = divMod(divisor).second

    /**
     * Devuelve los coeficientes como símbolos ordenados de mayor a menor
     * grado: out[0] es el coeficiente de x^(length-1). Si el polinomio tiene
     * grado menor, se rellena con ceros a la izquierda.
     */
    fun toSymbols(length: Int): IntArray {
        require(length >= 0)
        val out = IntArray(length)
        for (k in 0 until length) {
            val deg = length - 1 - k
            out[k] = if (deg < coeffs.size) coeffs[deg] else 0
        }
        return out
    }

    /** Representación legible, p. ej. «x³ + α²·x + 7». */
    fun toDisplayString(): String {
        if (isZero) return "0"
        val sb = StringBuilder()
        for (i in coeffs.indices.reversed()) {
            val c = coeffs[i]
            if (c == 0) continue
            if (sb.isNotEmpty()) sb.append(" + ")
            val coefText = when {
                i == 0 -> c.toString()
                c == 1 -> ""
                else -> coefToText(c) + "·"
            }
            sb.append(
                when (i) {
                    0 -> coefText
                    1 -> "${coefText}x"
                    else -> "${coefText}x^$i"
                }
            )
        }
        return sb.toString()
    }

    private fun coefToText(c: Int): String {
        if (c == 0) return "0"
        val p = GF256.log[c]
        return if (p == 0) "1" else "α^$p"
    }

    override fun equals(other: Any?): Boolean =
        other is GFPoly && coeffs.contentEquals(other.coeffs)

    override fun hashCode(): Int = coeffs.contentHashCode()

    override fun toString(): String = toDisplayString()

    companion object {
        val ZERO = GFPoly(intArrayOf(0))
        val ONE = GFPoly(intArrayOf(1))

        fun of(vararg c: Int): GFPoly = GFPoly(c.copyOf())

        /**
         * Construye el polinomio a partir de símbolos, siendo
         * symbols[0] el coeficiente de mayor grado.
         */
        fun fromSymbols(symbols: IntArray): GFPoly {
            val c = IntArray(symbols.size)
            for (i in symbols.indices) c[i] = symbols[symbols.size - 1 - i]
            return GFPoly(c)
        }

        /** Producto de (x + r) para cada raíz r. */
        fun fromRoots(roots: IntArray): GFPoly {
            var g = ONE
            for (r in roots) {
                g = g * GFPoly(intArrayOf(r, 1))
            }
            return g
        }
    }
}
