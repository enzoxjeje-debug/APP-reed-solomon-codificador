import * as React from "react";
import { BookOpenText, Code2, Download, GraduationCap, Hammer, Satellite, Smartphone, Wrench } from "lucide-react";
import AstroBubbleHero from "./components/ui/astro-bubble-hero";
import DecodeLab from "./components/DecodeLab";
import EncodeLab from "./components/EncodeLab";
import { Reveal } from "./components/parts";
import { buildGenerator, encodeBlock, textToSymbols } from "./rs/engine";

const scrollTo = (id: string) =>
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });

const NAV = [
  { label: "El proyecto", target: "proyecto" },
  { label: "Codificar", target: "codificar" },
  { label: "Decodificar", target: "decodificar" },
  { label: "Herramientas", target: "herramientas" },
  { label: "Descargar", target: "descargar" },
];

const TOOLS = [
  { icon: Code2, name: "Kotlin", desc: "El único lenguaje del proyecto: motor, UI y tests." },
  { icon: Hammer, name: "Android Studio", desc: "IDE donde se escribió el código y se compiló el APK." },
  { icon: Wrench, name: "Gradle + JDK 17 + SDK 34", desc: "Build system real: ./gradlew :app:assembleDebug." },
  { icon: Smartphone, name: "Jetpack Compose + Material 3", desc: "Interfaz nativa con tema oscuro neón, 3 pestañas." },
];

