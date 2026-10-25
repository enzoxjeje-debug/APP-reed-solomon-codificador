// The Astro Association hero, standalone: a giant wordmark filled with a
// living confetti of bubbles — they drift, scatter from the pointer and pop
// when clicked, with a running count — a compass-ring logo whose needle
// follows you, a sliced title, a huge issue number, a blurb, two CTAs and a
// credit rule, over slowly turning orbit lines.
//
// Part of the Astro library — lifted out of astro-association-template, so it
// looks the same and stacks with the other pieces. Every picture is drawn in
// this file (canvas + SVG). Nothing loads at runtime.
import * as React from "react"

/* ------------------------------------------------------------------ types */

export type AstroNavLink = { label: string; /** A section key ("mission") or any href. */ target: string }
export type AstroPalette = {
  name: string
  /** Type, buttons, rules. */
  accent: string
  /** The page frame. */
  frame: string
  /** Bubble colours. The first is the ground the others sit on. */
  colors: string[]
}
export type AstroPaletteName = "cobalt" | "aurora" | "nebula" | "solar"
export type AstroHero = {
  /** The bubble-filled wordmark. Short words read best. */
  wordmark: string
  issue: string
  /** `
` breaks the line; the last line gets the sliced echo. */
  title: string
  blurb: string
  credit: string
  primaryCta: string
  secondaryCta: string
  /** Under the wordmark. Empty hides it. */
  hint: string
}
export type AstroStat = { value: string; label: string }
export type AstroMission = {
  /** White type on the bubble band. `
` breaks the line. */
  bandTitle: string
  kicker: string
  /** Three small labels spread across a hairline. */
  meta: string[]
  title: string
  body: string
  stats: AstroStat[]
}
export type AstroProject = {
  code: string
  title: string
  summary: string
  detail: string
  status: "open" | "ongoing" | "archived"
  /** 0–1. Omit to hide the bar. */
  progress?: number
  /** Caption for the bar, e.g. "7,412 of 10,000 readings". */
  goal?: string
  cta?: string
}
export type AstroProjects = {
  /** Oversized type behind the ribbons. `
` breaks the line. */
  title: string
  issue: string
  /** Text that runs along the dark ribbons. */
  ribbonText: string
  body: string
  heading: string
}
export type AstroNight = {
  /** ISO date, "2026-11-14". */
  date: string
  time: string
  title: string
  place: string
  /** Filter chip: "Star party", "Talk", ... */
  kind: string
  /** What to look at. */
  target: string
  seats?: number
  going?: number
}
export type AstroTier = { name: string; price: string; period?: string; blurb: string; perks: string[]; featured?: boolean }
export type AstroJoin = {
  tag: string
  title: string
  body: string
  namePlaceholder: string
  emailPlaceholder: string
  button: string
  /** `{name}` is replaced with the member's first name. */
  success: string
}
export type AstroFooter = { bandTitle: string; tagline: string; credit: string; links: { label: string; href: string }[] }
export type AstroMember = { name: string; email: string; tier: string; number: string }

export type AstroBubbleHeroProps = {
  brand?: string
  /** The bubble-filled wordmark. Short words read best. */
  wordmark?: string
  issue?: string
  /** `
` breaks the line. */
  title?: string
  blurb?: string
  credit?: string
  primaryCta?: string
  onPrimary?: () => void
  secondaryCta?: string
  onSecondary?: () => void
  /** Under the wordmark. Empty hides it. */
  hint?: string
  onPop?: (total: number) => void
  /** Starting arrangement of the bubbles. Any integer. */
  seed?: number
  /** Minimum height. Must be a definite length. */
  height?: string
  /** A preset ("cobalt" · "aurora" · "nebula" · "solar") or your own palette. */
  palette?: AstroPaletteName | AstroPalette
  /** "system" follows the host's .dark class, then the OS. */
  defaultTheme?: "system" | "light" | "dark"
  onThemeChange?: (theme: "light" | "dark") => void
  /** The periwinkle page frame. Turn off when stacking pieces. */
  frame?: boolean
  fonts?: { display?: string; body?: string; mono?: string }
  maxWidth?: string
  className?: string
}

/* ------------------------------------------------------------------ logic */

// #region logic
function clamp(v: number, a: number, b: number): number {
  return Math.min(b, Math.max(a, v))
}

