# Graph Report - AppReed SOLOMON  (2026-10-06)

## Corpus Check
- Corpus is ~21,372 words - fits in a single context window. You may not need a graph.

## Summary
- 473 nodes · 1134 edges · 20 communities (11 shown, 9 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 9 edges (avg confidence: 0.82)
- Token cost: 1,400 input · 2,500 output

## Community Hubs (Navigation)
- RS engine and traces
- Message codec and config
- Neon UI components
- Encode decode screens
- Sound and app state
- RS engine tests
- Navigation and theme
- Finite field arithmetic
- Decoding view model
- Neon design system
- Info screen
- Encoding view model
- App entry and theme
- Project documentation
- Status banners

## God Nodes (most connected - your core abstractions)
1. `RSConfig` - 49 edges
2. `ReedSolomon` - 39 edges
3. `GFPoly` - 33 edges
4. `ReedSolomonTest` - 30 edges
5. `CodecTest` - 22 edges
6. `DecodingScreen()` - 20 edges
7. `AppViewModel` - 19 edges
8. `DecodingViewModel` - 19 edges
9. `MessageCodec` - 18 edges
10. `EncodingScreen()` - 16 edges

## Surprising Connections (you probably didn't know these)
- `ReedSolomonApp()` --calls--> `NeonBackground()`  [EXTRACTED]
  app/src/main/java/com/example/reedsolomonlab/ReedSolomonApp.kt → app/src/main/java/com/example/reedsolomonlab/ui/components/NeonComponents.kt
- `ReedSolomonApp()` --calls--> `DecodingScreen()`  [EXTRACTED]
  app/src/main/java/com/example/reedsolomonlab/ReedSolomonApp.kt → app/src/main/java/com/example/reedsolomonlab/ui/decoding/DecodingScreen.kt
- `ReedSolomonApp()` --calls--> `EncodingScreen()`  [EXTRACTED]
  app/src/main/java/com/example/reedsolomonlab/ReedSolomonApp.kt → app/src/main/java/com/example/reedsolomonlab/ui/encoding/EncodingScreen.kt
- `ReedSolomonApp()` --calls--> `InfoScreen()`  [EXTRACTED]
  app/src/main/java/com/example/reedsolomonlab/ReedSolomonApp.kt → app/src/main/java/com/example/reedsolomonlab/ui/info/InfoScreen.kt
- `ReedSolomonApp()` --references--> `AppViewModel`  [EXTRACTED]
  app/src/main/java/com/example/reedsolomonlab/ReedSolomonApp.kt → app/src/main/java/com/example/reedsolomonlab/viewmodel/AppViewModel.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Decode pipeline flow** — readme_decodificar_tab, readme_decoding_pipeline, readme_gf_256, readme_educationalstep_trace [EXTRACTED 1.00]
- **Three-tab app structure** — readme_rs_com2026, readme_codificar_tab, readme_decodificar_tab, readme_mas_info_tab [EXTRACTED 1.00]

## Communities (20 total, 9 thin omitted)

### Community 0 - "RS engine and traces"
Cohesion: 0.06
Nodes (40): EducationalStep, StepType, CORRECTION, DATA, ERROR, ERROR_LOCATION, INFO, MATH (+32 more)

### Community 1 - "Message codec and config"
Cohesion: 0.12
Nodes (8): Err, MessageCodec, Ok, ParseResult, SymbolList, SymbolListResult, RSConfig, CodecTest

### Community 2 - "Neon UI components"
Cohesion: 0.06
Nodes (4): toHex(), pseudo(), stepAccent(), symbolColors()

### Community 4 - "Sound and app state"
Cohesion: 0.08
Nodes (12): Sound, CLICK, CORRECTED, ENCODE_DONE, ERROR_DETECTED, NAVIGATE, SUCCESS, SoundManager (+4 more)

### Community 6 - "Navigation and theme"
Cohesion: 0.07
Nodes (3): ReedSolomonApp(), Tab, RsColors

### Community 8 - "Decoding view model"
Cohesion: 0.10
Nodes (15): ConfigSource, DETECTED, HEADER, IMPORTED, NONE, DecoderInputMode, BINARIO, SIMBOLOS (+7 more)

### Community 9 - "Neon design system"
Cohesion: 0.20
Nodes (18): AmbientBackground(), AnimatedSection(), GhostButton(), GlowCard(), NeonBackground(), NeonButton(), ParticleLayer(), SectionHeader() (+10 more)

### Community 10 - "Info screen"
Cohesion: 0.12
Nodes (6): AppRow(), ConceptRow(), FlowDiagram(), InfoParagraph(), InfoScreen(), SettingsRow()

### Community 11 - "Encoding view model"
Cohesion: 0.11
Nodes (9): MessageType, BINARIO, TEXTO, SymbolRepresentation, BINARIO, DECIMAL, HEXADECIMAL, EncodingUiState (+1 more)

### Community 13 - "Project documentation"
Cohesion: 0.20
Nodes (9): CODIFICAR tab, DECODIFICAR tab, Decode pipeline sindromes-BM-Chien-Forney, EducationalStep trace, GF(256) finite field, MAS INFO tab, RS-COM2026 app, Shareable numbers-only format (+1 more)

### Community 15 - "Status banners"
Cohesion: 0.40
Nodes (5): BannerType, ERROR, INFO, SUCCESS, WARNING

## Knowledge Gaps
- **45 isolated node(s):** `CLICK`, `NAVIGATE`, `ENCODE_DONE`, `ERROR_DETECTED`, `CORRECTED` (+40 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 145 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ReedSolomon` connect `RS engine and traces` to `Message codec and config`, `Encode decode screens`, `RS engine tests`, `Finite field arithmetic`, `Decoding view model`, `Encoding view model`?**
  _High betweenness centrality (0.234) - this node is a cross-community bridge._
- **What connects `CLICK`, `NAVIGATE`, `ENCODE_DONE` to the rest of the system?**
  _45 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `RS engine and traces` be split into smaller, more focused modules?**
  _Cohesion score 0.05627545353572751 - nodes in this community are weakly interconnected._
- **Why does `RSConfig` connect `Message codec and config` to `RS engine and traces`, `Neon UI components`, `Sound and app state`, `RS engine tests`, `Finite field arithmetic`, `Decoding view model`, `Neon design system`, `Encoding view model`?**
  _High betweenness centrality (0.219) - this node is a cross-community bridge._
- **Should `Message codec and config` be split into smaller, more focused modules?**
  _Cohesion score 0.12323232323232323 - nodes in this community are weakly interconnected._
- **Why does `SoundManager` connect `Sound and app state` to `Decoding view model`, `Encoding view model`, `Encode decode screens`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Should `Neon UI components` be split into smaller, more focused modules?**
  _Cohesion score 0.059800664451827246 - nodes in this community are weakly interconnected._