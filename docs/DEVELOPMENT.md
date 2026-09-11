# Development notes

Reverse-engineering notes, the settings-store map, and how to build Recipe Lab.
For using the app, see the [README](../README.md). For the branch/commit/release rules,
see [CONTRIBUTING.md](CONTRIBUTING.md).

---

**Contents**

- [Source layout](#source-layout)
- [Settings slots](#settings-slots)
- [Exit rule](#exit-rule)
- [Live preview](#live-preview)
- [Building](#building)
- [Versioning](#versioning)
- [Adding recipes](#adding-recipes)

---

## Source layout

```
AndroidManifest.xml            package com.voxivoid.recipelab
src/com/voxivoid/recipelab/
  MainActivity.java            UI state, key handling, live preview (CameraEx via reflection), store + sync
  Recipes.java                 the 77 recipes, brands, GROUP_START / GROUP_COUNT
  res/raw/ids.txt              every settings entry of 16 bytes or less, used by the Fn snapshot/diff tool
  PickerView.java              Canvas-drawn brand browser
  Legend.java                  Canvas-drawn key icons, fit-to-width (camera font has no symbol glyphs)
  HintBar.java                 legend view under the panel (uses Legend)
  NativeBackup.java            JNI: read / write / attr / sync / isProtected
jni/jni.cpp                    Backup_read / Backup_write / Backup_sync_all via OpenMemories-Platform
jni/platform/                  git submodule: ma1co/OpenMemories-Platform
res/                           layout, shape drawables, launcher icon
build.cmd                      full Windows build → RecipeLab.apk (+ copy to dist/)
```

## Settings slots

Found by disassembling the camera app's parameter registration in `libObj.so`):

| setting | id | notes |
|---|---|---|
| Creative Style | `0x01070175` | index in the runtime `color-mode-values` list (1 standard, 2 vivid, 3 neutral … 6 mono; verified) |
| Contrast | `0x01070178` | signed byte |
| Saturation | `0x01070187` | signed byte, core accepts ±16 |
| Sharpness | `0x0107018a` | signed byte |
| Picture Profile no. | `0x0107031c` | 0 off, 3 = alternate colour matrix (no gamma on this body) |
| WB mode | `0x01070019` | 1 auto, 14 colour temperature |
| WB Kelvin | `0x01070018` | Kelvin / 100 |
| WB A-B / G-M | `0x01070017` / `0x01070016` + per-mode copies: AWB `0x0107067f` / `0x0107067e`, colour temp `0x01070683` / `0x01070682` | signed, magenta positive (menu G1 = 0xff). The camera applies the per-mode copy (verified end-to-end) |
| Picture Effect | `0x010706f1` | index in `picture-effect-values` (verified: Retro = 4) |
| Effect sub-setting | `0x010709d8` high-key tint · `0x010706f3` toy tone · `0x010706ee` partial-colour hue · `0x010706ef` posterization | index in the runtime value list (high-key tint verified) |
| Exposure bias | `0x010700b8` + copy `0x01070c7f` | 1/3 EV steps, signed (verified: +0.7 = 2); both written |
| DRO | `0x01070104` (+ level byte `0x01070775`) | Off 0, Auto 1, Lv1–5 = 2–6; level byte 1 for Off/Auto, Lv n = n+1 (verified) |
| Quality: file format | `0x01070013` (+ mirror `0x01070aa9`) | RAW = 1, RAW+JPEG = 2, JPEG = 0 (verified) |
| Quality: JPEG level | `0x01070014` (+ mirror `0x01070aaa`) | Std = 0, Fine = 1 (verified) |

## Exit rule

The camera writes some live parameters (exposure bias, WB fine-tune) straight back into the settings
store, so on exit the app sets the live parameters to the *stored* values rather than to its launch snapshot —
otherwise a freshly stored recipe would be undone the moment the app closes.

## Live preview

Goes through `Camera.Parameters`: `color-mode`, `saturation`, `contrast`, `sharpness`,
`whitebalance`, `color-temperture-white-balance`, `light-balance-for-white-balance`,
`color-compensation-for-white-balance`, `rgb-matrix` (Q10, 1.0 = 1024) + `rgb-matrix-mode`, `picture-effect`,
`exposure-compensation` (1/3 EV steps), `dro-mode` + `dro-level`.
**Key scan codes:** wheel 522 / 523, top dial 525 / 526, AEL 532, C1 622, Fn 520, trash 595, centre 232, MENU 514.

## Building

Toolchain, both platforms: **JDK 17**, Android SDK **build-tools 30.0.3** with a platform jar (**API 28**), and
**NDK r16b** — the last NDK with the GCC toolchain this Android 2.3.7 target needs. `jni/Application.mk` pins
`APP_ABI := armeabi`, `APP_STL := stlport_static`, `APP_PLATFORM := android-14`, `NDK_TOOLCHAIN_VERSION := 4.9`;
that combination is what rules out every later NDK.

```
git clone --recursive https://github.com/voxivoid/recipe-lab-sony-a6000.git
cd recipe-lab-sony-a6000
```

**Windows** — `build.cmd`:

```
set ANDROID_NDK=C:\path\to\android-ndk-r16b      REM optional: JAVA_HOME, ANDROID_SDK, BUILD_TOOLS, PLATFORM_JAR
build.cmd
```

**Linux / WSL / macOS** — `build.sh`, the same seven steps and the same APK. This is what CI
runs, and it is the supported way to build:

```bash
export JAVA_HOME=$HOME/toolchains/jdk17
export ANDROID_SDK=$HOME/Android/Sdk
export ANDROID_NDK=$ANDROID_SDK/ndk/16.1.4479499
./build.sh                 # X.Y.Z-dev.N
RELEASE=1 ./build.sh       # X.Y.Z — the tag must match the manifest
```

Setting the toolchain up from nothing, no root required:

```bash
# JDK 17
curl -sL -o jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
mkdir -p ~/toolchains/jdk17 && tar xzf jdk.tar.gz -C ~/toolchains/jdk17 --strip-components=1

# Android cmdline-tools, then the three packages
curl -sL -o cmdline.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
mkdir -p ~/Android/Sdk/cmdline-tools && unzip -q cmdline.zip -d /tmp/ct
mv /tmp/ct/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
  "build-tools;30.0.3" "platforms;android-28" "ndk;16.1.4479499"
```

About 3 GB installed. To sign with the project key rather than a throwaway one, set
`ANDROID_KEYSTORE_B64` (`base64 -w0 <keystore>`), `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` — the same four values CI holds as secrets.

Both scripts park the platform's `errno.h` shim (updater-only, it shadows the NDK header), then run ndk-build,
aapt, javac (`-encoding UTF-8`), d8 (invoked as `java -cp d8.jar`, because `d8.bat` uses whatever Java is on
PATH), zipalign and apksigner — **v1 signing only**, since the camera does not understand v2/v3.
`build.sh` restores `errno.h` from an `EXIT` trap, so an aborted build never leaves the submodule dirty.

**Signing.** With no keystore configured, both scripts generate a throwaway key. An APK signed with a
different key **cannot be installed over an existing one** — the camera would need the app removed first.
CI therefore signs with the project key, held as the `ANDROID_KEYSTORE_B64` repo secret; set the same four
`ANDROID_KEYSTORE_*` variables locally if you need a build that updates an existing install in place.

### Installing on the camera from WSL

WSL2 has no USB stack of its own, so the camera is not visible until the device is forwarded
in. `~/code/pmca-scripts/setup-usb-wsl.sh` does the Linux half (usbip tools, the `054c` udev
rule, pyusb); the Windows half is `usbipd-win`, installed once from an Administrator
PowerShell, then `usbipd attach --wsl --busid <id>` each time the camera is plugged in.

This is the one part that cannot live entirely inside WSL — forwarding a USB device requires
a driver on the Windows side by design.

## Versioning

`AndroidManifest.xml` `android:versionName` is the **single source of truth**, and always holds the *next
target release* (`X.Y.Z`, no suffix). Nothing else stores a version — not the README, not the Java source.

```
versionCode = MAJOR*10_000_000 + MINOR*100_000 + PATCH*1_000 + P
P = N    dev prerelease, N = commits since the last v* tag (1..998)
P = 999  release
```

`999` makes a release outrank every prerelease before it, so `1.1.0` installs cleanly over `1.1.0-dev.42`
instead of being refused as a downgrade.

| script | does |
|---|---|
| `tools/version.sh` | computes `VERSION_NAME` / `VERSION_CODE` for a build; sourced by `build.sh` |
| `tools/bump-version.sh <x.y.z>` | opens the next cycle — the only way a version is ever typed |
| `tools/check-version.sh` | CI gate: manifest is consistent and no version mirror has crept back in |

A build never mutates the checked-in manifest; it writes `out/AndroidManifest.xml` and points `aapt` there.

## Adding recipes

Adding a recipe is one line in `Recipes.java` inside its brand block. Adding a brand is a new entry in `GROUPS` plus
a block of recipes.
