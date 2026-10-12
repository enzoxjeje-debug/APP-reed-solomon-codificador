# RS-COM2026 🛰

Laboratorio educativo e interactivo para aprender, codificar, decodificar, detectar y
corregir errores con el algoritmo **Reed-Solomon**. Aplicación Android nativa en **Kotlin**,
**Jetpack Compose** y **Material 3**, completamente en español.

> 📲 **¿Solo querés instalarla?** El APK listo está en la raíz: `RS-COM2026-v1.0.apk`
> (Android 8.0+, sin permisos especiales). Ver [sección 5](#5-apk-exportado-ubicación-instalación-y-compilación).

---

## 1. ¿Qué es Reed-Solomon?

Reed-Solomon (RS) es un **código corrector de errores** inventado en 1960 por Irving Reed y
Gustave Solomon. Su idea central es sencilla de enunciar: al mensaje original se le agregan
símbolos extra de **paridad** calculados matemáticamente, de modo que si parte del mensaje
se daña durante la transmisión o el almacenamiento, el receptor puede **reconstruir los datos
originales** sin pedir que se los envíen de nuevo.

Lo usás todos los días sin darte cuenta:

- **Códigos QR**: siguen leyéndose aunque estén parcialmente tapados o rayados.
- **CD y DVD**: suenan bien aunque el disco tenga polvo o rayones.
- **Satélites y sondas espaciales** (como la Voyager): envían fotos desde millones de
  kilómetros con una señal llena de ruido.
- **Discos RAID, memorias flash y transmisiones digitales** (TV satelital, 5G, Wi-Fi).

### La matemática en pocas palabras

- Se trabaja en el campo finito **GF(256)**: los bytes 0–255, donde sumar es un XOR y
  multiplicar se hace con tablas de logaritmos en base α = 2 (polinomio primitivo `0x11D`).
- **Codificación**: con `2t` símbolos de paridad se corrigen hasta `t` errores por bloque
  (**regla de oro**). El mensaje se codifica de forma *sistemática*: `datos ∥ paridad`.
- **Decodificación**: se calculan los **síndromes** `Sⱼ = R(αʲ)`; si son todos cero, no hay
  errores. Si no, el polinomio localizador Λ(x) se obtiene con **Berlekamp-Massey**, las
  posiciones de error con la **búsqueda de Chien**, las magnitudes con la fórmula de
  **Forney** (`Y = X·Ω(X⁻¹)/Λ′(X⁻¹)`) y al final todo se **verifica** con los síndromes.

---

## 2. ¿En qué se basa la app?

- **Motor matemático real, nada simulado**: cada número que ves en pantalla (síndromes,
  polinomios, posiciones de error, magnitudes) sale del motor de `domain/reedsolomon/`.
  La interfaz solo dibuja la traza que el motor genera al calcular.
- **Trazas educativas**: el motor produce una lista de `EducationalStep`
  (título, explicación sencilla, fórmula, valores y grupos de símbolos) que la UI
  muestra paso a paso.
- **Decodificador honesto**: si hay más errores de los que la paridad permite corregir,
  la app lo dice en lugar de inventar datos.
- **Stack 100 % nativo**: Kotlin + Jetpack Compose + Material 3, tema oscuro neón,
  sin red, sin permisos especiales y sin dependencias nativas. Los sonidos se generan
  por síntesis (`AudioTrack`, sin archivos de audio) y se pueden silenciar.

---

## 3. ¿Para qué sirve?

1. **CODIFICAR**: escribís texto o binario, configurás el código RS(k, n) y observás la
   codificación paso a paso con los cálculos reales (conversión, símbolos, parámetros,
   división de polinomios, paridad y palabra final). Copiás el resultado o lo enviás
   directo al decodificador.
2. **DECODIFICAR**: pegás solo los números del mensaje codificado (la app deduce sola la
   configuración), simulás errores tocando símbolos (o al azar con el botón aleatorio) y
   observás la decodificación real: síndromes → Berlekamp-Massey → Chien → Forney →
   corrección → verificación.
3. **MÁS INFO**: teoría en español sencillo, diagrama del proceso, aplicaciones reales
   (QR, CD, satélites…), glosario y nota técnica.

Es una herramienta de **estudio de comunicaciones**: sirve para entender qué hace cada
etapa del algoritmo, experimentar cuántos errores aguanta cada configuración y comprobar
con números reales por qué con `2t` símbolos de paridad se corrigen `t` errores.

---

## 4. ¿Cómo está estructurada la app?

Proyecto Gradle con un único módulo `:app` (23 archivos Kotlin, ~5100 líneas, 56 tests
unitarios que pasan: 21 de codec + 8 de GF(256) + 27 de Reed-Solomon).

```text
com.example.reedsolomonlab
├── MainActivity.kt / ReedSolomonApp.kt   # Actividad + navegación (3 pestañas)
├── data/                                 # Modelos y conversión de mensajes
│   ├── RSConfig.kt                       # Configuración del código RS(k, n)
│   ├── MessageCodec.kt                   # Texto/binario ↔ símbolos; formato copiable
│   └── EducationalStep.kt                # Pasos que el motor genera para la UI
├── domain/reedsolomon/                   # Motor matemático REAL
│   ├── GF256.kt                          # Aritmética de GF(256) con tablas exp/log
│   ├── GFPoly.kt                         # Polinomios: +, ×, ÷, resto, Horner, derivada
│   └── ReedSolomon.kt                    # Codificador/decodificador + trazas educativas
├── ui/
│   ├── theme/                            # Tema oscuro neón: Color.kt, Theme.kt, Type.kt
│   ├── components/                       # NeonComponents.kt, SymbolComponents.kt
│   ├── encoding/EncodingScreen.kt        # Pestaña CODIFICAR
│   ├── decoding/DecodingScreen.kt        # Pestaña DECODIFICAR
│   └── info/InfoScreen.kt                # Pestaña MÁS INFO
├── viewmodel/                            # AppViewModel, EncodingViewModel, DecodingViewModel
└── audio/SoundManager.kt                 # Sonidos sintetizados (sin assets)
```

---

## 5. APK exportado: ubicación, instalación y compilación

### 📲 APK listo para instalar ✅

El APK está copiado en la **raíz del repositorio** para búsqueda e instalación rápida:

```text
RS-COM2026-v1.0.apk   (~17 MB, v1.0, com.example.reedsolomonlab)
```

Copia idéntica del APK de depuración generado por Gradle en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Requiere **Android 8.0 (API 26) o superior**. No pide permisos especiales.

### Instalarlo en el celular (2 opciones)

**Opción A — por USB con adb** (depuración USB activada en el móvil):

```bash
adb install -r RS-COM2026-v1.0.apk
```

**Opción B — copiando el archivo**: pasá `RS-COM2026-v1.0.apk` al celular (USB, Drive, WhatsApp…),
abrilo desde el administrador de archivos y aceptá “instalar apps desconocidas” cuando
Android lo pida.

### Compilarlo desde cero

Requisitos: **JDK 17** y **Android SDK** (platform 34 + build-tools 34).

```bash
# 1. Tests unitarios (motor matemático + validaciones) — 56/56 ✅
./gradlew :app:testDebugUnitTest          # en Windows: gradlew.bat :app:testDebugUnitTest

# 2. Generar el APK de depuración
./gradlew :app:assembleDebug
# El APK sale en: app/build/outputs/apk/debug/app-debug.apk

# 3. Instalar en el móvil (depuración USB activada)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Con **Android Studio**: *File → Open* sobre esta carpeta, esperá la sincronización de
Gradle y pulsá ▶ *Run* (o *Build → Build APK(s)* para generarlo sin instalar).

> El APK de depuración (`debug`) está firmado automáticamente con la clave de debug,
> así que se instala directo. Un APK `release` para publicar requeriría firma propia
> y no está incluido en este repositorio.

---

## Notas

- Mínimo Android 8.0 (API 26). Sin permisos especiales, sin red, sin dependencias nativas.
- Sonidos generados por síntesis (`AudioTrack`), desactivables con 🔊.
- Explicaciones en lenguaje sencillo con detalle matemático completo.

## Créditos

App realizada por:

- Gutierrez Enzo Leonel
- Recalde Lisandro Facundo
- Aylan Mauricio Gaston
