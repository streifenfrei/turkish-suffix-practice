# CLAUDE.md — Turkish Suffix Trainer

Project context for Claude Code. Read this before making changes.

## What we're building

An Android app for practicing Turkish suffixes (case endings and tense/mood
markers). The app is a flashcard reviewer:

- Each **card** shows an English translation, the Turkish sentence with the
  target suffix(es) blanked out (e.g. `ev___` for `evde`), and audio.
- A **Settings** tab toggles which grammatical categories are active (e.g.
  locative case on, future tense off). Only suffixes from enabled categories
  get blanked, and cards with no enabled target suffix are filtered out.

Sentences and audio come from **Tatoeba**. Suffix segmentation comes from
**Zemberek** morphological analysis. Both run in an offline build step that
produces a prepackaged SQLite database shipped inside the APK — the app itself
does no scraping and no on-device NLP.

## Tech stack

- **Language:** Kotlin (latest stable). Target/compile SDK = latest stable;
  minSdk 26.
- **UI:** Jetpack Compose + Material 3. Navigation via Navigation-Compose.
- **Architecture:** MVVM. `ViewModel` exposes immutable UI state via
  `StateFlow`; Compose collects with `collectAsStateWithLifecycle`. No business
  logic in composables.
- **Persistence:** Room over a prepackaged SQLite db (`createFromAsset`).
  User progress/settings in a separate writable Room db (or DataStore for
  settings).
- **Audio:** AndroidX **Media3 (ExoPlayer)**. Audio files bundled in assets, or
  streamed from Tatoeba's audio CDN if APK size is a concern (decide early — see
  Open Questions).
- **DI:** Hilt. Keep it light; don't over-abstract.
- **Settings store:** Jetpack DataStore (Preferences).
- **Data pipeline:** a separate **pure-JVM Gradle module** (`:datapipeline`),
  not an Android module, so it can pull in Zemberek and run on the desktop JVM.

Always verify current stable versions before adding dependencies rather than
trusting memory — Compose/Room/Media3 versions move quickly. Check the
product-self-knowledge skill only for Anthropic-specific facts, not these.

## Repository layout

```
:app              Android app (Compose UI, ViewModels, Room access)
:core-model       Shared Kotlin data classes (Sentence, Token, Suffix, Category)
:datapipeline     Pure-JVM module: Tatoeba ingest + Zemberek analysis -> trainer.db
  /src/main/kotlin
  /build/output/trainer.db   <- generated, copied into :app/src/main/assets
```

The pipeline is run manually (or in CI), not on every app build. Its output
`trainer.db` is the committed/cached artifact the app depends on.

## Data model (Room entities)

- **Sentence**: `id`, `turkishText`, `englishText`, `audioPath` (nullable),
  `tatoebaId`.
- **Token**: one row per word in a sentence — `sentenceId`, `position`,
  `surface` (e.g. `evde`), `lemma`/root (e.g. `ev`), `charStart`/`charEnd`.
  (Root and char spans are needed for morphology and blanking respectively.)
- **Suffix**: zero or more per token — `tokenId`, `morpheme` (e.g. `-de`),
  `category` (enum: a grammatical case or a tense/mood), `startInSurface`,
  `endInSurface`. These ranges define what gets blanked.
- **Category** is an enum, not free text. See below.

A "card" is derived, not stored: pick a sentence, find its tokens whose suffixes
belong to currently-enabled categories, blank those character ranges.

### Categories enum (the toggle list)

Cases: `NOMINATIVE`, `ACCUSATIVE`, `DATIVE`, `LOCATIVE`, `ABLATIVE`,
`GENITIVE`, `INSTRUMENTAL`.
Tense/mood: `PRESENT_CONTINUOUS` (-iyor), `AORIST` (-ir/-er),
`PAST_DEFINITE` (-di), `PAST_INFERENTIAL` (-miş), `FUTURE` (-ecek),
`CONDITIONAL` (-se), `NECESSITATIVE` (-meli), `OPTATIVE` (-e).
Start with cases + the five core tenses; add the rest behind the same toggle
mechanism. Every category is independently toggleable in Settings.

## Data pipeline (`:datapipeline`)

