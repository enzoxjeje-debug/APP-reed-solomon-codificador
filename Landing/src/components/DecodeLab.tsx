import * as React from "react";
import { CAPACITY, computeSyndromes, decodeBlock, polyToString, symbolsToText } from "../rs/engine";
import { Pipeline, SlideDeck, SymChips, type Slide, type SymKind } from "./parts";

export default function DecodeLab({
  codeword,
  k,
  nsym,
}: {
  codeword: number[];
  k: number;
  nsym: number;
}) {
  const [errors, setErrors] = React.useState<Record<number, number>>({});

  const received = React.useMemo(
    () => codeword.map((v, i) => (i in errors ? errors[i] : v)),
    [codeword, errors]
  );
  const dec = React.useMemo(() => decodeBlock(received, k, nsym), [received, k, nsym]);
  const t = CAPACITY(nsym);
  const n = k + nsym;
  const errIdx = Object.keys(errors).map(Number);

  const corrupt = (i: number) => {
    setErrors((prev) => {
      const next = { ...prev };
      if (i in next) delete next[i];
      else next[i] = (codeword[i] + 37 + i * 53) % 256;
      if (next[i] === codeword[i]) next[i] = (next[i] + 1) % 256;
      return next;
    });
  };
  const randomError = () => {
    const free = codeword.map((_, i) => i).filter((i) => !(i in errors));
    if (!free.length) return;
    corrupt(free[Math.floor(Math.random() * free.length)]);
  };

  // La palabra RECIBIDA siempre marca en rojo dónde metiste el error.
  // El verde "reparado" solo aparece en la palabra CORREGIDA del paso 5.
  const kinds: SymKind[] = received.map((_, i) =>
    i in errors ? "error" : i < k ? "data" : "parity"
  );
  const posLabels = received.map((_, i) =>
    i in errors
      ? `posición ${i}: original ${codeword[i]} → tu error ${received[i]}`
      : `posición ${i} · valor ${received[i]}`
  );

  // Mapa del camino: qué etapas ya se pudieron completar con esta palabra.
  const stages = [
    {
      label: "1 · Síndromes",
      detail: dec.syndromes.length ? `${dec.syndromes.filter((s) => s !== 0).length} ≠ 0` : "—",
      state: (dec.syndromes.length ? "done" : "todo") as "done" | "todo",
    },
    {
      label: "2 · Λ(x)",
      detail: dec.lambda ? `grado ${dec.lambda.length - 1}` : "—",
      state: (dec.lambda ? "done" : "todo") as "done" | "todo",
    },
    {
      label: "3 · Chien",
      detail: dec.chien.length ? `${dec.positions.length} raíz${dec.positions.length === 1 ? "" : "es"}` : "—",
      state: (!dec.chien.length ? "todo" : dec.positions.length ? "done" : "bad") as "done" | "bad" | "todo",
    },
    {
      label: "4 · Forney",
      detail: dec.forney.length ? `${dec.forney.length} magnitud${dec.forney.length === 1 ? "" : "es"}` : "—",
      state: (!dec.forney.length
        ? "todo"
        : dec.status === "CORRECTED"
          ? "done"
          : "bad") as "done" | "bad" | "todo",
    },
    {
      label: "5 · Verificación",
      detail:
        dec.status === "CORRECTED" ? "cero ✓" : dec.status === "NO_ERROR" ? "intacto ✓" : dec.status === "TOO_MANY_ERRORS" ? "falla ✕" : "—",
      state: (dec.status === "CORRECTED" || dec.status === "NO_ERROR"
        ? "done"
        : dec.status === "TOO_MANY_ERRORS"
          ? "bad"
          : "todo") as "done" | "bad" | "todo",
    },
  ];

  const verification = dec.status === "CORRECTED" ? computeSyndromes(dec.corrected, nsym) : null;

  const slides: Slide[] = [
    {
      kicker: "El crimen",
      title: "Dañá la palabra a propósito",
      body: (
        <>
          <p className="rs-lede text-[15px]">
            Esta es la palabra que "viajó por el canal". <b>Tocá un símbolo para dañarlo</b>{" "}
            (o usá el botón aleatorio). El decodificador no sabe lo que tocaste: tiene que{" "}
            <b>descubrir dónde, cuánto y si puede repararlo</b> siguiendo este camino:
          </p>
          <Pipeline stages={stages} />
          <div className="flex flex-wrap gap-2">
            <button type="button" className="rs-btn rs-btn-solid !py-2.5" onClick={randomError}>
              Error aleatorio
            </button>
            <button type="button" className="rs-btn rs-btn-line !py-2.5" onClick={() => setErrors({})}>
              Limpiar
            </button>
            <span className={`rs-chip ${errIdx.length > t ? "bad" : errIdx.length ? "warn" : "ok"}`}>
              {errIdx.length} {errIdx.length === 1 ? "error" : "errores"} / máx {t}
            </span>
          </div>
          <SymChips values={received} kinds={kinds} labels={posLabels} pick onPick={corrupt} />
          <p className="rs-mono text-[var(--rs-muted)]">
            posiciones 0–{k - 1} = datos · {k}–{n - 1} = paridad · rojo = tu error (pasá el mouse para ver el valor original)
          </p>
        </>
      ),
    },
    {
      kicker: "Síndromes: ¿llegó dañada?",
      title: "El chequeo de salud",
      body: (
        <>
          <p className="rs-lede text-[15px]">
            Se evalúa la palabra recibida R(x) en α⁰…α<sup>{nsym - 1}</sup>: cada resultado es un
            síndrome Sⱼ. Una palabra válida da <b>cero en todos</b>. Los que no dan cero{" "}
            <b>son la huella del error</b>: con ellos (y solo con ellos) se reconstruye todo lo demás.
          </p>
          <div className="flex flex-wrap gap-1.5">
            {dec.syndromes.map((s, j) => (
              <span key={j} className={`rs-chip ${s === 0 ? "ok" : "bad"}`} title={`S${j + 1} = R(α^${j})`}>
                S{j + 1} = {s}
              </span>
            ))}
          </div>
          <div className="rs-formula">
            {dec.syndromes.map((s, j) => `S${j + 1} = R(α^${j}) = ${s}`).join(" · ")}
          </div>
          <p className="rs-lede text-[15px]">
            {dec.syndromes.every((s) => s === 0)
              ? "Todos en cero: el mensaje está intacto y el camino termina acá."
              : "Hay valores ≠ 0: el mensaje está dañado. El camino sigue con Berlekamp-Massey."}
          </p>
        </>
      ),
    },
    {
      kicker: "Berlekamp-Massey: ¿cuántos errores hay?",
      title: "El polinomio localizador Λ(x)",
      body: dec.lambda && dec.bm ? (
        <>
          <p className="rs-lede text-[15px]">
            Con solo los síndromes, este algoritmo <b>deduce cuántos símbolos se dañaron y cómo se
            relacionan</b>, resumido en Λ(x). Su <b>grado = cantidad de errores</b> que cree haber:{" "}
            <span className={`rs-chip ${dec.lambda.length - 1 > t || dec.lambda.length - 1 <= 0 ? "bad" : "ok"}`}>
              grado {dec.lambda.length - 1} · capacidad {t}
            </span>
          </p>
          <div className="rs-formula">Λ(x) = {polyToString(dec.lambda)}</div>
          <p className="rs-mono text-[var(--rs-muted)]">cómo lo construyó, síndrome por síndrome:</p>
          <div className="flex flex-col gap-1">
            {dec.bm.iterations.map((it) => (
              <p key={it.n} className="rs-mono text-[var(--rs-muted)]">
                n={it.n} Δ={it.delta} → L: {it.lBefore} → {it.lAfter}
                {it.delta !== 0 && it.lAfter > it.lBefore ? " ★ encontró un error más" : ""}
              </p>
            ))}
          </div>
        </>
      ) : (
        <p className="rs-lede text-[15px]">
          No hizo falta: los síndromes eran cero y no hay polinomio que buscar.
        </p>
      ),
    },
    {
      kicker: "Chien: ¿DÓNDE está cada error?",
      title: "Probar posición por posición",
      body: dec.chien.length ? (
        <>
          <p className="rs-lede text-[15px]">
            Cada posición <b>pos</b> tiene un exponente <b>m = {n}−1−pos</b>. Chien evalúa{" "}
            <b>Λ(α⁻ᵐ)</b> en todas: donde da <b>cero exacto</b> hay un error, y ese{" "}
            <b>X = αᵐ</b> es su "valor localizador". Las demás posiciones se descartan.
          </p>
          <div className="rs-chien" role="table" aria-label="Evaluación de Chien por posición">
            {dec.chien.map((c) => (
              <span
                key={c.pos}
                role="cell"
                className={`rs-chien-cell ${c.isRoot ? "hit" : "ok"}`}
                title={`posición ${c.pos}, m=${c.degree}, Λ(α^−${c.degree})=${c.evaluation}`}
              >
                p{c.pos} · Λ={c.evaluation} {c.isRoot ? "✕ ERROR" : "✓"}
              </span>
            ))}
          </div>
          <SymChips values={received} kinds={kinds} labels={posLabels} />
          <p className="rs-mono text-[var(--rs-muted)]">
            {dec.positions.length
              ? `errores localizados en ${dec.positions.map((p) => `posición ${p.pos} (X = α^${p.degree})`).join(" · ")}`
              : "Ninguna evaluación dio cero: el daño supera lo que Λ(x) puede explicar."}
          </p>
        </>
      ) : (
        <p className="rs-lede text-[15px]">
          {dec.lambda
            ? "Λ(x) existe pero su grado excede la capacidad: no hay posiciones que buscar."
            : "Sin errores no hay posiciones que localizar."}
        </p>
      ),
    },
    {
      kicker: "Forney: ¿CUÁNTO cambió cada símbolo?",
      title: "La magnitud exacta del daño",
      body: dec.forney.length && dec.omega ? (
        <>
          <p className="rs-lede text-[15px]">
            Ya sabemos <b>dónde</b>. Ahora, con el polinomio evaluador{" "}
            <b>Ω(x) = S(x)·Λ(x) mod x^{nsym}</b> y la fórmula de Forney, calculamos{" "}
            <b>cuánto</b> se alteró cada símbolo: <i>magnitud = X · Ω(X⁻¹) / Λ′(X⁻¹)</i>, y se
            repara con <i>corregido = recibido ⊕ magnitud</i>.
          </p>
          <div className="rs-formula">Ω(x) = {polyToString(dec.omega)}</div>
          <div className="flex flex-col gap-3">
            {dec.forney.map((f) => (
              <div key={f.pos} className="rs-forney">
                <p className="rs-forney-head">
                  Error en posición {f.pos} (X = α^{f.degree} = {f.x})
                  <span className="rs-chip ok">reparado</span>
                </p>
                <p className="rs-mono text-[var(--rs-muted)]">
                  X⁻¹ = {f.xInv} · Ω(X⁻¹) = {f.omegaEval} · Λ′(X⁻¹) = {f.primeEval}
                </p>
                <p className="rs-mono text-[var(--rs-muted)]">
                  magnitud = {f.x} × {f.omegaEval} ÷ {f.primeEval} = <b>{f.magnitude}</b>
                </p>
                <p className="rs-mono">
                  recibido {f.received} ⊕ {f.magnitude} = <b>{f.corrected}</b>
                </p>
              </div>
            ))}
          </div>
          <SymChips values={dec.corrected} kinds={dec.corrected.map((_, i) => (dec.positions.some((p) => p.pos === i) ? "fixed" : i < k ? "data" : "parity"))} labels={posLabels} />
        </>
      ) : (
        <p className="rs-lede text-[15px]">
          No hay magnitudes que calcular: o no hubo errores, o el daño excede la capacidad.
        </p>
      ),
    },
    {
      kicker: "Verificación: ¿quedó perfecta?",
      title: "Los síndromes tienen la última palabra",
      body: (
        <>
          {dec.status === "CORRECTED" && verification && (
            <>
              <span className="rs-chip ok w-fit">✓ corregido y verificado</span>
              <p className="rs-lede text-[15px]">
                Se recalculan los síndromes sobre la palabra reparada: <b>volvieron todos a cero</b>,
                prueba de que la corrección fue exacta. Mensaje recuperado:
              </p>
              <div className="flex flex-wrap gap-1.5">
                {verification.map((s, j) => (
                  <span key={j} className="rs-chip ok">
                    S{j + 1} = {s}
                  </span>
                ))}
              </div>
              <div className="rs-formula">«{symbolsToText(dec.corrected.slice(0, k))}»</div>
            </>
          )}
          {dec.status === "NO_ERROR" && (
            <>
              <span className="rs-chip ok w-fit">✓ intacto</span>
              <p className="rs-lede text-[15px]">
                Síndromes en cero desde el inicio, nada que reparar: «{symbolsToText(dec.corrected.slice(0, k))}»
              </p>
            </>
          )}
          {dec.status === "TOO_MANY_ERRORS" && (
            <>
              <span className="rs-chip bad w-fit">✗ no corregible</span>
              <p className="rs-lede text-[15px]">
                {dec.note} El decodificador se niega a inventar datos: es mejor avisar que
                devolver un mensaje falso.
              </p>
            </>
          )}
        </>
      ),
    },
  ];

  return <SlideDeck slides={slides} label="Presentación: cómo decodifica Reed-Solomon" />;
}