function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function hashStr(s: string): number {
  let h = 2166136261
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

function isEmail(s: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(s.trim())
}

function easeOutCubic(t: number): number {
  const c = clamp(t, 0, 1)
  return 1 - Math.pow(1 - c, 3)
}

// "ASTRO.
// ASSOCiATION" → lines. A literal backslash-n counts too, since
// that's what "
// " becomes in a JSX attribute.
function lines(s: string): string[] {
  return s.split(/\n|\\n/).filter((l) => l.length > 0)
}

type Bubble = { x: number; y: number; r: number; c: number; ph: number; amp: number; sp: number }

// A seeded confetti of circles covering a w × h field (and a little past its
// edges, so nothing reads as cut). Sizes lean small; the big ones go first so
// the small ones layer on top.
function buildBubbles(seed: number, w: number, h: number, nColors: number, density = 1, scale = 1): Bubble[] {
  const rnd = mulberry32(seed)
  const unit = Math.max(6, Math.min(w, h) * 0.12 * scale)
  const n = Math.round(clamp(((w * h) / (unit * unit * 1.25)) * density, 12, 1400))
  const out: Bubble[] = []
  for (let i = 0; i < n; i++) {
    const r = unit * (0.22 + 1.3 * Math.pow(rnd(), 2.3))
    out.push({
      x: rnd() * (w + r) - r / 2,
      y: rnd() * (h + r) - r / 2,
      r,
      c: 1 + Math.floor(rnd() * Math.max(1, nColors - 1)),
      ph: rnd() * Math.PI * 2,
      amp: 1 + rnd() * 3.5,
      sp: 0.25 + rnd() * 0.6,
    })
  }
  return out.sort((a, b) => b.r - a.r)
}

// The same confetti as a CSS background, for small surfaces that don't need a
// live canvas: hard-edged radial gradients over the ground colour.
function bubbleCss(seed: number, colors: string[], n = 34, maxR = 46): string {
  const rnd = mulberry32(seed)
  const layers: string[] = []
  for (let i = 0; i < n; i++) {
    const r = Math.round(6 + (maxR - 6) * Math.pow(rnd(), 1.8))
    const x = Math.round(rnd() * 100)
    const y = Math.round(rnd() * 100)
    const c = colors[1 + Math.floor(rnd() * Math.max(1, colors.length - 1))] ?? colors[0]
    layers.push("radial-gradient(circle at " + x + "% " + y + "%, " + c + " 0 " + r + "px, transparent " + (r + 0.6) + "px)")
  }
  return layers.reverse().join(", ") + ", " + (colors[0] ?? "#2b3bff")
}

const SYNODIC = 29.530588853
// A known new moon: 2000-01-06 18:14 UTC.
const NEW_MOON_REF = Date.UTC(2000, 0, 6, 18, 14)

function moonPhase(at: Date | number): { age: number; fraction: number; illumination: number; name: string; waxing: boolean; daysToFull: number; daysToNew: number } {
  const ms = typeof at === "number" ? at : at.getTime()
  const days = (ms - NEW_MOON_REF) / 86400000
  const age = ((days % SYNODIC) + SYNODIC) % SYNODIC
  const fraction = age / SYNODIC
  const illumination = (1 - Math.cos(2 * Math.PI * fraction)) / 2
  return {
    age,
    fraction,
    illumination,
    name: phaseName(fraction),
    waxing: fraction < 0.5,
    daysToFull: (((0.5 - fraction) % 1) + 1) % 1 * SYNODIC,
    daysToNew: ((1 - fraction) % 1) * SYNODIC,
  }
}

function phaseName(f: number): string {
  if (f < 0.0339 || f > 0.9661) return "New Moon"
  if (f < 0.216) return "Waxing Crescent"
  if (f < 0.284) return "First Quarter"
  if (f < 0.466) return "Waxing Gibbous"
  if (f < 0.534) return "Full Moon"
  if (f < 0.716) return "Waning Gibbous"
  if (f < 0.784) return "Last Quarter"
  return "Waning Crescent"
}

// The lit part of a moon of radius r centred on 0,0, as an SVG path:
// the bright limb as a half circle, closed by the terminator's half ellipse.
function moonPath(fraction: number, r: number): string {
  const f = ((fraction % 1) + 1) % 1
  const waxing = f < 0.5
  const c = Math.cos(2 * Math.PI * f)
  const rx = Math.abs(c) * r
  const limb = waxing ? 1 : 0
  const term = waxing ? (c > 0 ? 0 : 1) : c < 0 ? 0 : 1
  const R = r.toFixed(2)
  return "M0,-" + R + "A" + R + "," + R + " 0 0 " + limb + " 0," + R + "A" + rx.toFixed(2) + "," + R + " 0 0 " + term + " 0,-" + R + "Z"
}

const MONTHS = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"]
const WEEKDAYS = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"]

// "2026-11-14" → its parts, read as a calendar date (no timezone drift).
function parseDay(iso: string): { day: string; mon: string; dow: string; ms: number } | null {
  const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!m) return null
  const ms = Date.UTC(+m[1], +m[2] - 1, +m[3], 21)
  const d = new Date(ms)
  if (d.getUTCMonth() !== +m[2] - 1) return null
  return { day: String(d.getUTCDate()).padStart(2, "0"), mon: MONTHS[d.getUTCMonth()], dow: WEEKDAYS[d.getUTCDay()], ms }
}

function isoDay(d: Date): string {
  return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0")
}

function memberNumber(name: string, email: string): string {
  return "#" + String(100 + (hashStr(name.trim().toLowerCase() + "|" + email.trim().toLowerCase()) % 9900)).padStart(4, "0")
}

function firstName(name: string): string {
  return name.trim().split(/\s+/)[0] ?? ""
}

// "1,840+" → { prefix: "", value: 1840, decimals: 0, suffix: "+" }
function splitStat(s: string): { prefix: string; value: number; decimals: number; suffix: string } | null {
  const m = s.match(/^(\D*?)(\d[\d,]*(?:\.\d+)?)(.*)$/)
  if (!m) return null
  const num = m[2].replace(/,/g, "")
  const dot = num.indexOf(".")
  return { prefix: m[1], value: parseFloat(num), decimals: dot < 0 ? 0 : num.length - dot - 1, suffix: m[3] }
}

function formatStat(s: string, t: number): string {
  const p = splitStat(s)
  if (!p) return s
  const v = p.value * easeOutCubic(t)
  const grouped = p.value >= 1000 && p.decimals === 0
  const body = grouped ? Math.round(v).toLocaleString("en-US") : v.toFixed(p.decimals)
  return p.prefix + body + p.suffix
}

// Keeps a followed angle continuous, so a CSS transition never spins the long way.
function unwrapAngle(prev: number, next: number): number {
  let d = (next - prev) % 360
  if (d > 180) d -= 360
  if (d < -180) d += 360
  return prev + d
}
// #endregion logic

/* ---------------------------------------------------------------- palettes */

const PALETTES: { [K in AstroPaletteName]: AstroPalette } = {
  cobalt: {
    name: "Cobalt",
    accent: "#2233ff",
    frame: "#8d9cff",
    colors: ["#2f47ff", "#1a2cff", "#3d63ff", "#1e88ff", "#27b4f5", "#12d0ff", "#1fe3c0", "#41ef96", "#7f93ff", "#b5c3ff", "#dfe6ff", "#0f1fc4", "#5a46ff"],
  },
  aurora: {
    name: "Aurora",
    accent: "#00876a",
    frame: "#86dcc2",
    colors: ["#00a884", "#007a63", "#00c49a", "#25e0a7", "#7ef5c4", "#00b3c7", "#1f8fff", "#a8f0d8", "#d9fff1", "#00594a", "#5be37f", "#c6f86a"],
  },
  nebula: {
    name: "Nebula",
    accent: "#6a22ff",
    frame: "#c3a8ff",
    colors: ["#6f3cff", "#4b1fd6", "#8f5bff", "#c04dff", "#ff4fd2", "#ff8ad9", "#3d6bff", "#b8a4ff", "#ead9ff", "#2a118f", "#ff6f91", "#7cd3ff"],
  },
  solar: {
    name: "Solar",
    accent: "#e23d00",
    frame: "#ffb48c",
    colors: ["#ff5a1f", "#e23d00", "#ff7a00", "#ffa400", "#ffd23f", "#ff3d6e", "#ff8f6b", "#ffe08a", "#fff1cc", "#b52a00", "#ff5fa2", "#ffc2a1"],
  },
}