1. **Ingest Tatoeba.** Download the CSV exports from tatoeba.org/downloads:
   `sentences.csv` (filter `tur`), the English `eng` rows, `links.csv` to pair
   Turkish↔English, and `sentences_with_audio.csv` for audio availability.
   Prefer the bulk CSV dumps over the live API for the corpus build; the API is
   for ad-hoc lookups only and is rate-limited. Keep raw downloads cached so the
   pipeline is re-runnable offline.
2. **Filter for quality.** Drop sentences that are too long, lack an English
   pair, or fail Zemberek analysis. Aim for a clean, learner-appropriate subset
   rather than the entire corpus.
3. **Analyze with Zemberek.** For each Turkish token, run morphological
   analysis to get the root and the ordered morphemes. Map Zemberek's morpheme
   labels to our `Category` enum, and record the surface character span each
   suffix occupies (needed for accurate blanking). Turkish vowel harmony means
   the *surface* form of a suffix varies (`-de`/`-da`/`-te`/`-ta`) — store the
   actual surface span, not a canonical form.
4. **Resolve ambiguity.** Zemberek may return multiple analyses per word. Use
   its disambiguator with sentence context; if still ambiguous, prefer the
   analysis and log low-confidence tokens for review. Do not silently guess.
5. **Emit `trainer.db`** as a SQLite file matching the Room schema, then copy it
   into `:app/src/main/assets/`.

## UI structure

- **Bottom navigation:** Practice | Settings.
- **Practice screen:** swipeable card stack (or simple next/prev). Card =
  English line, Turkish line with blanks rendered inline, play-audio button.
  A reveal control shows the blanked suffix(es).
- **Settings screen:** grouped toggles — "Cases" and "Tenses & moods" — each row
  a category with a switch. Changes immediately re-filter the practice deck.
- Keep state hoisted; the deck contents are a function of (enabled categories,
  available cards). Recompute the filtered card list in the ViewModel when
  settings change.

## Build & run

```
./gradlew :app:assembleDebug        # build the app
./gradlew :app:installDebug         # install on device/emulator
./gradlew :datapipeline:run         # regenerate trainer.db (do this when corpus changes)
./gradlew test                      # unit tests
./gradlew connectedAndroidTest      # instrumented tests
```

The app build assumes `trainer.db` already exists in assets. Run the pipeline
first on a clean checkout.

## Conventions

- Compose: stateless composables + state hoisting; preview every screen-level
  composable. Material 3 theming via a central `Theme.kt`.
- Coroutines for all I/O; never block the main thread. Audio and db access off
  the main dispatcher.
- One ViewModel per screen. UI state is a single immutable data class per
  screen.
- Keep Zemberek and Tatoeba logic entirely inside `:datapipeline` — the `:app`
  module must not depend on either.
- Tests: pipeline morphology mapping is the highest-value place for unit tests
  (assert known words segment into the expected categories and spans).

## Open questions / decide early

- **Audio: bundle vs. stream.** Bundling thousands of clips bloats the APK;
  streaming needs network and a caching layer. Decide based on target corpus
  size. Leaning toward streaming with a disk cache for a large corpus, bundling
  for a small curated set.
- **Corpus size & curation.** A smaller, hand-filtered set of clear sentences
  beats the full Tatoeba dump for a learning app. Pick a target count.
- **Multi-suffix cards.** When a sentence has several enabled suffixes, blank
  all of them or just one target? Affects difficulty pacing.
- **Spaced repetition.** Out of scope for v1, but the progress db should leave
  room for it (per-card review history) so it's not a rewrite later.
- **Tatoeba licensing.** Sentences are CC-BY; audio licenses vary per
  contributor. Confirm attribution requirements before shipping and include an
  attribution screen.

## Milestones

1. `:datapipeline` ingests Tatoeba tur↔eng pairs and produces a basic
   `trainer.db` (no suffix analysis yet).
2. Add Zemberek analysis → populate `Token` and `Suffix` with category + spans.
3. `:app` Practice screen renders cards from the db with blanking; audio
   playback works.
4. Settings toggles wired to deck filtering.
5. Polish: theming, edge cases, attribution, low-confidence handling.