export default function App() {
  const [text, setText] = React.useState("HOLA");
  const [k, setK] = React.useState(8);
  const [nsym, setNsym] = React.useState(6);

  const codeword = React.useMemo(() => {
    const { symbols } = textToSymbols(text || " ");
    const data = symbols.slice(0, k);
    const padded = [...data, ...new Array(Math.max(0, k - data.length)).fill(0)];
    return encodeBlock(padded, nsym, buildGenerator(nsym)).codeword;
  }, [text, k, nsym]);

  return (
    <div className="rs-page">
      {/* Nav */}
      <nav className="sticky top-0 z-40 border-b border-[var(--rs-line)] bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-[1240px] flex-wrap items-center gap-x-6 gap-y-1 px-4 py-3 sm:px-8">
          <span className="font-extrabold tracking-tight">RS-COM2026 🛰</span>
          {NAV.map((n) => (
            <button
              key={n.target}
              type="button"
              onClick={() => scrollTo(n.target)}
              className="rs-mono text-[var(--rs-muted)] transition-colors hover:text-[var(--rs-accent)]"
            >
              {n.label}
            </button>
          ))}
        </div>
      </nav>

      {/* Hero */}
      <AstroBubbleHero
        frame={false}
        palette="cobalt"
        defaultTheme="light"
        height="92svh"
        brand="RS-COM2026"
        wordmark="RS-COM"
        issue="v1.0"
        title={"RS-COM2026\nLABORATORIO RS"}
        blurb="Laboratorio educativo para codificar, decodificar y corregir errores con Reed-Solomon de verdad: cada número de esta página sale de un motor GF(256) calculado en vivo, igual que en la app Android."
        credit="RS-COM2026 · Kotlin + Jetpack Compose · APK en la raíz del repo"
        primaryCta="Descargar APK"
        onPrimary={() => scrollTo("descargar")}
        secondaryCta="Ver cómo funciona"
        onSecondary={() => scrollTo("codificar")}
        hint=""
      />

      {/* Misión */}
      <div className="aa-in">
        <Reveal id="proyecto">
          <section className="aa-sec aa-pad">
            <p className="rs-kicker">El proyecto</p>
            <div className="mt-6 grid gap-4 md:grid-cols-3">
              <div className="rs-card flex gap-4 p-5">
                <BookOpenText size={26} className="shrink-0 text-[var(--rs-accent)]" />
                <div>
                  <p className="font-extrabold">Qué es Reed-Solomon</p>
                  <p className="mt-1 text-[15px] text-[var(--rs-soft)]">
                    Un código corrector de errores publicado en 1960 por Irving Reed y
                    Gustave Solomon. En vez de pedir que le reenvíen el mensaje, agrega
                    redundancia matemática para que el receptor <b>repare el daño solo</b>:
                    con 2t símbolos de paridad se corrigen t errores por bloque.
                  </p>
                </div>
              </div>
              <div className="rs-card flex gap-4 p-5">
                <Satellite size={26} className="shrink-0 text-[var(--rs-accent)]" />
                <div>
                  <p className="font-extrabold">Dónde se usa</p>
                  <p className="mt-1 text-[15px] text-[var(--rs-soft)]">
                    Está en todas partes aunque no se vea: códigos QR, CD, DVD y Blu-ray,
                    las fotos del Voyager desde el espacio profundo, la TV satelital y los
                    discos SSD y arreglos RAID que cuidan tus archivos.
                  </p>
                </div>
              </div>
              <div className="rs-card flex gap-4 p-5">
                <GraduationCap size={26} className="shrink-0 text-[var(--rs-accent)]" />
                <div>
                  <p className="font-extrabold">Por qué lo construimos</p>
                  <p className="mt-1 text-[15px] text-[var(--rs-soft)]">
                    RS-COM2026 es nuestro laboratorio educativo: una app Android en español
                    que muestra cada etapa con números reales en vez de solo fórmulas. La
                    construimos para entender —y hacer entender— la matemática que protege
                    tus datos todos los días.
                  </p>
                </div>
              </div>
            </div>
          </section>
        </Reveal>

        {/* Config compartida */}
        <Reveal id="codificar">
          <section className="aa-sec aa-pad">
            <p className="rs-kicker">Paso a paso · 1/2</p>
            <h2 className="aa-h2 mt-3">El codificador</h2>
            <p className="aa-lede mt-3">
              Escribí un mensaje y elegí el código RS(n, k). Todo lo que ves — bytes,
              generador, paridad — se calcula en vivo con el mismo algoritmo de la app.
            </p>
            <div className="rs-card mt-6 flex flex-wrap items-end gap-4 p-5">
              <label className="flex flex-col gap-1.5">
                <span className="rs-mono text-[var(--rs-muted)]">mensaje</span>
                <input
                  className="rs-input w-44"
                  value={text}
                  maxLength={24}
                  onChange={(e) => setText(e.target.value.toUpperCase().slice(0, 24))}
                />
              </label>
              <label className="flex flex-col gap-1.5">
                <span className="rs-mono text-[var(--rs-muted)]">datos k</span>
                <select className="rs-input" value={k} onChange={(e) => setK(Number(e.target.value))}>
                  {[4, 6, 8, 10].map((v) => (
                    <option key={v} value={v}>
                      {v}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1.5">
                <span className="rs-mono text-[var(--rs-muted)]">paridad 2t</span>
                <select className="rs-input" value={nsym} onChange={(e) => setNsym(Number(e.target.value))}>
                  {[4, 6, 8].map((v) => (
                    <option key={v} value={v}>
                      {v} → corrige {v / 2}
                    </option>
                  ))}
                </select>
              </label>
              <span className="rs-chip ok">RS({k + nsym}, {k}) · t = {nsym / 2}</span>
            </div>
            <div className="mt-6">
              <EncodeLab text={text || " "} k={k} nsym={nsym} />
            </div>
          </section>
        </Reveal>

        <Reveal id="decodificar">
          <section className="aa-sec aa-pad">
            <p className="rs-kicker">Paso a paso · 2/2</p>
            <h2 className="aa-h2 mt-3">El decodificador</h2>
            <p className="aa-lede mt-3">
              La palabra codificada de arriba viaja al decodificador. Dañá símbolos y mirá
              cómo los síndromes delatan el error, Chien lo ubica y Forney lo repara.
            </p>
            <div className="mt-6">
              <DecodeLab key={`${codeword.join(",")}|${k}|${nsym}`} codeword={codeword} k={k} nsym={nsym} />
            </div>
          </section>
        </Reveal>

        {/* Herramientas */}
        <Reveal id="herramientas">
          <section className="aa-sec aa-pad">
            <p className="rs-kicker">Con qué se hizo</p>
            <h2 className="aa-h2 mt-3">Lenguaje y compilación</h2>
            <p className="aa-lede mt-3">
              La app se codificó íntegramente en <b>Kotlin</b> y se compiló con{" "}
              <b>Android Studio + Gradle</b> (JDK 17, SDK 34). El motor Reed-Solomon es
              100 % propio, sin librerías externas de matemática.
            </p>
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              {TOOLS.map((t) => (
                <div key={t.name} className="rs-card flex gap-4 p-5">
                  <t.icon size={26} className="shrink-0 text-[var(--rs-accent)]" />
                  <div>
                    <p className="font-extrabold">{t.name}</p>
                    <p className="mt-1 text-[15px] text-[var(--rs-soft)]">{t.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </Reveal>

        {/* Descarga */}
        <Reveal id="descargar">
          <section className="aa-sec aa-pad">
            <div className="rs-card flex flex-col items-start gap-5 p-6 sm:p-10">
              <p className="rs-kicker">Instalala en tu celular</p>
              <h2 className="aa-h2">RS-COM2026-v1.0.0.apk</h2>
              <p className="aa-lede">
                Android 8.0 o superior, sin permisos especiales. Descargala, copiá el
                archivo al teléfono y aceptá "instalar apps desconocidas".
              </p>
              <div className="flex flex-wrap gap-3">
                <a className="rs-btn rs-btn-solid" href="https://drive.google.com/file/d/17TfCcIQMc1T0kzBumL4I4MxbZalN5MLc/view?usp=sharing" target="_blank" rel="noopener noreferrer">
                  <Download size={15} /> Descargar APK · 17 MB
                </a>
                <button type="button" className="rs-btn rs-btn-line" onClick={() => scrollTo("codificar")}>
                  Repasar el funcionamiento
                </button>
              </div>
            </div>
            <footer className="mt-10 flex flex-wrap items-center justify-between gap-3 border-t-[1.5px] border-[var(--rs-ink)] pt-4">
              <span className="rs-mono text-[var(--rs-muted)]">
                RS-COM2026 · Gutierrez · Recalde · Aylan
              </span>
              <span className="rs-mono text-[var(--rs-muted)]">Kotlin · Reed-Solomon · GF(256)</span>
            </footer>
            <div className="h-6" />
          </section>
        </Reveal>
      </div>
    </div>
  );
}
