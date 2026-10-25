import * as React from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";

export type SymKind = "data" | "parity" | "normal" | "error" | "fixed";

export function SymChips({
  values,
  kinds,
  labels,
  pick,
  onPick,
}: {
  values: number[];
  kinds?: SymKind[];
  labels?: (string | undefined)[];
  pick?: boolean;
  onPick?: (i: number) => void;
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {values.map((v, i) => (
        <span
          key={i}
          title={labels?.[i] ?? `símbolo ${i}`}
          onClick={pick ? () => onPick?.(i) : undefined}
          className={`rs-sym ${kinds?.[i] ?? "normal"}${pick ? " pick" : ""}`}
          style={{ animationDelay: `${Math.min(i * 28, 560)}ms` }}
        >
          {v}
        </span>
      ))}
    </div>
  );
}

export interface Slide {
  kicker: string;
  title: string;
  body: React.ReactNode;
}

// La presentación activa (la que tiene el mouse o el foco): solo ella responde al teclado.
let activeDeck = 0;
let deckSeq = 0;

/** Presentación estilo PowerPoint: diapositiva grande, flechas, puntos, ←/→ y barra de progreso. */
export function SlideDeck({ slides, label }: { slides: Slide[]; label: string }) {
  const [index, setIndex] = React.useState(0);
  const [dir, setDir] = React.useState(1);
  const id = React.useRef(0);
  if (id.current === 0) id.current = ++deckSeq;
  const indexRef = React.useRef(0);
  const n = slides.length;
  const safe = Math.max(0, Math.min(n - 1, index));

  const go = React.useCallback(
    (next: number) => {
      const clamped = Math.max(0, Math.min(n - 1, next));
      setDir(clamped >= indexRef.current ? 1 : -1);
      indexRef.current = clamped;
      setIndex(clamped);
    },
    [n]
  );

  React.useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (activeDeck !== id.current) return;
      if (e.key === "ArrowRight") {
        e.preventDefault();
        go(indexRef.current + 1);
      } else if (e.key === "ArrowLeft") {
        e.preventDefault();
        go(indexRef.current - 1);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [go]);

  const s = slides[safe];
  return (
    <div
      role="region"
      aria-roledescription="presentación"
      aria-label={label}
      tabIndex={0}
      onMouseEnter={() => {
        activeDeck = id.current;
      }}
      onFocus={() => {
        activeDeck = id.current;
      }}
      className="rs-card overflow-hidden outline-none focus-visible:ring-2 focus-visible:ring-[var(--rs-accent)]"
    >
      {/* Barra de progreso */}
      <div className="rs-progress" aria-hidden="true">
        <i style={{ width: `${((safe + 1) / n) * 100}%` }} />
      </div>
      {/* Diapositiva */}
      <div key={safe} className={dir >= 0 ? "rs-slide-r p-5 sm:p-8" : "rs-slide-l p-5 sm:p-8"}>
        <p className="rs-kicker">{s.kicker}</p>
        <h3 className="mt-2 text-2xl font-extrabold tracking-tight sm:text-3xl">{s.title}</h3>
        <div className="mt-4 flex flex-col gap-4">{s.body}</div>
      </div>
      {/* Controles */}
      <div className="flex flex-wrap items-center gap-3 border-t border-[var(--rs-line)] px-5 py-4 sm:px-8">
        <button
          type="button"
          className="rs-btn rs-btn-line !px-4 !py-2.5"
          disabled={safe === 0}
          onClick={() => go(safe - 1)}
          aria-label="Diapositiva anterior"
        >
          <ArrowLeft size={15} />
        </button>
        <button
          type="button"
          className="rs-btn rs-btn-solid !px-4 !py-2.5"
          disabled={safe === n - 1}
          onClick={() => go(safe + 1)}
          aria-label="Diapositiva siguiente"
        >
          <ArrowRight size={15} />
        </button>
        <span className="rs-mono text-[var(--rs-muted)]" aria-live="polite">
          {safe + 1} / {n}
        </span>
        <div className="flex items-center gap-1.5" role="tablist" aria-label="Ir a la diapositiva">
          {slides.map((sl, i) => (
            <button
              key={sl.title}
              type="button"
              role="tab"
              aria-selected={i === safe}
              aria-label={`Ir a la diapositiva ${i + 1}: ${sl.title}`}
              title={sl.title}
              onClick={() => go(i)}
              className={`rs-dot${i === safe ? " now" : ""}${i < safe ? " done" : ""}`}
            />
          ))}
        </div>
        <span className="rs-mono ml-auto hidden text-[var(--rs-muted)] sm:inline">
          <kbd className="rs-kbd">←</kbd> <kbd className="rs-kbd">→</kbd> para pasar
        </span>
      </div>
    </div>
  );
}

export type PipeState = "done" | "bad" | "todo";

/** Mapa del camino síndromes → Λ → Chien → Forney → verificación, con el estado real. */
export function Pipeline({ stages }: { stages: { label: string; detail: string; state: PipeState }[] }) {
  return (
    <ol className="flex flex-col gap-1.5 sm:flex-row sm:flex-wrap">
      {stages.map((s, i) => (
        <li key={s.label} className="flex min-w-0 flex-1 items-center gap-1.5">
          <div className={`rs-pipe rs-pipe-${s.state}`}>
            <span className="rs-pipe-n">{s.state === "done" ? "✓" : s.state === "bad" ? "✕" : i + 1}</span>
            <span className="rs-pipe-t">
              {s.label}
              <small>{s.detail}</small>
            </span>
          </div>
          {i < stages.length - 1 && (
            <span className="rs-pipe-arrow" aria-hidden="true">
              →
            </span>
          )}
        </li>
      ))}
    </ol>
  );
}

export function Reveal({ children, id }: { children: React.ReactNode; id?: string }) {
  const ref = React.useRef<HTMLDivElement | null>(null);
  const [inView, setInView] = React.useState(false);
  React.useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (typeof IntersectionObserver !== "function") {
      setInView(true);
      return;
    }
    const io = new IntersectionObserver(
      (es) => {
        if (es.some((e) => e.isIntersecting)) {
          setInView(true);
          io.disconnect();
        }
      },
      { threshold: 0.12 }
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);
  return (
    <div id={id} ref={ref} className={inView ? "rs-reveal" : "opacity-0"} style={{ scrollMarginTop: 24 }}>
      {children}
    </div>
  );
}
