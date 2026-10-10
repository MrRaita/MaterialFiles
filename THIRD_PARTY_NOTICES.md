# Third-party notices

This project is a **modified version** of [Material Files](https://github.com/zhanghai/MaterialFiles)
by Hai Zhang, distributed under the **GNU General Public License, version 3 or (at your option) any later
version** (see [`LICENSE`](LICENSE)).

- Original work: Copyright (C) 2018 Hai Zhang.
- Modifications in this fork (integrated code/text editor, PDF and image viewers, JSON/XML compare, CI): Copyright (C) 2026 MrRaita, released under the same GPL-3.0-or-later terms.

The app itself is free software. It contains **no analytics or tracking services**.

Full license texts are in [`LICENSES/`](LICENSES/). The list shown inside the app
(About → Open source licenses) is generated from `app/src/main/res/raw/licenses.xml`.

## Libraries linked into the app

| Component | License | Notes |
|---|---|---|
| [sora-editor](https://github.com/Rosemoe/sora-editor) (`editor`, `language-textmate`) 0.24.6 | LGPL-2.1-or-later | Copyright 2020-2026 Rosemoe. Unmodified library, used through its public API. Text: `LICENSES/LGPL-2.1.txt` |
| [Eclipse tm4e](https://github.com/eclipse-tm4e/tm4e) core (bundled inside sora-editor `language-textmate`) | EPL-2.0 | Source: https://github.com/eclipse-tm4e/tm4e and https://github.com/Rosemoe/sora-editor . Text: `LICENSES/EPL-2.0.txt` (run `fetch-licenses.sh`) |
| Gson, SnakeYAML Engine, Joni, JCodings (transitive via language-textmate) | Apache-2.0 / Apache-2.0 / MIT / MIT | |
| [jcifs-ng](https://github.com/AgNO3/jcifs-ng) | LGPL-2.1-or-later | Copyright AgNO3 GmbH & Co. KG |
| [AndroidRetroFile](https://github.com/zhanghai/AndroidRetroFile) | GPL-2.0 with Classpath Exception | Copyright Hai Zhang |
| [dav4jvm](https://github.com/bitfireAT/dav4jvm) | MPL-2.0 | Unmodified; source: upstream repository (commit pinned in `app/build.gradle`). Text: `LICENSES/MPL-2.0.txt` |
| smali / baksmali / dexlib2 3.0.10 | BSD-3-Clause | Used by the binary-file viewer |
| sshj, smbj, commons-net, Apache FtpServer, Apache MINA, Guava, Coil, AndroidX, Material Components, Kotlin, libsu, PhotoView, AndroidSVG, SubsamplingScaleImageView and others | Apache-2.0 | Text: `LICENSES/Apache-2.0.txt` |
| Bouncy Castle, Shizuku-API, RikkaX, SLF4J Android | MIT | See MIT text below |
| smbj-rpc (dcerpc) | BSD-3-Clause | |
| PreferenceX, asn-one, Apache Commons IO, OkHttp, Okio, desugar_jdk_libs | Apache-2.0 | desugar_jdk_libs also contains OpenJDK parts (GPL-2.0 with Classpath Exception) |
| mbassador | MIT | Transitive via SMBJ |

The complete, per-library list with copyright lines is in `app/src/main/res/raw/licenses.xml`.

## Bundled fonts (`app/src/main/assets/fonts/`)

Unmodified static TrueType files, each under the **SIL Open Font License 1.1**. They are separate works,
not covered by the GPL, and are not sold separately. Their license texts are in
`app/src/main/assets/fonts/licenses/`.

| Font | Version | Copyright | Reserved Font Name |
|---|---|---|---|
| JetBrains Mono | 2.304 | Copyright 2020 The JetBrains Mono Project Authors (https://github.com/JetBrains/JetBrainsMono) | none declared |
| Fira Code | 6.002 | Copyright 2014-2021 The Fira Code Project Authors (https://github.com/tonsky/FiraCode) | see upstream license |
| Source Code Pro | 2.042 | Copyright 2023 Adobe (http://www.adobe.com/), with Reserved Font Name 'Source' | 'Source' |
| Cascadia Code | 2407.024 | Copyright (c) 2019 - Present, Microsoft Corporation | 'Cascadia Code' |

"JetBrains Mono" is a trademark of JetBrains s.r.o.; "Source" of Adobe; "Cascadia Code" of the Microsoft
group of companies. Fira Code is based on Fira Mono (Mozilla); no font is renamed or modified here.

## TextMate grammars (`app/src/main/assets/textmate/`)

| Grammar | Upstream | License |
|---|---|---|
| Java | https://github.com/atom/language-java | MIT, Copyright (c) 2014 GitHub Inc. (itself derived from textmate/java.tmbundle) |
| JavaScript | https://github.com/microsoft/TypeScript-TmLanguage | MIT, Copyright (c) Microsoft Corporation |
| Python | https://github.com/MagicStack/MagicPython | MIT, Copyright (c) 2015-present MagicStack Inc. |
| Markdown | https://github.com/microsoft/vscode-markdown-tm-grammar | MIT, Copyright (c) Microsoft 2018 |
| Lua | https://github.com/sumneko/lua.tmbundle | MIT, Copyright (c) 2022 最萌小汐 |
| HTML | https://github.com/textmate/html.tmbundle | Permissive TextMate bundle notice: "Permission to copy, use, modify, sell and distribute this software is granted. ... provided \"as is\" ..." (quoted in `app/src/main/assets/textmate/LICENSES/html.tmbundle-license.txt`) |
| Kotlin | Copied from the sora-editor demo app assets (`app/src/main/assets/textmate/kotlin/`) | Distributed there under LGPL-2.1-or-later; original grammar author not stated upstream |

The remaining grammars (CSS, Gradle, JSON, properties, shell, smali, SQL, TOML, XML) are simple
originals written for this project and fall under the project's GPL-3.0-or-later license.

## MIT License (applies to every component above marked MIT, with its own copyright line)

```
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
USE OR OTHER DEALINGS IN THE SOFTWARE.
```