const FONT_DISPLAY = '"Poppins","Montserrat","Gilroy","Avenir Next","Century Gothic","Futura",ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,sans-serif'
const FONT_MONO = '"JetBrains Mono","IBM Plex Mono",ui-monospace,"SF Mono",Menlo,Consolas,monospace'

/* ---------------------------------------------------------------- defaults */

const D_HERO_BRAND = "ASTRO."

/* -------------------------------------------------------------- component */

export default function AstroBubbleHero({
  brand = D_HERO_BRAND,
  wordmark = "Astro",
  issue = "#102",
  title = "ASTRO.\nASSOCiATION",
  blurb = "Interplanetary exploration can be carried out by many means, from spacecraft and telescopes to a folding chair in a dark field. We do the last part together, every clear night.",
  credit = "Astro Association · Project designed by its members",
  primaryCta = "Join the association",
  onPrimary,
  secondaryCta = "Tonight's sky",
  onSecondary,
  hint = "Click the bubbles. They pop.",
  onPop,
  seed = 102,
  height = "100svh",
  palette = "cobalt",
  defaultTheme = "system",
  onThemeChange,
  frame = true,
  fonts,
  maxWidth = "1240px",
  className = "",
}: AstroBubbleHeroProps) {
  const reduced = useReducedMotion()
  const { pal, colors, theme } = useAstroLook(palette, defaultTheme, onThemeChange)
  const shellRef = React.useRef(null as HTMLDivElement | null)
  const wide = useWide(shellRef)
  const display = fonts?.display ?? FONT_DISPLAY
  const [pops, setPops] = React.useState(0)
  const popsRef = React.useRef(0)
  const onPopRef = React.useRef(onPop)
  onPopRef.current = onPop
  const handlePop = React.useCallback(() => {
    popsRef.current += 1
    setPops(popsRef.current)
    onPopRef.current?.(popsRef.current)
  }, [])
  const wordMask = React.useMemo(
    () => ({ kind: "text" as const, text: wordmark, font: display, fit: wide ? 0.9 : 1.04, x: wide ? -0.025 : -0.03, baseline: 0.8 }),
    [wordmark, display, wide],
  )

  return (
    <div className={"aa-root" + (frame ? "" : " aa-noframe") + " " + className} data-theme={theme} style={rootStyleOf(pal, fonts, maxWidth, { "--aa-h": height })}>
      <style>{AA_CSS}</style>
      <div className="aa-shell" ref={shellRef}>
        <section className="aa-hero aa-sec" style={{ minHeight: "calc(" + height + " - 24px)" }}>
          <div className="aa-orbits" aria-hidden="true">
            <svg style={{ left: "38%", top: "-30%", width: "90cqi", height: "90cqi" }} viewBox="0 0 100 100" fill="none">
              <g className="aa-orbit-spin">
                <circle cx="50" cy="50" r="49" stroke="currentColor" strokeWidth=".15" />
                <circle cx="99" cy="50" r=".9" fill="var(--aa-blue)" />
              </g>
            </svg>
            <svg style={{ left: "-22%", top: "30%", width: "60cqi", height: "60cqi" }} viewBox="0 0 100 100" fill="none">
              <g className="aa-orbit-spin aa-orbit-spin-r">
                <circle cx="50" cy="50" r="49" stroke="currentColor" strokeWidth=".2" />
                <circle cx="50" cy="1" r="1.4" fill={colors[6]} />
                {Array.from({ length: 14 }).map((_, i) => {
                  const a = Math.PI * 0.62 + i * 0.075
                  return <circle key={i} cx={50 + Math.cos(a) * 44} cy={50 + Math.sin(a) * 44} r={0.35 + (14 - i) * 0.06} fill={colors[(i % (colors.length - 1)) + 1]} opacity={0.4 + (14 - i) / 28} />
                })}
              </g>
            </svg>
          </div>

          <h1 className="aa-sr">{lines(title).join(" ")}</h1>
          <div className="aa-wm">
            <Bubbles seed={seed} colors={colors} density={2} scale={0.4} mask={wordMask} reduced={reduced} onPop={handlePop} />
            <div className="aa-hero-mark">
              <LogoMark size={150} follow reduced={reduced} title={brand + " logo"} />
              {wide && <Title text={title} className="aa-hero-title" />}
            </div>
          </div>

          <div className="aa-in">
            <div className="aa-hero-row">
              <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {issue && <span className="aa-issue-big">{issue}</span>}
                {!wide && <Title text={title} className="aa-hero-title" />}
                {hint && (
                  <span className="aa-hint aa-mono" aria-live="polite">
                    <i className="aa-pop-dot" />
                    {hint}
                    {pops > 0 && <b>{pops} popped</b>}
                  </span>
                )}
              </div>
              <div className="aa-hero-copy">
                <Asterisk size={20} style={{ alignSelf: "flex-start" }} />
                {blurb && <p>{blurb}</p>}
                <div className="aa-ctas">
                  {primaryCta && (
                    <button type="button" className="aa-btn aa-btn-solid" onClick={onPrimary}>
                      {primaryCta}
                      <Arrow />
                    </button>
                  )}
                  {secondaryCta && (
                    <button type="button" className="aa-btn aa-btn-line" onClick={onSecondary}>
                      {secondaryCta}
                    </button>
                  )}
                </div>
                {credit && <div className="aa-credit">{credit}</div>}
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}

/* ---------------------------------------------------------------- helpers */

const useIsoLayoutEffect = typeof window !== "undefined" ? React.useLayoutEffect : React.useEffect

function useReducedMotion(): boolean {
  const [reduced, setReduced] = React.useState(false)
  React.useEffect(() => {
    if (typeof matchMedia !== "function") return
    const mq = matchMedia("(prefers-reduced-motion: reduce)")
    const on = () => setReduced(mq.matches)
    on()
    mq.addEventListener?.("change", on)
    return () => mq.removeEventListener?.("change", on)
  }, [])
  return reduced
}

// true once the element has been on screen
function useInView(threshold = 0.2) {
  const ref = React.useRef(null as HTMLElement | null)
  const [inView, setInView] = React.useState(false)
  React.useEffect(() => {
    const el = ref.current
    if (!el || inView) return
    if (typeof IntersectionObserver !== "function") {
      setInView(true)
      return
    }
    const io = new IntersectionObserver(
      (es) => {
        if (es.some((e) => e.isIntersecting)) {
          setInView(true)
          io.disconnect()
        }
      },
      { threshold },
    )
    io.observe(el)
    return () => io.disconnect()
  }, [inView, threshold])
  return [ref, inView] as const
}

/* ------------------------------------------------------------- the look */

// palette (preset name or your own) and theme ("system" follows the host's
// .dark class, then the OS, until the visitor flips it)
function useAstroLook(palette: AstroPaletteName | AstroPalette, defaultTheme: "system" | "light" | "dark", onThemeChange?: (t: "light" | "dark") => void) {
  const initial = typeof palette === "string" ? PALETTES[palette] ?? PALETTES.cobalt : palette
  const [pal, setPal] = React.useState(initial)
  const palKey = typeof palette === "string" ? palette : palette.name + palette.colors.join("")
  React.useEffect(() => setPal(typeof palette === "string" ? PALETTES[palette] ?? PALETTES.cobalt : palette), [palKey])
  const colors = pal.colors.length ? pal.colors : PALETTES.cobalt.colors
  const [theme, setTheme] = React.useState((defaultTheme === "dark" ? "dark" : "light") as "light" | "dark")
  const [touched, setTouched] = React.useState(false)
  React.useEffect(() => {
    if (defaultTheme !== "system" || touched) {
      if (!touched) setTheme(defaultTheme === "dark" ? "dark" : "light")
      return
    }
    const read = () =>
      setTheme(
        document.documentElement.classList.contains("dark") || (typeof matchMedia === "function" && matchMedia("(prefers-color-scheme: dark)").matches) ? "dark" : "light",
      )
    read()
    const mo = new MutationObserver(read)
    mo.observe(document.documentElement, { attributes: true, attributeFilter: ["class"] })
    const mq = typeof matchMedia === "function" ? matchMedia("(prefers-color-scheme: dark)") : null
    mq?.addEventListener?.("change", read)
    return () => {
      mo.disconnect()
      mq?.removeEventListener?.("change", read)
    }
  }, [defaultTheme, touched])
  const toggleTheme = () => {
    const next = theme === "dark" ? "light" : "dark"
    setTouched(true)
    setTheme(next)
    onThemeChange?.(next)
  }
  return { pal, setPal, colors, theme, toggleTheme }
}

// true when the piece is at least 640px wide (the template's breakpoint)
function useWide(ref: { current: HTMLElement | null }) {
  const [wide, setWide] = React.useState(true)
  useIsoLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const read = () => setWide(el.clientWidth >= 640)
    read()
    if (typeof ResizeObserver !== "function") return
    const ro = new ResizeObserver(read)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  return wide
}

function rootStyleOf(pal: AstroPalette, fonts: { display?: string; body?: string; mono?: string } | undefined, maxWidth: string, extra?: { [k: string]: string }) {
  const display = fonts?.display ?? FONT_DISPLAY
  return {
    "--aa-max": maxWidth,
    "--aa-accent": pal.accent,
    "--aa-frame": pal.frame,
    "--aa-display": display,
    "--aa-body": fonts?.body ?? display,
    "--aa-mono": fonts?.mono ?? FONT_MONO,
    ...extra,
  } as React.CSSProperties
}

/* ------------------------------------------------------------------- logo */

// The mark, in a 100-unit box: a ring, a "( U )" inside it, and an arrow
// that leaves the ring at the north-east like a compass needle. A paper-coloured
// slash runs through the ring under the arrow, so the needle reads as cut in.
const LOGO = {
  ring: "M14 50a36 36 0 1 0 72 0a36 36 0 1 0-72 0",
  left: "M34.4 34.4A22 22 0 0 0 34.4 65.6",
  right: "M65.6 34.4A22 22 0 0 1 65.6 65.6",
  u: "M43.5 41V53a6.5 6.5 0 0 0 13 0V41",
  bar: "M50 41V50",
  // pointing east; rotated -45° by default
  slash: "M8 50H86",
  arrow: "M86 50H98",
  head: "M85 38L98 50L85 62",
}

/* ---------------------------------------------------------------- pieces */

type BubbleMask =
  | { kind: "text"; text: string; font: string; /** fraction of the width the word spans */ fit: number; x: number; baseline: number }
  | { kind: "logo"; cx: number; cy: number; /** logo box, as a fraction of the field's height */ size: number; bar?: boolean }
  | null

type Spark = { x: number; y: number; vx: number; vy: number; r: number; c: number; life: number }

function LogoMark({ size = 40, follow = false, reduced = false, gap = "var(--aa-paper)", className = "", title }: { size?: number; follow?: boolean; reduced?: boolean; gap?: string; className?: string; title?: string }) {
  const svg = React.useRef(null as SVGSVGElement | null)
  const arrow = React.useRef(null as SVGGElement | null)
  React.useEffect(() => {
    if (!follow) return
    let raf = 0
    let deg = -45
    let px = 0
    let py = 0
    const apply = () => {
      raf = 0
      const el = svg.current
      const g = arrow.current
      if (!el || !g) return
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      if (Math.hypot(px - cx, py - cy) < r.width * 0.3) return
      deg = unwrapAngle(deg, (Math.atan2(py - cy, px - cx) * 180) / Math.PI)
      g.style.transform = "rotate(" + deg.toFixed(1) + "deg)"
    }
    const move = (e: PointerEvent) => {
      px = e.clientX
      py = e.clientY
      if (!raf) raf = requestAnimationFrame(apply)
    }
    addEventListener("pointermove", move, { passive: true })
    return () => {
      removeEventListener("pointermove", move)
      cancelAnimationFrame(raf)
    }
  }, [follow])
  return (
    <svg
      ref={svg}
      className={"aa-logo " + className}
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      role={title ? "img" : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
    >
      <g className="aa-logo-spin">
        <path d={LOGO.ring} strokeWidth="9" />
        <path d={LOGO.left} strokeWidth="6.5" />
        <path d={LOGO.right} strokeWidth="6.5" />
        <path d={LOGO.u} strokeWidth="6.5" />
        <path d={LOGO.bar} strokeWidth="5" />
      </g>
      <g ref={arrow} className="aa-logo-arrow" style={{ transform: "rotate(-45deg)", transition: reduced ? "none" : undefined }}>
        <path d={LOGO.slash} strokeWidth="5" stroke={gap} strokeLinecap="butt" style={{ transform: "translate(0,8px)" }} />
        <path d={LOGO.arrow} strokeWidth="8" />
        <path d={LOGO.head} strokeWidth="7" strokeLinejoin="round" />
      </g>
    </svg>
  )
}

function Asterisk({ size = 22, className = "", style }: { size?: number; className?: string; style?: React.CSSProperties }) {
  const [turn, setTurn] = React.useState(0)
  return (
    <button
      type="button"
      className={"aa-ast " + className}
      style={{ ...style, transform: turn ? "rotate(" + turn * 120 + "deg)" : undefined }}
      onClick={() => setTurn((t) => t + 1)}
      aria-label="Spin"
      tabIndex={-1}
    >
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.6" strokeLinecap="round" aria-hidden="true">
        <path d="M12 2.5v19M3.8 7.25l16.4 9.5M3.8 16.75l16.4-9.5" />
      </svg>
    </button>
  )
}

// the last line of a title, kept on one line
function Echo({ text }: { text: string }) {
  return <span className="aa-echo">{text}</span>
}

function Title({ text, className = "", as = "div" }: { text: string; className?: string; as?: "div" | "h1" | "h2" }) {
  const ls = lines(text)
  const Tag = as
  return (
    <Tag className={"aa-title " + className}>
      {ls.map((l, i) => (
        <span key={i}>{i === ls.length - 1 ? <Echo text={l} /> : l}</span>
      ))}
    </Tag>
  )
}

function Arrow({ size = 14 }: { size?: number }) {
  return (
    <svg className="aa-arr" width={size} height={size} viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M3 8h10M9 4l4 4-4 4" />
    </svg>
  )
}

// A seeded, living confetti field on canvas. Bubbles drift, scatter from the
// pointer and pop on click. A mask either keeps only what's inside a word, or
// knocks the logo out of the field so the page shows through.
function Bubbles({
  seed,
  colors,
  density = 1,
  scale = 1,
  mask = null,
  reduced,
  interactive = true,
  onPop,
}: {
  seed: number
  colors: string[]
  density?: number
  scale?: number
  mask?: BubbleMask
  reduced: boolean
  interactive?: boolean
  onPop?: () => void
}) {
  const ref = React.useRef(null as HTMLCanvasElement | null)
  const live = React.useRef({ colors, mask, onPop })
  live.current = { colors, mask, onPop }
  const redraw = React.useRef(() => {})
  const maskKey = mask ? JSON.stringify(mask) : ""

  React.useEffect(() => {
    const cv = ref.current
    const ctx = cv?.getContext("2d")
    if (!cv || !ctx) return
    const host = (cv.parentElement?.parentElement ?? cv.parentElement) as HTMLElement
    const rand = mulberry32(seed ^ 0x5bd1e995)
    let w = 0
    let h = 0
    let dpr = 1
    let list: (Bubble & { ox: number; oy: number; s: number })[] = []
    let sparks: Spark[] = []
    let raf = 0
    let running = false
    let visible = true
    let knock = null as HTMLCanvasElement | null
    const ptr = { x: -1e4, y: -1e4, on: false, dx: 0, dy: 0, down: false }

    const buildKnock = () => {
      const m = live.current.mask
      knock = null
      if (!m || m.kind !== "logo" || typeof Path2D !== "function" || !w || !h) return
      const off = document.createElement("canvas")
      off.width = Math.round(w * dpr)
      off.height = Math.round(h * dpr)
      const o = off.getContext("2d")
      if (!o) return
      const s = (h * m.size) / 100
      o.setTransform(dpr, 0, 0, dpr, 0, 0)
      o.translate(m.cx * w - s * 50, m.cy * h - s * 50)
      o.scale(s, s)
      o.lineCap = "round"
      o.strokeStyle = "#000"
      o.lineWidth = 9
      o.stroke(new Path2D(LOGO.ring))
      o.lineWidth = 6.5
      o.stroke(new Path2D(LOGO.left))
      o.stroke(new Path2D(LOGO.right))
      o.stroke(new Path2D(LOGO.u))
      o.lineWidth = 5
      o.stroke(new Path2D(LOGO.bar))
      o.translate(50, 50)
      o.rotate(-Math.PI / 4)
      o.translate(-50, -50)
      // the slash gives bubbles back
      o.globalCompositeOperation = "destination-out"
      o.lineCap = "butt"
      o.lineWidth = 5
      o.translate(0, 8)
      o.stroke(new Path2D(LOGO.slash))
      o.translate(0, -8)
      o.globalCompositeOperation = "source-over"
      o.lineCap = "round"
      o.lineWidth = 8
      o.stroke(new Path2D(LOGO.arrow))
      o.lineJoin = "round"
      o.lineWidth = 7
      o.stroke(new Path2D(LOGO.head))
      if (m.bar) {
        o.setTransform(dpr, 0, 0, dpr, 0, 0)
        o.fillRect(w * 0.72, h * 0.74, w * 0.3, h * 0.1)
      }
      knock = off
    }

    let textSize = 0
    let textFor = ""
    const draw = (t: number) => {
      const { colors: cols, mask: m } = live.current
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.globalCompositeOperation = "source-over"
      ctx.clearRect(0, 0, w, h)
      ctx.fillStyle = cols[0] ?? "#2233ff"
      ctx.fillRect(0, 0, w, h)
      const ts = t / 1000
      const R = Math.max(70, Math.min(h, w) * 0.34)
      for (const b of list) {
        let tx = 0
        let ty = 0
        if (ptr.on) {
          const dx = b.x + b.ox - ptr.x
          const dy = b.y + b.oy - ptr.y
          const d = Math.hypot(dx, dy) || 1
          const reach = R + b.r
          if (d < reach) {
            const f = 1 - d / reach
            const push = f * f * R * 0.6
            tx = (dx / d) * push + ptr.dx * f * 0.6
            ty = (dy / d) * push + ptr.dy * f * 0.6
          }
        }
        if (reduced) {
          b.ox = tx
          b.oy = ty
          b.s = 1
        } else {
          b.ox += (tx - b.ox) * 0.09
          b.oy += (ty - b.oy) * 0.09
          b.s += (1 - b.s) * 0.07
        }
        const wx = reduced ? 0 : Math.sin(ts * b.sp + b.ph) * b.amp
        const wy = reduced ? 0 : Math.cos(ts * b.sp * 0.8 + b.ph) * b.amp
        ctx.fillStyle = cols[b.c % cols.length]
        ctx.beginPath()
        ctx.arc(b.x + b.ox + wx, b.y + b.oy + wy, Math.max(0.1, b.r * b.s), 0, Math.PI * 2)
        ctx.fill()
      }
      ptr.dx *= 0.85
      ptr.dy *= 0.85
      if (sparks.length) {
        for (const s of sparks) {
          s.x += s.vx
          s.y += s.vy
          s.vx *= 0.94
          s.vy = s.vy * 0.94 - 0.05
          s.life -= 0.022
          if (s.life <= 0) continue
          ctx.fillStyle = cols[s.c % cols.length]
          ctx.beginPath()
          ctx.arc(s.x, s.y, s.r * s.life, 0, Math.PI * 2)
          ctx.fill()
        }
        sparks = sparks.filter((s) => s.life > 0)
      }
      if (m && m.kind === "text") {
        const key = m.font + "|" + m.text + "|" + w + "|" + h + "|" + m.fit
        if (key !== textFor) {
          ctx.font = "800 100px " + m.font
          const mw = ctx.measureText(m.text).width || 1
          textSize = Math.min((100 * w * m.fit) / mw, h * 1.32)
          textFor = key
        }
        ctx.globalCompositeOperation = "destination-in"
        ctx.fillStyle = "#000"
        ctx.font = "800 " + textSize.toFixed(1) + "px " + m.font
        ctx.textBaseline = "alphabetic"
        ctx.fillText(m.text, w * m.x, h * m.baseline)
      } else if (knock) {
        ctx.setTransform(1, 0, 0, 1, 0, 0)
        ctx.globalCompositeOperation = "destination-out"
        ctx.drawImage(knock, 0, 0)
      }
    }

    const loop = (t: number) => {
      draw(t)
      raf = requestAnimationFrame(loop)
    }
    const start = () => {
      if (running || reduced || !visible || !w) return
      running = true
      raf = requestAnimationFrame(loop)
    }
    const stop = () => {
      running = false
      cancelAnimationFrame(raf)
    }
    redraw.current = () => {
      buildKnock()
      if (!running) draw(performance.now())
    }

    const resize = () => {
      // layout size, not the transformed box: a rotated ribbon must not stretch its bubbles
      if (!cv.clientWidth || !cv.clientHeight) return
      w = cv.clientWidth
      h = cv.clientHeight
      dpr = Math.min(2, devicePixelRatio || 1)
      cv.width = Math.round(w * dpr)
      cv.height = Math.round(h * dpr)
      list = buildBubbles(seed, w, h, live.current.colors.length, density, scale).map((b) => ({ ...b, ox: 0, oy: 0, s: 1 }))
      textFor = ""
      buildKnock()
      draw(performance.now())
      start()
    }

    const local = (e: PointerEvent) => {
      const r = cv.getBoundingClientRect()
      return [e.clientX - r.left, e.clientY - r.top] as const
    }
    const onMove = (e: PointerEvent) => {
      const [x, y] = local(e)
      if (ptr.on) {
        ptr.dx = clamp(x - ptr.x, -40, 40)
        ptr.dy = clamp(y - ptr.y, -40, 40)
      }
      ptr.x = x
      ptr.y = y
      ptr.on = x >= -40 && y >= -40 && x <= w + 40 && y <= h + 40
      if (!running) draw(performance.now())
    }
    const onLeave = () => {
      ptr.on = false
      if (!running) draw(performance.now())
    }
    let downAt = [0, 0]
    const onDown = (e: PointerEvent) => {
      downAt = [e.clientX, e.clientY]
    }
    const onUp = (e: PointerEvent) => {
      if (Math.hypot(e.clientX - downAt[0], e.clientY - downAt[1]) > 6) return
      if ((e.target as HTMLElement | null)?.closest?.("a,button,input,label,select,textarea")) return
      const [x, y] = local(e)
      if (x < 0 || y < 0 || x > w || y > h) return
      // only what you can see pops: the pixel under the pointer must be painted
      const px = ctx.getImageData(Math.round(x * dpr), Math.round(y * dpr), 1, 1).data
      if (px[3] < 10) return
      for (let i = list.length - 1; i >= 0; i--) {
        const b = list[i]
        if (Math.hypot(b.x + b.ox - x, b.y + b.oy - y) > b.r * b.s + 2) continue
        const n = live.current.colors.length
        const old = b.c
        b.c = 1 + ((b.c + Math.floor(rand() * (n - 2))) % Math.max(1, n - 1))
        b.s = reduced ? 1 : 0
        if (!reduced) {
          for (let k = 0; k < 9; k++) {
            const a = (k / 9) * Math.PI * 2 + rand() * 0.5
            const sp = 2 + rand() * 4
            sparks.push({ x: b.x + b.ox, y: b.y + b.oy, vx: Math.cos(a) * sp, vy: Math.sin(a) * sp, r: b.r * (0.18 + rand() * 0.22), c: old, life: 1 })
          }
        }
        live.current.onPop?.()
        if (!running) draw(performance.now())
        break
      }
    }

    resize()
    const ro = typeof ResizeObserver === "function" ? new ResizeObserver(resize) : null
    ro?.observe(cv)
    const io =
      typeof IntersectionObserver === "function"
        ? new IntersectionObserver((es) => {
            visible = es.some((e) => e.isIntersecting)
            if (visible) start()
            else stop()
          })
        : null
    io?.observe(cv)
    const vis = () => (document.hidden ? stop() : start())
    document.addEventListener("visibilitychange", vis)
    if (interactive) {
      host.addEventListener("pointermove", onMove, { passive: true })
      host.addEventListener("pointerleave", onLeave)
      host.addEventListener("pointerdown", onDown)
      host.addEventListener("pointerup", onUp)
    }
    // a webfont the host loads later changes the word's width
    const fonts = (document as Document & { fonts?: { ready?: Promise<unknown> } }).fonts
    let alive = true
    fonts?.ready?.then(() => {
      if (!alive) return
      textFor = ""
      if (!running) draw(performance.now())
    })
    return () => {
      alive = false
      stop()
      ro?.disconnect()
      io?.disconnect()
      document.removeEventListener("visibilitychange", vis)
      host.removeEventListener("pointermove", onMove)
      host.removeEventListener("pointerleave", onLeave)
      host.removeEventListener("pointerdown", onDown)
      host.removeEventListener("pointerup", onUp)
    }
  }, [seed, density, scale, reduced, interactive])

  // palette swaps and mask changes repaint without reshuffling
  React.useEffect(() => {
    redraw.current()
  }, [colors, maskKey])

  return (
    <div className="aa-bub" aria-hidden="true">
      <canvas ref={ref} />
    </div>
  )
}

/* -------------------------------------------------------------------- css */

const AA_CSS = `
.aa-root{--aa-accent:#2233ff;--aa-frame:#8d9cff;--aa-paper:#ffffff;--aa-ink:#0b0c16;--aa-soft:#2b2e45;--aa-muted:#6b7090;--aa-card:#f5f6ff;--aa-moon:#e6e9ff;--aa-blue:var(--aa-accent);--aa-line:color-mix(in srgb,var(--aa-accent) 16%,transparent);--aa-line-strong:color-mix(in srgb,var(--aa-accent) 34%,transparent);--aa-display:"Poppins",sans-serif;--aa-body:var(--aa-display);--aa-mono:ui-monospace,monospace;position:relative;width:100%;box-sizing:border-box;background:var(--aa-paper);color:var(--aa-ink);font-family:var(--aa-body);font-size:15px;line-height:1.5;-webkit-font-smoothing:antialiased;border:clamp(6px,1.1vw,12px) solid var(--aa-frame);transition:background-color .45s ease,color .45s ease,border-color .45s ease;overflow:clip}
.aa-root[data-theme="dark"]{--aa-paper:#060821;--aa-ink:#f2f4ff;--aa-soft:#c8cdf2;--aa-muted:#8a90bd;--aa-card:#0c1033;--aa-moon:#1a1f4d;--aa-blue:color-mix(in srgb,var(--aa-accent) 52%,#ffffff);--aa-line:color-mix(in srgb,var(--aa-blue) 18%,transparent);--aa-line-strong:color-mix(in srgb,var(--aa-blue) 36%,transparent);border-color:color-mix(in srgb,var(--aa-frame) 45%,#060821)}
.aa-root :where(*){box-sizing:border-box}
.aa-root ::selection{background:var(--aa-blue);color:var(--aa-paper)}
.aa-root :focus-visible{outline:2px solid var(--aa-blue);outline-offset:3px}
.aa-root :where(button){font:inherit;color:inherit;background:none;border:0;padding:0;margin:0;cursor:pointer;text-align:inherit;letter-spacing:inherit}
.aa-root :where(a){color:inherit;text-decoration:none}
.aa-root :where(svg){display:block;max-width:none;flex:none}
.aa-root :where(canvas){display:block;max-width:none}
.aa-root :where(h1,h2,h3,h4,p,ul,ol,li,dl,dd,figure){margin:0;padding:0;font-size:inherit;font-weight:inherit;list-style:none}
.aa-root :where(input){font:inherit;color:inherit;margin:0}
.aa-sr{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}
.aa-shell{width:100%;container-type:inline-size;position:relative}
.aa-in{width:100%;max-width:var(--aa-max,1240px);margin:0 auto;padding:0 clamp(16px,4cqi,48px)}
.aa-disp{font-family:var(--aa-display);font-weight:800;letter-spacing:-.02em;line-height:.86;text-transform:none}
.aa-mono{font-family:var(--aa-mono);font-size:11px;letter-spacing:.06em;text-transform:uppercase}
.aa-label{font-family:var(--aa-display);font-weight:800;font-size:11px;letter-spacing:.04em;text-transform:uppercase;color:var(--aa-blue)}
.aa-echo{display:inline-block;white-space:nowrap}
.aa-title{display:flex;flex-direction:column;font-family:var(--aa-display);font-weight:800;letter-spacing:-.01em;line-height:.9;text-transform:none}
.aa-title>span:last-child{padding-bottom:.42em}
.aa-noframe{border:0}
.aa-sticky{position:sticky;top:0;z-index:40;overflow:visible}
.aa-btn{position:relative;display:inline-flex;align-items:center;justify-content:center;gap:9px;padding:13px 20px;border-radius:999px;font-family:var(--aa-display);font-weight:800;font-size:13px;letter-spacing:.03em;text-transform:uppercase;line-height:1;white-space:nowrap;transition:background-color .2s,color .25s,box-shadow .25s,transform .25s cubic-bezier(.2,.8,.2,1)}
.aa-btn-solid{background:var(--aa-blue);color:var(--aa-paper);box-shadow:0 10px 24px -14px var(--aa-blue)}
.aa-btn-solid:hover{transform:translateY(-2px);box-shadow:0 16px 30px -14px var(--aa-blue)}
.aa-btn-line{color:var(--aa-blue);box-shadow:inset 0 0 0 2px var(--aa-blue)}
.aa-btn-line:hover{background:var(--aa-blue);color:var(--aa-paper)}
.aa-btn-ink{background:var(--aa-ink);color:var(--aa-paper)}
.aa-btn-ink:hover{transform:translateY(-2px)}
.aa-btn:active{transform:scale(.97)}
.aa-btn[disabled]{opacity:.55;cursor:default;transform:none}
.aa-btn .aa-arr{transition:transform .25s cubic-bezier(.2,.8,.2,1)}
.aa-btn:hover .aa-arr{transform:translateX(3px)}
.aa-nav .aa-btn{padding:11px 16px;font-size:12px}
.aa-bub{position:absolute;inset:0}
.aa-bub canvas{position:absolute;left:0;top:0;width:100%;height:100%}
.aa-ast{display:inline-grid;place-items:center;color:var(--aa-blue);transition:transform .8s cubic-bezier(.2,.8,.2,1)}
.aa-ast:hover{transform:rotate(60deg) scale(1.15)}
.aa-logo{overflow:visible}
.aa-logo-arrow{transform-box:view-box;transform-origin:50px 50px;transition:transform .6s cubic-bezier(.2,.8,.2,1)}
.aa-logo-spin{transform-box:view-box;transform-origin:50px 50px;transition:transform .9s cubic-bezier(.2,.8,.2,1)}
.aa-brand:hover .aa-logo-spin{transform:rotate(-360deg)}
.aa-sec{position:relative;scroll-margin-top:62px}
.aa-pad{padding:clamp(48px,8cqi,104px) 0}
.aa-reveal{opacity:0;transform:translateY(22px);transition:opacity .9s cubic-bezier(.2,.7,.2,1),transform .9s cubic-bezier(.2,.7,.2,1)}
.aa-reveal[data-in="true"]{opacity:1;transform:none}
.aa-kicker{display:flex;flex-direction:column;align-items:center;gap:14px;padding:clamp(18px,3cqi,30px) 0 clamp(26px,4cqi,44px)}
.aa-kicker .aa-label{font-size:clamp(12px,1.3cqi,15px)}
.aa-meta{display:flex;flex-wrap:wrap;justify-content:space-between;gap:8px 20px;padding:10px 0;border-top:1px solid var(--aa-line-strong)}
.aa-meta span{font-size:10px}
.aa-h2{font-family:var(--aa-display);font-weight:800;font-size:clamp(30px,5cqi,62px);line-height:.98;letter-spacing:-.03em}
.aa-lede{font-size:clamp(15px,1.5cqi,17px);color:var(--aa-soft);line-height:1.65;max-width:56ch}
.aa-mission{display:grid;gap:clamp(28px,5cqi,64px);padding-top:clamp(32px,5cqi,64px)}
.aa-stats{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0;border-top:1.5px solid var(--aa-ink)}
.aa-stat{padding:18px 4px 18px 0;border-bottom:1px solid var(--aa-line-strong)}
.aa-stat dt{font-family:var(--aa-display);font-weight:800;font-size:clamp(32px,4.6cqi,56px);line-height:1;letter-spacing:-.03em;color:var(--aa-blue);font-variant-numeric:tabular-nums}
.aa-stat dd{margin-top:6px}
@container (min-width:860px){.aa-mission{grid-template-columns:1.1fr 1fr;align-items:start}}
.aa-orbits{position:absolute;inset:0;pointer-events:none;color:var(--aa-line-strong)}
.aa-orbits svg{position:absolute}
.aa-orbit-spin{transform-box:view-box;transform-origin:50% 50%;animation:aa-spin 60s linear infinite}
.aa-orbit-spin-r{animation-duration:90s;animation-direction:reverse}
.aa-hero{position:relative;display:flex;flex-direction:column;justify-content:center;min-height:calc(var(--aa-h,100svh) - 74px);padding:clamp(14px,2.4cqi,30px) 0 clamp(24px,4cqi,48px);scroll-margin-top:62px;overflow:hidden}
.aa-wm{position:relative;height:clamp(200px,40cqi,520px);margin:0 calc(-1 * clamp(4px,1cqi,14px));cursor:crosshair;touch-action:pan-y}
.aa-hero-mark{position:absolute;right:clamp(16px,4cqi,56px);top:clamp(4px,1.4cqi,20px);display:flex;flex-direction:column;align-items:flex-end;gap:clamp(6px,1cqi,12px);pointer-events:none;z-index:2}
.aa-hero-mark .aa-logo{pointer-events:auto;color:var(--aa-ink);width:clamp(64px,13cqi,170px);height:auto}
.aa-hero-title{font-size:clamp(22px,4.6cqi,60px);color:var(--aa-ink);text-align:left}
.aa-hero-title>span{background:var(--aa-paper);padding:0 .08em;box-decoration-break:clone;-webkit-box-decoration-break:clone}
.aa-hero-row{position:relative;display:grid;gap:clamp(18px,3cqi,32px);align-items:end;margin-top:clamp(-56px,-4cqi,-8px);z-index:3}
.aa-issue-big{font-family:var(--aa-display);font-weight:800;font-size:clamp(52px,11cqi,150px);line-height:.8;letter-spacing:-.04em;color:var(--aa-ink);background:var(--aa-paper);justify-self:start;padding:.06em .1em 0 0}
.aa-hero-copy{display:flex;flex-direction:column;gap:16px;max-width:520px}
.aa-hero-copy p{font-size:clamp(14px,1.35cqi,16px);color:var(--aa-soft);line-height:1.6}
.aa-ctas{display:flex;flex-wrap:wrap;gap:10px}
.aa-credit{display:flex;align-items:center;gap:12px;padding-top:10px;border-top:1.5px solid var(--aa-ink);font-family:var(--aa-display);font-weight:800;font-size:10.5px;letter-spacing:.04em;text-transform:uppercase}
.aa-hint{display:inline-flex;align-items:center;gap:8px;color:var(--aa-muted)}
.aa-hint b{color:var(--aa-blue);font-weight:700}
.aa-pop-dot{width:7px;height:7px;border-radius:99px;background:var(--aa-blue);animation:aa-blink 1.6s ease-in-out infinite}
@container (min-width:760px){.aa-hero-row{grid-template-columns:auto 1fr;column-gap:clamp(24px,5cqi,80px)}.aa-hero-copy{justify-self:end}}
@keyframes aa-spin{to{transform:rotate(360deg)}}
@keyframes aa-marq{to{transform:translateX(-50%)}}
@keyframes aa-blink{0%,100%{opacity:1}50%{opacity:.25}}
@keyframes aa-drop{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:none}}
@keyframes aa-rise{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:none}}
@keyframes aa-print{0%{transform:translateY(16px) scale(.96);filter:brightness(1.6)}100%{transform:none;filter:none}}
@media (prefers-reduced-motion:reduce){
.aa-root,.aa-root :where(*){scroll-behavior:auto}
.aa-reveal{opacity:1;transform:none;transition:none}
.aa-marq,.aa-orbit-spin,.aa-pop-dot,.aa-chip i,.aa-event,.aa-printed,.aa-mobile{animation:none}
.aa-logo-arrow,.aa-logo-spin,.aa-rib,.aa-card,.aa-card-fill,.aa-member,.aa-ast,.aa-bar i,.aa-more{transition:none}
}
`
