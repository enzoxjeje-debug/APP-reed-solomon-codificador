# 📘 Cómo funciona RS-COM2026 — Codificador, Decodificador y Corrección de Errores

Documento complementario a `README.md`: explica **qué trabajo hace** el codificador
y el decodificador, **cómo encuentra los errores** cuando el mensaje llega dañado y
**con qué lenguaje y programa** se codificó y compiló el APK.

---

## 1. El codificador: agrega protección al mensaje

El codificador toma tu texto (o números) y produce un mensaje "blindado". Estos son
sus trabajos, en orden real de ejecución (`ReedSolomon.kt`):

### 1.1 Conversión a símbolos
- Si escribiste texto, cada carácter se convierte a bytes **UTF-8**: la «H» es el 72.
- Cada byte (0–255) es un **símbolo** del campo finito **GF(256)**. Reed-Solomon
  trabaja con símbolos enteros, no con letras ni bits.

### 1.2 División en bloques
- Tus símbolos se cortan en bloques de `k` datos. Si el último bloque no se llena,
  se **rellena con ceros** (`encodeAll()`).

### 1.3 Construcción del polinomio generador
- `g(x) = (x − α⁰)(x − α¹)…(x − α^(2t−1))`, una multiplicación de `2t` factores
  simples sobre GF(256) (`buildGeneratorTrace()`), donde `t = paridad/2`.

### 1.4 Cálculo de la paridad (el corazón del codificador)
- Se forma el polinomio del mensaje `m(x) = D(x)·x^(2t)` (desplazamiento a la izquierda).
- Se divide entre `g(x)` con **división larga de polinomios sobre GF(256)**
  (`encodeBlock()` → `GF256.mul/div/inv`). **El resto de esa división es la paridad.**
- El resultado final es la palabra **sistemática** `c(x) = datos ∥ paridad`:
  tus datos van al principio, la paridad al final. Todo es calculado de verdad,
  sin valores simulados.

### 1.5 Salida
- La app muestra la palabra final en decimal y hexadecimal, y genera un string
  **copiable** (`MessageCodec.toShareableString`) que pegás en la pestaña DECODIFICAR.

---

## 2. El decodificador: comprueba, localiza y corrige

El decodificador recibe la palabra `r` (que puede estar dañada) y hace este trabajo
por bloque (`decodeBlock()` en `ReedSolomon.kt`):

1. **Recepción y validación**: comprueba que la longitud sea múltiplo del tamaño
   de bloque; además, la app **deduce sola** la configuración RS(k, n) desde el
   mensaje pegado si no la conocés (`DecodingViewModel.detectRsConfig()`).
2. **Síndromes**: calcula `Sⱼ = R(αʲ)` para `j = 0…2t−1`.
   - Si **todos son cero** → el mensaje está intacto: toma los `k` primeros
     símbolos como datos y termina.
   - Si **no** → hay errores y continúa (sección 3).
3. **Separación y mensaje recuperado**: cuando es válido, descarta la paridad,
   convierte los símbolos de vuelta a texto y muestra el mensaje original.
4. **Honestidad**: si el daño supera la capacidad, declara
   `TOO_MANY_ERRORS` ("no es posible corregir") en vez de inventar datos.

---

## 3. ¿Cómo encuentra el error cuando el código está mal?

Cuando algún síndrome es distinto de cero, el decodificador ejecuta un pipeline
de 4 etapas con matemática **real** sobre GF(256):

| Etapa | Algoritmo | Pregunta que responde | Archivo |
|-------|-----------|----------------------|---------|
| 1 | **Síndromes** `Sⱼ = R(αʲ)` | ¿Llegó dañado? | `computeSyndromes()` |
| 2 | **Berlekamp-Massey** | ¿Un polinomio Λ(x) cuya estructura "explica" los síndromes? | `berlekampMassey()` |
| 3 | **Búsqueda de Chien** | ¿DÓNDE están los errores? | `chienSearch()` |
| 4 | **Fórmula de Forney** | ¿CUÁNTO cambió cada símbolo? | `errorEvaluator()` + `forneyMagnitudes()` |

### Paso a paso con valores reales

1. **Los síndromes son la "huella" del error.** Cada `Sⱼ` es el resultado de
   evaluar la palabra recibida en un punto distinto del campo: juntos codifican
   posición y magnitud del daño.
2. **Berlekamp-Massey** ajusta iterativamente el *polinomio localizador* `Λ(x)`:
   en cada iteración calcula la discrepancia `Δ`; si `Δ = 0` mantiene Λ(x),
   si no, lo corrige (y controla que el número de errores no supere `t`,
   la capacidad: con `2t` de paridad se corrigen hasta `t` errores).
3. **Chien busca las raíces de Λ(x)**: evalúa `Λ(α^(−m))` en cada posición
   del bloque. Cuando el resultado es `0`, ahí hay un símbolo dañado
   (la UI las marca en rojo; además las verifica contra el grado de Λ: si el
   número de raíces no coincide, declara que hay demasiados errores).
4. **Forney calcula la magnitud**: con el polinomio evaluador
   `Ω(x) = S(x)·Λ(x) mod x^(2t)` y la derivada `Λ′(x)`, aplica
   `Y = X·Ω(X⁻¹) / Λ′(X⁻¹)` (con `X = αᵐ`) para cada posición.
5. **Corrección**: `corregido = recibido ⊕ magnitud` (un XOR en GF(256)).
6. **Verificación final**: recalcula los síndromes de la palabra corregida.
   Si vuelven a ser cero, la corrección es válida; si no, el daño superaba
   la capacidad y se avisa al usuario.

> **¿Por qué funciona?** Toda palabra válida es múltiplo de `g(x)`, así que
> evaluarla en las raíces de `g(x)` (α⁰…α^(2t−1)) da cero. Un error de un solo
> símbolo rompe ese patrón («ruido»), y el álgebra del campo permite invertir
> el proceso: de la huella (síndromes) al culpable (posición) y al daño (magnitud).

---

## 4. ¿Con qué se codificó y compiló el APK?

- **Lenguaje: Kotlin** (todo el proyecto está escrito en Kotlin, un solo lenguaje).
- **Programa: Android Studio** (Kotlin Ladybug o superior) — ahí se escribió el código
  y se compila con *Build → Build APK(s)*.
- La compilación real la hace **Gradle** con el Plugin de Android, sobre
  **JDK 17 + Android SDK 34** (min SDK 26 = Android 8.0). También puede
  compilarse desde consola: `./gradlew :app:assembleDebug`.

Con eso es suficiente para reproducir el build de la app; el resto son librerías
de la interfaz (Jetpack Compose, Material 3, Navigation Compose) y los tests con
JUnit 4, todo gestionado por el propio Gradle.

---

## 5. Archivos clave, en un vistazo

```text
app/src/main/java/com/example/reedsolomonlab/
├── domain/reedsolomon/   ← EL MOTOR
│   ├── GF256.kt          Campo finito: suma=XOR, ×/÷ con tablas log/exp
│   ├── GFPoly.kt         Polinomios: +, ×, ÷, resto, Horner, derivada
│   └── ReedSolomon.kt    Generador, encodeBlock, síndromes, BM,
│                         Chien, Forney y verificación (+ trazas educativas)
├── data/RSConfig.kt      RS(k, n): paridad y capacidad de corrección
├── data/MessageCodec.kt  Texto ↔ símbolos y formato copiable
└── viewmodel/            Conecta el motor con las pantallas Compose
```

*Versión 1.0 · Museo educativo del código Reed-Solomon.*
