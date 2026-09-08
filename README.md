# Film Simulations — persistent film-look recipes for the Sony A6000

A PlayMemories Camera App for the **Sony ILCE-6000 (A6000)** that stores film-simulation-style colour recipes
directly in the camera's settings, so they apply to **every photo and video mode** and survive power cycles.
Pick a recipe with the control wheel while watching the live view, press the centre button to store it, power-cycle,
done — the look is now the camera's default in P/A/S/M, movie, everything.

> The A6000 has no Picture Profile menu and cannot store PP gamma / colour-depth values (the UI was never compiled
> into its firmware; the controller code exists but has no storage). Recipes here use only what this body stores
> persistently: Creative Style base, saturation / contrast / sharpness, white balance (mode, Kelvin, A-B, G-M) and
> the hidden alternate colour matrix that `PP_NO=3` switches on (~+45 % chroma). They are approximations of film
> looks, not clones of anyone's Picture-Profile recipes.

Current version: **0.12** · ready-to-install APK: [`dist/FilmSimulations.apk`](dist/FilmSimulations.apk)

## Installing (step by step)

You need: the camera, its USB cable, a computer (Windows, macOS or Linux), Python 3. No camera modification, no
unlocking — this is the same channel Sony used for PlayMemories Camera Apps. Everything is reversible: the app can be
uninstalled from the camera menu and TRASH → ENTER inside the app restores factory colour settings.

### 1. Get the installer (Sony-PMCA-RE)

```
git clone https://github.com/ma1co/Sony-PMCA-RE.git
cd Sony-PMCA-RE
pip install -r requirements.txt
```

