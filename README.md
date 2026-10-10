# Kanrimate Files

Kanrimate Files is a modified version (fork) of [Material Files](https://github.com/zhanghai/MaterialFiles) by Hai Zhang — an open source,
Material Design file manager for Android. This fork is **not** the original app and is **not** affiliated with,
endorsed by, or published by the original author. It is not available on Google Play or F-Droid.

> Package name: `com.mrraita.kanrimate`. It has its own identity, so it installs alongside the original app and does not replace or update it.

## What this fork adds

- Built-in text and code editor ([sora-editor](https://github.com/Rosemoe/sora-editor)) with syntax highlighting, themes and monospace fonts
- Built-in PDF and image viewers
- JSON and XML comparison
- Root and Shizuku access options
- No analytics, tracking or crash-reporting services of any kind (Firebase and similar services were removed)

Everything from the original app is still there: breadcrumbs, root support, archive support (view, extract, create),
FTP / SFTP / SMB / WebDAV support, themes with night mode, and Linux-aware file handling (symbolic links, permissions,
SELinux context).

## Building

Requirements: JDK 21 and the Android SDK / NDK versions used in `.github/workflows/prebuild.yml`.

```
./gradlew assembleRelease
```

Release signing uses `signing.properties` and a keystore, neither of which is stored in this repository
(see `signing.properties.example`). Pre-release and release APKs are built by the GitHub Actions workflows.

## Privacy

The app contains no analytics or tracking and sends no data to the developer. See [PRIVACY.md](PRIVACY.md).

## License

This program is free software, licensed under the **GNU General Public License, version 3 or (at your option) any
later version** — see [LICENSE](LICENSE).

- Original work: Copyright (C) 2018 Hai Zhang ([zhanghai/MaterialFiles](https://github.com/zhanghai/MaterialFiles)).
- Modifications in this fork: Copyright (C) 2026 MrRaita, under the same license. The modification history is in the git log.
- Third-party libraries, fonts and syntax grammars keep their own licenses. They are listed in
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), in the app under About → Open source licenses, and the full
  license texts are in the [LICENSES](LICENSES) folder.
- The complete corresponding source code of every build is this repository.

"Material Files" is the name of the original project; no endorsement by its author is implied.

    Copyright (C) 2018 Hai Zhang
    Copyright (C) 2026 MrRaita

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
