# Material Files Integrated Editor V4

## Editor fixes and redesign

- Fixed the three build-time source issues permanently:
  - `Symbol.insert` now defaults to `label`.
  - `TextEditorPageGuideView` uses `android.R.attr.textColorSecondary`.
  - `readAllBytes` import uses the correct plural function name.
- Keyboard symbol row is now placed above the main editor toolbar so it expands upward.
- Symbol buttons no longer use filled `MaterialButton` backgrounds; they are transparent Material-style text controls with theme-aware ripple feedback.
- Toolbar actions are created dynamically and use Google Material icon names/designs (`undo`, `redo`, `content_cut`, `content_copy`, `content_paste`, `select_all`, `format_indent_increase`, `format_indent_decrease`, `comment`, `search`, `keyboard_arrow_up/down`).
- Added a dedicated Editor toolbar settings screen. Users can enable/disable actions and drag them into any order. The order and enabled set are persisted.
- Split settings into separate `Code editor` and `Text editor` screens.
- Plain text (`txt`, `log`, `csv`, `tsv`, and generic text files without a recognized code extension) now opens through a dedicated `PlainTextEditorActivity` entry point.
- Plain-text mode is document-oriented: no syntax highlighting, no line numbers, no sticky scroll, no current-line highlight, no gutter divider, word wrap enabled by default, proportional font and wider line spacing.
- Code mode retains Sora/TextMate, line numbers and developer-oriented behavior.

## Resource synchronization formatting

`TextCompareSynchronizer` now rebuilds the target resource body without `join("\\n\\n")`.
Existing translations are preserved verbatim, target indentation is preserved, and the target file's existing number of blank-line separators is retained. This prevents the previous overwrite from stripping indentation and inserting blank lines after almost every resource.

## PDF/DOCX

The project already has a PDF viewer. V4 deliberately does not add a half-finished PDF writer or DOCX editor yet. The editor-mode split and settings architecture are now ready for a separate document viewer/editor phase.

## Build

Build on the existing DroidSpaces ARM64 environment:

```bash
cd /root/MaterialFiles-Integrated-Editor-v4
./gradlew clean assembleDebug
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`
