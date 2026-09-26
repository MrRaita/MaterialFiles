# Material Files Integrated Editor V4.1

## Fixes
- Fixed the editor-toolbar settings page crash: the RecyclerView now has a LinearLayoutManager.
- Replaced the standalone toolbar settings Activity base with Material Files' AppActivity theme lifecycle.
- Fixed toolbar enable/disable state handling after drag/reorder.

## Editor appearance
- Added a dedicated Appearance page from both Code editor and Text editor settings.
- Added theme presets: GitHub Dark/Light, Dracula, One Dark, Nord, Tokyo Night, Catppuccin Mocha, Solarized Dark, Darcula and Quiet Light.
- Added per-category syntax colors: comments, keywords, strings, numbers, types, functions, variables, constants, operators, tags, attributes and punctuation.
- Added editor chrome colors: background, text, current line, selection, gutter background/text, cursor, toolbar background and toolbar icon color.
- Added font family, weight and programming-ligature settings using Android/system fonts. External coding fonts are prepared for the next asset bundle.
- Theme/color changes are applied when returning to the editor.

## Research
- Popular coding fonts reviewed: Fira Code, JetBrains Mono, Source Code Pro and Cascadia Code.
- Popular editor themes reviewed: GitHub, Dracula, One Dark, Nord, Tokyo Night and Catppuccin.
- Google Material Symbols remain the icon source for editor toolbar controls.

## Document/image component research
See EDITOR_COMPONENTS_RESEARCH.md.

## Build
Build in DroidSpaces:

    cd /root/MaterialFiles-Integrated-Editor-v4.1
    ./gradlew clean assembleDebug

APK:

    app/build/outputs/apk/debug/app-debug.apk

The final Gradle build was not run in this offline model environment.