Windows users can instead download `pmca-gui.exe` from the
[Sony-PMCA-RE releases](https://github.com/ma1co/Sony-PMCA-RE/releases) — no Python needed.

### 2. Get the app

Either download [`dist/FilmSimulations.apk`](dist/FilmSimulations.apk) from this repository, or build it yourself
(see *Building*).

### 3. Prepare the camera

1. Charge the battery (installation takes ~1 minute but do not let it die mid-way).
2. `MENU → Setup → USB Connection → Mass Storage` (not MTP, not Auto).
3. `MENU → Setup → USB LUN Setting → Multi` (default) is fine.
4. Insert a memory card (the camera refuses USB mode without one).
5. Turn the camera on and connect it with the USB cable. The screen shows *USB Mode*.

### 4. Install

Command line (all platforms):

```
python pmca-console.py install -f FilmSimulations.apk
```

GUI (Windows `pmca-gui.exe`): open it, *Install app from file*, choose the APK, wait.

What you will see:

```
Switching to app install mode
Waiting for camera to switch...
Analyzing apk
Package: com.voxivoid.ppselect
Version: 0.12
Uploading 100%
Installing 100%
Task completed successfully
```

The camera switches modes by itself during this; do not press anything on it. It returns to the shooting screen
when done. Unplug the cable.

Linux note: USB access needs root or a udev rule — `sudo python pmca-console.py install ...` is the quick way.
Windows note: the console version uses the built-in mass-storage driver; if it says `No devices found`, check that
the camera is in Mass Storage mode and shows *USB Mode* on its screen, then retry.

### 5. Run it

`MENU → Application → Application List → Film Simulations`. The live view appears with the recipe panel at the
bottom. Turn the control wheel to scroll recipes, C1 for the brand browser, press the centre button to store the one
you like, then **turn the camera off and on** — the look is now applied to every photo and video mode.

Check `MENU → Creative Style`: you will see the base style and the ±values the recipe wrote. You can still change
anything there manually; the app only sets starting values.

### 6. Uninstall / undo

* Undo the colour settings: open the app, TRASH, ENTER, power-cycle (or set Creative Style / WB back by hand).
* Remove the app: `MENU → Application → Application Management → Manage and Remove → Film Simulations`.

### Troubleshooting

| symptom | fix |
|---|---|
| `No devices found` | USB Connection must be *Mass Storage*; card inserted; camera on and showing *USB Mode*; try another cable/port |
| Camera shows *USB Mode* but install hangs at *Waiting for camera to switch* | Unplug, power-cycle the camera, plug back in and rerun |
| Badge says **PROTECTED** in the app | Settings store is write-protected. Install [OpenMemories-Tweak](https://github.com/ma1co/OpenMemories-Tweak) and disable *Backup protection* in it, then rerun this app |
| Look not applied after storing | You must power-cycle; the app's own preview stops when it exits |
| Wrong Creative Style shown after power-cycle | Style enum for that entry is still provisional (only *Standard* is verified); open an issue with what you got |

## Screen

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│                      live view (full frame)                   │
│                                                               │
│ Kodak Portra 400  PREVIEW                   KODAK   31 / 72 │
│ Portrait  ·  WB auto                                          │
│ [STYLE  ][SAT ][CON ][SHARP][MATRIX][WB  ][KELVIN][A-B][G-M]  │
│  Portrait  -1    0     0     off    auto    -     A2   G1     │
│ ◎ recipe ▲▼ param C1 browse ● store 🗑 factory AEL hide ▤ exit│
└───────────────────────────────────────────────────────────────┘
```

* **Title row** — recipe name, badge, position in the list.
  Badge: **STORED** = camera settings already equal the recipe · **PREVIEW** = only the live view shows it, press the
  centre button to store · **PROTECTED** = settings store is write-protected (disable protection with
  OpenMemories-Tweak first; the app will not be able to write).
* **Meta line** — base Creative Style, white balance, colour-matrix flag, preview errors if any.
* **Parameter chips** — the nine stored values. Amber chip = selected for editing; amber value = differs from what is
  stored. Kelvin shows `-` unless WB is in colour-temperature mode.
* **Legend** — drawn with Canvas (the camera font has no symbol glyphs); switches to "adjust" when a chip is selected.
* **Browser** — C1 opens the brand picker (see Recipes).
* **Pill / hidden** — press AEL to shrink the overlay to a small pill (recipe name + index) or hide it completely.
  Recipe scrolling keeps working in both states so you can compare looks on a clean frame.
* **Toast** — status messages (stored, protected, errors) appear top-centre and fade.

## Controls

| key | action |
|---|---|
| control wheel, LEFT / RIGHT | previous / next recipe — applied to the live view immediately |
| UP / DOWN | select a parameter chip; LEFT / RIGHT or the top dial then adjust it |
| C1 | open / close the brand browser (see Recipes) |
| centre button (ENTER) | **store** the staged values in the settings store + sync |
| AEL (also DISP, Fn) | overlay: full panel → small pill → hidden |
| TRASH | stage factory values (Standard, 0 / 0 / 0, matrix off, WB auto) — press ENTER to store them |
| shutter | take a photo with the previewed look (half-press = AF) |
| MENU | exit (live preview reverts; stored values stay) |

PLAY is deliberately unbound: the firmware always opens playback on that key, nothing an app can do about it.
Changes stored with ENTER take effect after a **power-cycle**.

## Recipes

72 entries in `Recipes.java`, grouped by brand. Only looks that map credibly onto this body's controls were kept:
colour negatives, slides, in-camera "looks" and monochromes. Log profiles (S-Log, V-Log, Blackmagic Film, Cinelike D)
need a tone curve this body cannot store, and tinted monochromes (selenium, cyanotype) are impossible because Sony's
B&W ignores WB tint — none of those are included.

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

**Browser (C1):** a two-column picker — brands on the left, that brand's recipes on the right with a one-line summary
(base style, sat/con, matrix, WB). Wheel / UP / DOWN walk through recipes (crossing into the next brand at the end),
LEFT / RIGHT jump brands, every move is previewed live on the frame behind, ENTER picks and returns to the panel,
C1 or MENU close. In the main panel the wheel still scrolls the whole list linearly; the counter shows the brand.

Editing a recipe: pick it, UP/DOWN to a chip, dial or LEFT/RIGHT, ENTER. The live view is always what will be stored.
Adding a recipe: one line in `Recipes.java` (keep it inside its brand block), rebuild.

## How it works

* `NativeBackup` (`jni/jni.cpp`, `libppsel.so`) reads/writes single settings bytes through the camera's backup
  driver via OSAL messages — the same mechanism [OpenMemories-Tweak](https://github.com/ma1co/OpenMemories-Tweak)
  uses. `Backup_sync_all()` flushes to `/setting/Backup.bin`.
* Settings slots (found by disassembling the camera app's parameter-registration routine, `libObj.so`):

  | setting | id | notes |
  |---|---|---|
  | Creative Style | `0x01070175` | `1 = standard` verified; other enum values provisional |
  | Contrast | `0x01070178` | signed byte, menu range ±3 |
  | Saturation | `0x01070187` | signed byte, menu range ±3, camera core accepts ±16 |
  | Sharpness | `0x0107018a` | signed byte |
  | Picture Profile no. | `0x0107031c` | 0 = off, 3 = alternate colour matrix (no gamma on this body) |
  | WB mode | `0x01070019` | `1 = auto`, `14 = colour temperature` (observed) |
  | WB Kelvin | `0x01070018` | Kelvin / 100 |
  | WB A-B | `0x01070017` | signed, amber + / blue − |
  | WB G-M | `0x01070016` | signed, green + / magenta − |

* Live preview uses the app camera API (`CameraEx` → `Camera.Parameters`): `color-mode`, `saturation`, `contrast`,
  `sharpness`, `whitebalance` / `color-temperture-white-balance`, `light-balance-for-white-balance`,
  `color-compensation-for-white-balance`, `rgb-matrix` (Q10 fixed point, 1.0 = 1024). Runtime parameters revert
  when the app closes; only the stored bytes persist.
* Keys arrive as Linux scan codes (`ScalarInput.ISV_KEY_*`): wheel rotation 522 / 523, top dial 525 / 526, AEL 532,
  C1 622, trash 595, centre 232, MENU 514.

## Safety

* Only writes bytes whose factory values are known; **TRASH → ENTER** restores them. Backup protection must be off
  (OpenMemories-Tweak can toggle it; the badge shows PROTECTED otherwise).
* Nothing touches firmware, bootloader or the PlayMemories system. Uninstall via *Application Management*.
* Tested on ILCE-6000 firmware 3.21 only. Other CXD90014 bodies likely share the slots — verify with the on-screen
  readings before trusting a recipe.

## Building (Windows)

Requirements: JDK 17, Android SDK build-tools 30.0.3 + a platform jar (API 28 used), **Android NDK r16b**
(last NDK whose GCC toolchain and stlport build for this 2011-era Android 2.3.7), Python 3 for the installer.

```
git clone --recursive https://github.com/voxivoid/film-simulations-sony-a6000.git
cd film-simulations-sony-a6000
set ANDROID_NDK=C:\path\to\android-ndk-r16b      REM optional overrides: JAVA_HOME, ANDROID_SDK, BUILD_TOOLS, PLATFORM_JAR
build.cmd
```

`build.cmd` renames the platform's `errno.h` shim (updater-only, shadows the NDK header), runs ndk-build, aapt,
javac (`-encoding UTF-8` — otherwise `·` becomes `Â·` on the camera), d8 (invoked as `java -cp d8.jar`, since
`d8.bat` picks whatever Java is on PATH), zipalign and apksigner (v1 signature only; a throw-away keystore is
generated on first run).

The result is `FilmSimulations.apk` (also copied to `dist/`). Install as described above. Reinstalling over an
existing version keeps the same package (`com.voxivoid.ppselect`, unchanged from the old "PP Select" name so updates
replace instead of duplicating); no uninstall needed.

## Credits

App icon: original film-strip drawing (`res/drawable-*/ic_launcher.png`, generated by a small PIL script), public domain.


**Author:** [André Domingues (voxivoid)](https://github.com/voxivoid) — reverse engineering of the A6000 settings
store and firmware (backup IDs, PP_NO behaviour, colour-matrix measurement), the app, the recipes.

**Huge thanks to [ma1co](https://github.com/ma1co).** None of this would exist without his years of work reverse
engineering Sony's PlayMemories camera platform:

* [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) — the app-install channel, the updater shell used to dump
  this camera's firmware and `Backup.bin`, and `fwtool` for unpacking it.
* [OpenMemories-Platform](https://github.com/ma1co/OpenMemories-Platform) — the backup driver / OSAL bindings this
  app links against (vendored here as a git submodule).
* [OpenMemories-Tweak](https://github.com/ma1co/OpenMemories-Tweak) and
  [OpenMemories-Framework](https://github.com/ma1co/OpenMemories-Framework) — the reference for talking to
  `Backup_read/write`, the `ScalarInput` key codes and the `CameraEx` API.
* The earlier nex-hack community research he built on and kept documented.

He figured out how these cameras work, documented it openly and licensed it permissively — this project just stands
on that.

Recipe names follow the popular Sony film-simulation list; the values are original approximations for this body.

## License

MIT (this repository). OpenMemories-Platform: MIT, © 2017 ma1co.
