import * as React from "react";
import { buildGenerator, CAPACITY, encodeBlock, polyToString, textToSymbols } from "../rs/engine";
import { SlideDeck, SymChips, type Slide } from "./parts";

export default function EncodeLab({ text, k, nsym }: { text: string; k: number; nsym: number }) {
  const slides: Slide[] = React.useMemo(() => {
    const { chars, symbols } = textToSymbols(text || " ");
    const data = symbols.slice(0, k);
    const padded = [...data, ...new Array(Math.max(0, k - data.length)).fill(0)];
    const gen = buildGenerator(nsym);
    const enc = encodeBlock(padded, nsym, gen);
    const trimmed = symbols.length > k;
    const t = CAPACITY(nsym);
    return [
      {
        kicker: "Conversión del mensaje",
        title: "Del texto a los símbolos",
        body: (
          <>
            <p className="rs-lede text-[15px]">
              Tu texto «{text || " "}» se convierte a bytes UTF-8. Cada byte (0–255) es un
              símbolo del campo GF(256): Reed-Solomon protege símbolos, no letras.
            </p>
            <div className="flex flex-wrap gap-2">
              {chars.map((c, i) => (
                <span key={i} className="rs-chip">
                  {c.ch === " " ? "␣" : c.ch} → {c.bytes.join(" · ")}
                </span>
              ))}
            </div>
            <SymChips values={symbols} kinds={symbols.map(() => "data")} />
          </>
        ),
      },
      {
        kicker: "Bloque y relleno",
        title: `Código RS(${k + nsym}, ${k})`,
        body: (
          <>
            <p className="rs-lede text-[15px]">
              El mensaje se agrupa en bloques de {k} símbolos.
              {trimmed
                ? " La demo usa solo el primer bloque."
                : " El último bloque se rellena con ceros."}{" "}
              Con {nsym} de paridad se corrigen hasta <b>{t}</b> errores por bloque.
            </p>
            <SymChips values={padded} kinds={padded.map((_, i) => (i < data.length ? "data" : "normal"))} />
            <p className="rs-mono text-[var(--rs-muted)]">
              bloque = [{padded.join(", ")}] · datos {data.length} + relleno {padded.length - data.length}
            </p>
          </>
        ),
      },
      {
        kicker: "El generador",
        title: "La receta de la redundancia",
        body: (
          <>
            <p className="rs-lede text-[15px]">
              El polinomio generador es la "receta" de la redundancia:{" "}
              <i>
                g(x) = (x − α⁰)(x − α¹)…(x − α<sup>{nsym - 1}</sup>)
              </i>
              , calculado de verdad sobre GF(256).
            </p>
            <div className="rs-formula">g(x) = {polyToString(gen)}</div>
          </>
        ),
      },
      {
        kicker: "Paridad y palabra final",
        title: "El resto es la protección",
        body: (
          <>
            <p className="rs-lede text-[15px]">
              Se divide el mensaje desplazado entre g(x) con división larga.{" "}
              <b>El resto es la paridad.</b> La palabra válida es sistemática: datos ∥ paridad.
            </p>
            <div className="rs-formula">paridad = resto de [ D(x)·x^{nsym} ÷ g(x) ]</div>
            <p className="rs-mono text-[var(--rs-muted)]">paridad</p>
            <SymChips values={enc.parity} kinds={enc.parity.map(() => "parity")} />
            <p className="rs-mono text-[var(--rs-muted)]">palabra final (datos ∥ paridad)</p>
            <SymChips
              values={enc.codeword}
              kinds={enc.codeword.map((_, i) => (i < k ? "data" : "parity"))}
            />
          </>
        ),
      },
    ];
  }, [text, k, nsym]);

  return <SlideDeck slides={slides} label="Presentación: cómo codifica Reed-Solomon" />;
}
