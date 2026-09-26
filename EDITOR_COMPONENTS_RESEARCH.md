# Editor component research

## Fonts to bundle

The next font bundle should contain OFL-licensed coding fonts:
- JetBrains Mono — regular, medium, bold
- Fira Code — regular, medium, bold
- Source Code Pro — regular, semibold, bold
- Cascadia Code — regular, semibold, bold

The project should load these from `app/src/main/assets/editor-fonts/` with `Typeface.createFromAsset()`.

## Code themes

Recommended presets for the editor UI/syntax layer:
- GitHub Dark / GitHub Light
- Dracula
- One Dark
- Nord
- Tokyo Night
- Catppuccin Mocha
- Solarized Dark
- Darcula
- Quiet Light

These are represented by the local theme factory in this version; syntax categories and editor chrome can then be customized independently.

## PDF

Preferred first integration target: `androidx.pdf:pdf-viewer-fragment:1.0.0-beta01`. Android's Jetpack PDF viewer supports viewing, search, text selection, zoom/scroll, annotations, and the `EditablePdfViewerFragment` API for annotation/form editing. It is available from Google Maven, so no source ZIP is required if DroidSpaces can resolve Google Maven.

## DOCX

There is no comparable official AndroidX DOCX editor. `docx4j` is Apache-2.0 and can create/edit/save OOXML, but it is a document-model library rather than a Word-like Android renderer/editor.

For high-fidelity offline DOCX rendering, Fadocx demonstrates embedding LibreOfficeKit on arm64-v8a. Fadocx is GPLv3 and its README reports roughly 214 MB for the LibreOfficeKit portion. Its native `jniLibs/arm64-v8a` payload is the large component we would need if we choose that route.

## Images

Existing Material Files already has a PhotoView-based image editor. For crop, `uCrop 2.2.11` is Apache-2.0. `AndroidPhotoEditor` is MIT and adds drawing, shapes, text/image stickers, filters, crop, undo/redo and transform operations. These can be integrated incrementally instead of replacing the existing image viewer.
