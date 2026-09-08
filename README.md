# Film Simulations for the Sony A6000

Version **0.12** · APK: [`dist/FilmSimulations.apk`](dist/FilmSimulations.apk)

## 0. What it is

A camera app (PlayMemories Camera App) for the **Sony ILCE-6000** that gives it film-simulation-style colour
recipes. You pick a recipe while watching the live view, press the centre button, power-cycle — the look is now the
camera's default in **every photo and video mode**, without the app running.

The A6000 has no Picture Profile menu and cannot store gamma curves. Recipes use only what this body stores
persistently: Creative Style, saturation / contrast / sharpness, white balance (Kelvin, A-B, G-M) and a hidden
alternate colour matrix. They approximate film looks; they are not copies of another camera's colour science.

72 recipes, grouped by brand:

| brand | recipes |
|---|---|
| Sony | FACTORY (ST), PT, NT, VV, VV2, FL, IN, SH |
| Fuji Sim | Provia, Velvia, Astia, Classic Chrome, Classic Negative, Nostalgic Neg, Reala Ace, Pro Neg Std / Hi, Eterna, Eterna Bleach Bypass, Acros, Acros +Ye / +R / +G, Sepia |
| Fuji Film | Pro 400H, Fortia 50, Superia 400, C200, Natura 1600 |
| Kodak | Portra 160 / 400 / 800, Gold 200, Ultra Max 400, Color Plus 200, Ektar 100, Ektachrome E100, Kodachrome 64, Vision3 500T, Vision 200T (Asteroid City), Tri-X 400, T-Max |
| Cine | Cinestill 50D, Cinestill 800T, Classic Cinema, Rec709 Video |
| Ricoh GR | Positive Film, Negative Film, Bleach Bypass, Retro, Cross Process, Hi-Contrast B&W, Hard Monotone, Soft Monotone |
| Leica | Contemporary, Classic, Eternal, Monochrom |
| Hasselblad | HNCS Natural |
| Canon / Nikon | Canon Standard / Portrait / Faithful, Nikon Flat / Vivid |
| Pana / Olympus | L.Monochrome D, L.ClassicNeo, Pop Art, Pale & Light |
| Other Stocks | Agfa Vista 200, Agfa Ultra 100, Polaroid / Instax |
| Ilford | HP5, FP4, Delta 100, Delta 3200, Pan F 50 |

Not included, because the hardware cannot do them: log profiles (S-Log, V-Log, Blackmagic Film, Cinelike D) and
tinted monochromes (selenium, cyanotype).

## 1. How to use

### Install

1. Get [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE): `git clone` + `pip install -r requirements.txt`,
   or on Windows download `pmca-gui.exe` from its releases.
2. Download [`dist/FilmSimulations.apk`](dist/FilmSimulations.apk).
3. Camera: charged battery, memory card inserted, `MENU → Setup → USB Connection → Mass Storage`. Turn on, connect
   USB; the screen shows *USB Mode*.
4. Run:

   ```
   python pmca-console.py install -f FilmSimulations.apk
   ```

   (or in `pmca-gui.exe`: *Install app from file*). Expected output ends with `Task completed successfully`. The
   camera switches modes on its own — do not touch it. Linux needs `sudo` or a udev rule.
5. Unplug. The app is in `MENU → Application → Application List → Film Simulations`.

### Use

| key | action |
|---|---|
| control wheel, LEFT / RIGHT | previous / next recipe, previewed live |
| C1 | brand browser: brands left, recipes right; wheel / UP / DOWN recipe, LEFT / RIGHT brand, ENTER pick, C1 close |
| UP / DOWN | select a parameter chip; LEFT / RIGHT or top dial adjust it |
| centre button (ENTER) | **store** the recipe in the camera |
| AEL (also DISP, Fn) | overlay: full → small pill → hidden, for a clean preview |
| TRASH | stage factory values (Standard 0/0/0, WB auto) — ENTER stores them |
| shutter | take a photo with the previewed look |
| MENU | exit |

Badge next to the recipe name: **STORED** = camera already has these values · **PREVIEW** = only the live view,
press ENTER · **PROTECTED** = camera refuses writes (see Troubleshooting).

After ENTER, **turn the camera off and on**. The recipe is now the default everywhere. You can still adjust
Creative Style / WB in the normal menus; the app only sets starting values.

## 2. What it changes, and is it permanent

The app writes **nine bytes** into the camera's settings store (the same file the normal menus write):
Creative Style, contrast, saturation, sharpness, Picture Profile flag, WB mode, WB Kelvin, WB A-B, WB G-M.

* **Not firmware.** Nothing in the bootloader, firmware or Sony's system is touched. No unlocking, no root, no
  warranty-relevant modification beyond installing an app through Sony's own app channel.
* **Reversible in three ways.** In the app: TRASH → ENTER → power-cycle. In the menus: set Creative Style to
  Standard 0/0/0 and WB to Auto. Or `MENU → Setup → Setting Reset → Camera Settings Reset`.
* **Survives power-cycles on purpose** — that is the point — until you change it.
* **Two values can be out of menu range**: some recipes push saturation beyond ±3 (the camera core accepts ±16).
  The menu then shows the nearest value; touching that menu item snaps it back into range. Also, the Creative Style
  number stored for anything other than *Standard* is provisional (only Standard is verified against the menu);
  if a recipe comes back as a different base style after power-cycle, please report it.
* **The live preview is temporary.** While the app runs it sets runtime camera parameters; those revert when it
  exits. Only stored values persist.
