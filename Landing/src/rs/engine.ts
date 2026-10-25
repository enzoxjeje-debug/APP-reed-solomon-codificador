/* Mini motor Reed-Solomon sobre GF(256), portado del motor Kotlin de la app
   (GF256.kt / GFPoly.kt / ReedSolomon.kt). Solo para la demo de la landing:
   bloques cortos, aritmética exacta con tablas exp/log en base α = 2. */

// ---------------- GF(256) ----------------
const EXP = new Int32Array(512);
const LOG = new Int32Array(256);
(function init() {
  let x = 1;
  for (let i = 0; i < 255; i++) {
    EXP[i] = x;
    LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
})();

const alphaPower = (e: number): number => EXP[((e % 255) + 255) % 255];
const gfMul = (a: number, b: number): number => (a === 0 || b === 0 ? 0 : EXP[LOG[a] + LOG[b]]);
const gfDiv = (a: number, b: number): number => {
  if (b === 0) throw new Error("División entre cero en GF(256)");
  if (a === 0) return 0;
  return EXP[LOG[a] - LOG[b] + 255];
};

// ---------------- Polinomios (little-endian: coeffs[0] = término independiente) ----------------
type Poly = number[];

const trim = (p: Poly): Poly => {
  let d = p.length - 1;
  while (d > 0 && p[d] === 0) d--;
  return p.slice(0, d + 1);
};
const degree = (p: Poly): number => trim(p).length - 1;
const polyMul = (a: Poly, b: Poly): Poly => {
  const out = new Array(a.length + b.length - 1).fill(0);
  for (let i = 0; i < a.length; i++)
    for (let j = 0; j < b.length; j++) out[i + j] ^= gfMul(a[i], b[j]);
  return trim(out);
};
const polyEval = (p: Poly, x: number): number => {
  let r = 0;
  for (let i = p.length - 1; i >= 0; i--) r = gfMul(r, x) ^ p[i];
  return r;
};
/** Símbolos en orden de transmisión ([0] = mayor grado) → coeficientes little-endian. */
const fromSymbols = (sym: number[]): Poly => [...sym].reverse();
/** Coeficientes little-endian → símbolos ([0] = mayor grado), relleno con ceros a la izquierda. */
const toSymbols = (p: Poly, length: number): number[] => {
  const t = trim(p);
  const out = new Array<number>(length).fill(0);
  for (let k = 0; k < length; k++) {
    const deg = length - 1 - k;
    out[k] = deg < t.length ? t[deg] : 0;
  }
  return out;
};
const polyDerivative = (p: Poly): Poly => {
  if (p.length <= 1) return [0];
  const out: number[] = [];
  for (let i = 1; i < p.length; i++) out.push(i % 2 === 1 ? p[i] : 0);
  return trim(out);
};
const polyToString = (p: Poly): string => {
  const t = trim(p);
  const parts: string[] = [];
  for (let i = t.length - 1; i >= 0; i--) {
    if (t[i] === 0) continue;
    const c = t[i] === 1 && i > 0 ? "" : String(t[i]);
    parts.push(i === 0 ? `${t[i]}` : i === 1 ? `${c}x` : `${c}x^${i}`);
  }
  return parts.length ? parts.join(" + ") : "0";
};

// ---------------- Codificador ----------------
export function buildGenerator(nsym: number): Poly {
  let g: Poly = [1];
  for (let i = 0; i < nsym; i++) g = polyMul(g, [alphaPower(i), 1]);
  return g;
}

export interface BlockEncoding {
  data: number[];
  parity: number[];
  codeword: number[];
}

export function encodeBlock(data: number[], nsym: number, gen: Poly): BlockEncoding {
  const g = trim(gen);
  const msg = fromSymbols(data);
  const shifted: Poly = [...new Array(nsym).fill(0), ...msg];
  const rem = [...shifted];
  const qSize = degree(shifted) - degree(g) + 1;
  const dLeadInv = EXP[255 - LOG[g[degree(g)]]]; // 1 / coef. líder
  for (let i = qSize - 1; i >= 0; i--) {
    const idx = i + degree(g);
    const factor = gfMul(rem[idx] ?? 0, dLeadInv);
    if (factor !== 0)
      for (let j = 0; j <= degree(g); j++) rem[i + j] ^= gfMul(factor, g[j]);
  }
  const parity = toSymbols(rem, nsym);
  return { data: [...data], parity, codeword: [...data, ...parity] };
}

// ---------------- Decodificador ----------------
export function computeSyndromes(codeword: number[], nsym: number): number[] {
  const p = fromSymbols(codeword);
  return Array.from({ length: nsym }, (_, j) => polyEval(p, alphaPower(j)));
}

export interface BMResult {
  lambda: Poly;
  iterations: { n: number; delta: number; lBefore: number; lAfter: number }[];
}

export function berlekampMassey(syndromes: number[]): BMResult {
  let lambda: Poly = [1];
  let b: Poly = [1];
  let l = 0, m = 1, bScale = 1;
  const iterations: BMResult["iterations"] = [];
  for (let n = 0; n < syndromes.length; n++) {
    let delta = syndromes[n];
    for (let j = 1; j <= l; j++) delta ^= gfMul(lambda[j] ?? 0, syndromes[n - j]);
    const lBefore = l;
    if (delta === 0) {
      m++;
    } else if (2 * l <= n) {
      const t = lambda;
      const factor = gfDiv(delta, bScale);
      const shifted = [...new Array(m).fill(0), ...b.map((c) => gfMul(factor, c))];
      const out = new Array(Math.max(lambda.length, shifted.length)).fill(0);
      lambda.forEach((c, i) => (out[i] ^= c));
      shifted.forEach((c, i) => (out[i] ^= c));
      lambda = trim(out);
      l = n + 1 - l;
      b = t;
      bScale = delta;
      m = 1;
    } else {
      const factor = gfDiv(delta, bScale);
      const shifted = [...new Array(m).fill(0), ...b.map((c) => gfMul(factor, c))];
      const out = new Array(Math.max(lambda.length, shifted.length)).fill(0);
      lambda.forEach((c, i) => (out[i] ^= c));
      shifted.forEach((c, i) => (out[i] ^= c));
      lambda = trim(out);
      m++;
    }
    iterations.push({ n, delta, lBefore, lAfter: l });
  }
  return { lambda, iterations };
}

export interface ChienCheck {
  pos: number;
  /** Exponente m = n−1−pos: la posición se evalúa como Λ(α^(−m)). */
  degree: number;
  evaluation: number;
  isRoot: boolean;
}

/** Búsqueda de Chien: evalúa Λ(α^(−m)) en TODAS las posiciones, sean raíz o no. */
export function chienSearch(lambda: Poly, n: number): ChienCheck[] {
  const checks: ChienCheck[] = [];
  for (let pos = 0; pos < n; pos++) {
    const deg = n - 1 - pos;
    const evaluation = polyEval(lambda, alphaPower(-deg));
    checks.push({ pos, degree: deg, evaluation, isRoot: evaluation === 0 });
  }
  return checks;
}

export type DecodeStatus = "NO_ERROR" | "CORRECTED" | "TOO_MANY_ERRORS";

export interface ForneyDetail {
  pos: number;
  degree: number;
  /** X = α^m: valor localizador del error. */
  x: number;
  xInv: number;
  omegaEval: number;
  primeEval: number;
  received: number;
  magnitude: number;
  corrected: number;
}

export interface BlockDecoding {
  status: DecodeStatus;
  syndromes: number[];
  bm: BMResult | null;
  lambda: Poly | null;
  /** Tabla completa de Chien (todas las posiciones evaluadas). */
  chien: ChienCheck[];
  /** Solo las posiciones donde Λ se anuló. */
  positions: ChienCheck[];
  omega: Poly | null;
  forney: ForneyDetail[];
  corrected: number[];
  note: string | null;
}

export function decodeBlock(received: number[], k: number, nsym: number): BlockDecoding {
  const t = Math.floor(nsym / 2);
  if (received.length !== k + nsym)
    return {
      status: "TOO_MANY_ERRORS", syndromes: [], bm: null, lambda: null, chien: [],
      positions: [], omega: null, forney: [],
      corrected: [...received],
      note: `El bloque debe tener ${k + nsym} símbolos (datos ${k} + paridad ${nsym}).`,
    };
  const syndromes = computeSyndromes(received, nsym);
  if (syndromes.every((s) => s === 0))
    return {
      status: "NO_ERROR", syndromes, bm: null, lambda: null, chien: [],
      positions: [], omega: null, forney: [],
      corrected: [...received], note: null,
    };
  const bm = berlekampMassey(syndromes);
  const lambda = bm.lambda;
  if (degree(lambda) > t || degree(lambda) <= 0)
    return {
      status: "TOO_MANY_ERRORS", syndromes, bm, lambda, chien: [],
      positions: [], omega: null, forney: [],
      corrected: [...received],
      note: `Λ(x) tiene grado ${degree(lambda)} pero este código solo corrige ${t} por bloque.`,
    };
  const chien = chienSearch(lambda, received.length);
  const positions = chien.filter((c) => c.isRoot);
  if (positions.length !== degree(lambda))
    return {
      status: "TOO_MANY_ERRORS", syndromes, bm, lambda, chien, positions,
      omega: null, forney: [],
      corrected: [...received],
      note: `Chien halló ${positions.length} raíces pero Λ(x) predice ${degree(lambda)} errores.`,
    };
  // Ω(x) = S(x)·Λ(x) mod x^nsym
  const prod = polyMul(syndromes, lambda);
  const omega = trim(prod.slice(0, nsym));
  const lambdaPrime = polyDerivative(lambda);
  const forney: ForneyDetail[] = positions.map(({ pos, degree: deg }) => {
    const x = alphaPower(deg);
    const xInv = alphaPower(-deg);
    const w = polyEval(omega, xInv);
    const d = polyEval(lambdaPrime, xInv);
    const magnitude = d === 0 ? 0 : gfMul(x, gfDiv(w, d));
    return {
      pos, degree: deg, x, xInv, omegaEval: w, primeEval: d,
      received: received[pos], magnitude, corrected: received[pos] ^ magnitude,
    };
  });
  const corrected = [...received];
  forney.forEach((f) => (corrected[f.pos] = f.corrected));
  if (computeSyndromes(corrected, nsym).every((s) => s === 0))
    return { status: "CORRECTED", syndromes, bm, lambda, chien, positions, omega, forney, corrected, note: null };
  return {
    status: "TOO_MANY_ERRORS", syndromes, bm, lambda, chien, positions,
    omega, forney,
    corrected: [...received],
    note: "Los síndromes no volvieron a cero: el daño supera la capacidad.",
  };
}

// ---------------- Utilidades para la demo ----------------
export const textToSymbols = (text: string): { chars: { ch: string; bytes: number[] }[]; symbols: number[] } => {
  const bytes = Array.from(new TextEncoder().encode(text));
  return { chars: [...text].map((ch) => ({ ch, bytes: [...new TextEncoder().encode(ch)] })), symbols: bytes };
};

export const symbolsToText = (symbols: number[]): string => {
  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(new Uint8Array(symbols));
  } catch {
    return "� (bytes no válidos en UTF-8)";
  }
};

export { polyToString };
export const CAPACITY = (nsym: number) => Math.floor(nsym / 2);
