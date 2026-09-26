# Material Files - Integrated Editor V3

## V3 fixes / changes

### Build fixes
- `CreateFileDialogFragment.onInflateBinding()` returns `NameDialogFragment.Binding`.
- `CreateFileDialogFragment` constructs `NameDialogFragment.Binding`, not `FileNameDialogFragment.Binding`.
- `NameDialogFragment.Binding` has a public constructor.
- `TextEditorFragment.kt` imports `me.zhanghai.android.files.provider.common.readAllBytes`.
- Sora Editor 0.24.6 cursor access uses `getLeftLine()` / `getRightLine()`; old `cursor.left.line` / `cursor.right.line` usages are removed.

### Text editor crash fix
- Font size is stored by `SimpleMenuPreference` as a String and is now read safely with `getString(...).toFloatOrNull()`.
- A legacy Float value is also accepted for compatibility.
- TextMate grammar/theme initialization is guarded. If initialization fails, the editor falls back to plain text instead of crashing.
- If TextMate is unavailable, `EmptyLanguage()` is used.

### Settings navigation
- Text editor settings are no longer embedded as a category inside the main Settings list.
- A `Text editor` preference was added under **Behavior**, alongside Default folder / Standard directories / Bookmark directories.
- Tapping it opens a dedicated `TextEditorSettingsActivity` page containing all text editor options.
- The editor's three-dot Settings action opens this dedicated page directly.

### Editor UI
- The editor toolbar `+` uses the theme-aware `add_icon_control_normal_24dp` instead of the white icon.
- Symbol row, A4/plain-text page guide, syntax highlighting and other V3 editor features remain enabled.

## Build

Build in DroidSpaces/Ubuntu ARM64:

```bash
cd /root/MaterialFiles-Integrated-Editor-v3
./gradlew clean assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```
