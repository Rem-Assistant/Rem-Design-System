# Android Agenda screen — handoff

For the engineer integrating the Android app. The Agenda/Today screen was drifting from the design
because the app was hand-building screens with **Material 3 defaults** instead of composing them from
the **Compose design system** (`compose/RemDesignSystem/`). This branch (`claude/ds-flows`) adds the
missing components and a full worked Agenda screen so you compose it the same way iOS does.

## The one rule

**Compose screens from `com.rem.designsystem` components. Do not hand-roll Material widgets.** Every
place the app uses a raw `Card`, `Chip`, `Icons.Filled.*`, `MaterialTheme.typography`, or a bare
`Text` with ad-hoc `sp`/`Color`, it drifts. The DS owns the look; the app owns the data + navigation.

## Wire it up (once)

1. **Depend on the module.** The DS is an `com.android.library` (`compose/RemDesignSystem`,
   namespace `com.rem.designsystem`). Include it as a module (or consume the AAR once published):
   ```kotlin
   // settings.gradle.kts
   include(":remdesignsystem")
   project(":remdesignsystem").projectDir = file("path/to/compose/RemDesignSystem")
   // app/build.gradle.kts
   implementation(project(":remdesignsystem"))
   ```
2. **Theme at the root.** Wrap the whole app in `RemTheme { }` so tokens cascade. This alone fixes
   most of the "wrong font / wrong spacing" feel — it switches type to **Inter** (not Roboto) and makes
   `RemColors.current` resolve for every DS component.
   ```kotlin
   setContent { RemTheme { AppRoot() } }
   ```
3. **Fonts + icons ship in the AAR.** Inter and the Material Symbols fonts are bundled (there's a
   `verifyMaterialSymbolResources` build gate so a release can't ship tofu). Use the DS components'
   icons; don't add your own `Icons.Filled.*` set.

## Build the Agenda screen from these components

The full worked screen is `AgendaScreenTodayPreview` in
`compose/RemDesignSystem/screens/AgendaScreen.kt` — open it in Android Studio's preview and copy its
structure. Component map, top to bottom:

| Screen element | DS component | Notes |
|---|---|---|
| Date header (`‹ chevron + dashes` · Today · `dashes + chevron ›`) | `DateNavigationHeader` | the inward dashes are **by design** (Figma `43:2`), not tofu |
| Daily brief | `DailyBriefCard` | **uncontained** — no grey card, no "DAILY BRIEF" label; title + prose (+ count capsules only when prose is absent) |
| Task / event rows | `TaskEventRow` | `kind = Task` / `Event(color)`, `leading`, `pills` via `RemPill` |
| "Add a task" field | `AddTaskField` | rounded field + brandBlue **Add** |
| Suggestions | `SuggestionSection` + `SuggestedTaskRow` | header is sentence-case **"Suggestions"**, bounded to 3 rows + "See more" |

The whole thing lives inside `AgendaScreen(dateText, onPrevious, onNext) { ...content... }`.

## The four drift fixes (what the current Android build got wrong)

These are the specific places the screenshot diverged from the design-system source of truth:

1. **Theme not applied** → type reads as Roboto/Material, spacing is off. Fix: `RemTheme` at the root.
2. **Daily brief in a grey card with a "DAILY BRIEF" header** → it should be **uncontained** (inline
   summary), with the brief's own title and no section label. Use `DailyBriefCard`.
3. **Suggestions as "title + trailing [Add] [Dismiss] text"** → the DS treatment is a **left Add/Move
   CTA + a dashed ring + an ✕ dismiss** (`SuggestedTaskRow`). The dashed ring is the "proposed, not
   yet real" signal.
4. **Uppercased section labels** ("SUGGESTIONS") → headers are **sentence case** ("Suggestions"), per
   the shared section component.

## Self-check (don't eyeball it)

- **Previews:** every DS component has an `@Preview`. The Agenda one is `AgendaScreen — Today`.
- **Paparazzi:** the module has `app.cash.paparazzi` — run the snapshot tests to get light+dark PNGs
  and diff them against the iOS renders (the `screenshots.yml` paired-render gate). That's the
  objective "does it match" signal; use it instead of comparing by eye.
- App chrome **not** in the DS: the bottom tab bar (menu · chat · +) and the `update_required` banner
  are app-level — theme them with `RemColors`/`RemTypography`, but they aren't DS components.

## Known follow-ups (DS side, not blocking you)

- Code Connect (`.figma.kt`) for the new components (`DailyBriefCard`, `SuggestedTaskRow`,
  `SuggestionSection`) — dormant-but-ready pattern, same as `ContainedIcon.figma.kt`.
- Icon convergence: the extracted row components still use Material Icons; the target is the DS's
  `RemMaterialSymbols` font so iOS/Android glyphs never drift. New components follow the existing
  Material-Icons pattern for now.