* **Uninstall**: `MENU → Application → Application Management → Manage and Remove → Film Simulations`.
  Uninstalling does not undo stored values; use one of the reverts above first if you want factory colour.

Tested only on ILCE-6000 firmware 3.21. Other bodies of the same generation likely share these settings slots, but
verify what the chips display against your menus before storing anything.

## 3. Troubleshooting

| symptom | fix |
|---|---|
| `No devices found` | USB Connection = *Mass Storage*; card inserted; camera on and showing *USB Mode*; different cable / port |
| Hangs at `Waiting for camera to switch...` | Unplug, power-cycle the camera, reconnect, rerun |
| Badge **PROTECTED** | Settings store is write-protected. Install [OpenMemories-Tweak](https://github.com/ma1co/OpenMemories-Tweak), disable *Backup protection*, rerun |
| Look not applied after ENTER | Power-cycle the camera |
| Base style differs after power-cycle | Provisional style enum for that recipe — open an issue with recipe name and what the menu shows |
| `no live preview: ...` in the meta line | Another app or the firmware holds the camera; close and reopen the app |
| Text shows `Â·` | Old build without `-encoding UTF-8`; install the APK from `dist/` |

## 4. How to develop

Layout:

```
AndroidManifest.xml            package com.voxivoid.ppselect (kept from the "PP Select" days so updates replace)
src/com/voxivoid/ppselect/
  MainActivity.java            UI state, key handling, live preview (CameraEx via reflection), store/sync
  Recipes.java                 the 72 recipes, brands, GROUP_START / GROUP_COUNT
  PickerView.java              Canvas-drawn brand browser
  HintBar.java                 Canvas-drawn key legend (camera font has no symbol glyphs)
  NativeBackup.java            JNI: read / write / attr / sync / isProtected
jni/jni.cpp                    Backup_read / Backup_write / Backup_sync_all via OpenMemories-Platform
jni/platform/                  git submodule: ma1co/OpenMemories-Platform
res/                           layout, shape drawables, launcher icon
build.cmd                      full Windows build → FilmSimulations.apk (+ copy to dist/)
```

Settings slots (found by disassembling the camera app's parameter registration in `libObj.so`):

| setting | id | notes |
|---|---|---|
| Creative Style | `0x01070175` | `1 = standard` verified; others provisional |
| Contrast | `0x01070178` | signed byte |
| Saturation | `0x01070187` | signed byte, core accepts ±16 |
| Sharpness | `0x0107018a` | signed byte |
| Picture Profile no. | `0x0107031c` | 0 off, 3 = alternate colour matrix (no gamma on this body) |
| WB mode | `0x01070019` | 1 auto, 14 colour temperature |
| WB Kelvin | `0x01070018` | Kelvin / 100 |
| WB A-B / G-M | `0x01070017` / `0x01070016` | signed |

Live preview parameters (`Camera.Parameters`): `color-mode`, `saturation`, `contrast`, `sharpness`, `whitebalance`,
`color-temperture-white-balance`, `light-balance-for-white-balance`, `color-compensation-for-white-balance`,
`rgb-matrix` (Q10, 1.0 = 1024) + `rgb-matrix-mode`. Key scan codes: wheel 522/523, top dial 525/526, AEL 532,
C1 622, trash 595, centre 232, MENU 514.

Build (Windows): JDK 17, Android SDK build-tools 30.0.3 + platform jar (API 28), **NDK r16b** (last one with the GCC
toolchain this Android 2.3.7 target needs).

```
git clone --recursive https://github.com/voxivoid/film-simulations-sony-a6000.git
cd film-simulations-sony-a6000
set ANDROID_NDK=C:\path\to\android-ndk-r16b      REM optional: JAVA_HOME, ANDROID_SDK, BUILD_TOOLS, PLATFORM_JAR
build.cmd
```

`build.cmd` parks the platform's `errno.h` shim (updater-only, shadows the NDK header), runs ndk-build, aapt, javac
(`-encoding UTF-8`), d8 (as `java -cp d8.jar`, since `d8.bat` uses whatever Java is on PATH), zipalign, apksigner
(v1 only, throw-away keystore generated on first run). Add a recipe = one line in `Recipes.java` inside its brand
block; add a brand = new entry in `GROUPS` + a block of recipes.

## 5. Credits

**Author:** [André Domingues (voxivoid)](https://github.com/voxivoid) — reverse engineering of the A6000 settings
store (backup IDs, PP flag behaviour, colour-matrix measurement), the app, the recipes, the icon.

**Huge thanks to [ma1co](https://github.com/ma1co).** None of this would exist without his years of work reverse
engineering Sony's PlayMemories camera platform:

* [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) — the app-install channel, the updater shell used to dump
  this camera's firmware and settings, and `fwtool`.
* [OpenMemories-Platform](https://github.com/ma1co/OpenMemories-Platform) — the backup driver / OSAL bindings this
  app links against (vendored here as a git submodule).
* [OpenMemories-Tweak](https://github.com/ma1co/OpenMemories-Tweak) and
  [OpenMemories-Framework](https://github.com/ma1co/OpenMemories-Framework) — reference for `Backup_read/write`,
  the `ScalarInput` key codes and the `CameraEx` API.
* The earlier nex-hack community research he built on and kept documented.

He figured out how these cameras work, documented it openly and licensed it permissively — this project just stands
on that.

Recipe names follow the popular film-simulation lists; the values are original approximations for this body.

License: MIT (this repository). OpenMemories-Platform: MIT, © 2017 ma1co.
