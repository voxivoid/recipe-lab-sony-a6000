# Recipe Lab for the Sony A6000

Version **0.13** · APK: [`dist/RecipeLab.apk`](dist/RecipeLab.apk)

## 0. What it is

A camera app (PlayMemories Camera App) for the **Sony ILCE-6000** with a library of colour recipes that replicate
other cameras' looks (Fuji film simulations, Ricoh GR image controls, Leica / Hasselblad / Canon / Nikon colour,
Sony's newer Creative Looks) and classic film stocks (Kodak, Fuji, Cinestill, Agfa, Ilford). You pick a recipe while
watching the live view, press the centre button, power-cycle — the look is now the camera's default in **every photo
and video mode**, without the app running.

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

### Install (about 10 minutes, once)

You need the camera, its USB cable, a memory card, and a Windows / Mac / Linux computer.

1. **Get the installer tool.** It is called Sony-PMCA-RE, made by ma1co; it puts apps on Sony cameras the same way
   Sony's own (now closed) app store did.
   * Windows, easiest: download `pmca-gui.exe` from the
     [releases page](https://github.com/ma1co/Sony-PMCA-RE/releases). Nothing to install, just run it.
   * Mac / Linux / Windows without the GUI: install Python 3, then in a terminal
     `git clone https://github.com/ma1co/Sony-PMCA-RE.git`, `cd Sony-PMCA-RE`, `pip install -r requirements.txt`.
2. **Download the app**: [`dist/RecipeLab.apk`](dist/RecipeLab.apk) (click, then *Download raw file*).
3. **Set the camera up for USB.** Battery charged, memory card inside. In the camera menu:
   `MENU → Setup (toolbox icon) → USB Connection → Mass Storage`. Turn the camera on and plug the cable into the
   computer. The camera screen should say *USB Mode*.
4. **Install.**
   * GUI: open `pmca-gui.exe` → *Install app from file* → choose `RecipeLab.apk` → wait.
   * Terminal, from the Sony-PMCA-RE folder:

     ```
     python pmca-console.py install -f RecipeLab.apk
     ```

     On Linux put `sudo` in front.

   The camera will flicker, show a black screen and change modes a couple of times on its own — that is normal, do
   not press anything. After about a minute the computer says `Task completed successfully` and the camera is back
   on its shooting screen.
5. **Unplug** the cable. The app is now under `MENU → Application → Application List → Recipe Lab`.

### Pick and store a look

1. Open `Recipe Lab` from the Application List. You see the live image with a panel at the bottom.
2. **Turn the control wheel** (the ring on the back). Each click is a different recipe, and the live image changes
   immediately — this is exactly how your photos and videos will look.
3. Want to jump between brands? Press **C1** (small button next to the shutter). A list opens: brands on the left,
   recipes on the right. Wheel or up/down moves through recipes, left/right switches brand, the live image keeps
   following. Press the **centre button** to pick one and close the list.
4. Want to see the image without any text? Press **AEL** once for a tiny label, twice for nothing at all. Wheel
   still works. Press again to bring the panel back.
5. Like it? Press the **centre button**. A message confirms it was stored.
6. **Turn the camera off and on.** Done — the look is now the camera's default in every mode (P, A, S, M, movie…),
   with the app closed.

Extras: up/down on the wheel selects one of the value chips (saturation, contrast, …) so you can fine-tune with
left/right before storing. **TRASH** (the bin button) stages the factory look; centre button stores it. **MENU**
leaves the app. The shutter takes a picture with whatever look you are previewing.

The small badge next to the recipe name tells you where you stand: **STORED** = the camera already has these
values · **PREVIEW** = you are only looking, press the centre button to keep it · **PROTECTED** = the camera is not
accepting changes (see Troubleshooting).

## 2. What it changes, and is it permanent

**What it actually does.** Recipe Lab sets the same things you could set by hand in the camera menus — Creative
Style, its contrast / saturation / sharpness sliders, white balance and its fine-tune — plus one hidden switch that
turns on a richer colour matrix the camera has but never exposes. That is all: nine settings values. It does not
modify the camera's software, firmware or operating system, and it does not need any unlocking or "jailbreak".
Installing it is the same mechanism Sony used for its own downloadable apps.

**Is it permanent?** The look stays until you change it, on purpose — that is what makes it work in every mode
without the app. It is not permanent in the sense of damage: you can undo it at any time in three ways.

* In the app: press **TRASH**, then the **centre button**, then turn the camera off and on.
* In the menus: set Creative Style back to *Standard* with 0 / 0 / 0 and White Balance to *Auto*.
* Or use the camera's own `MENU → Setup → Setting Reset → Camera Settings Reset`.

**Things worth knowing.**

* Some recipes set saturation further than the menu slider goes (the menu allows ±3, the camera itself accepts
  more). The menu then shows the nearest value it can; if you touch that slider, it snaps back into its normal
  range and the recipe loses that extra punch. Just re-store from the app if that happens.
* For anything other than the *Standard* base style, the stored style number is our best current mapping. If, after
  power-cycle, the menu shows a different Creative Style than the recipe named, please tell us (open an issue) — it
  helps confirm the mapping.
* The live preview inside the app is temporary. Closing the app removes it. Only what you *stored* with the centre
  button stays.
* Uninstalling the app does **not** put the colour settings back. If you want factory colour, undo first (any of the
  three ways above), then remove the app via
  `MENU → Application → Application Management → Manage and Remove → Recipe Lab`.
* Made for the A6000 on firmware 3.21, tested on that. Other Sony bodies of the same generation probably store
  these settings in the same place, but nobody has checked — compare what the chips show with your menus before
  storing anything.

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
AndroidManifest.xml            package com.voxivoid.recipelab
src/com/voxivoid/recipelab/
  MainActivity.java            UI state, key handling, live preview (CameraEx via reflection), store/sync
  Recipes.java                 the 72 recipes, brands, GROUP_START / GROUP_COUNT
  PickerView.java              Canvas-drawn brand browser
  HintBar.java                 Canvas-drawn key legend (camera font has no symbol glyphs)
  NativeBackup.java            JNI: read / write / attr / sync / isProtected
jni/jni.cpp                    Backup_read / Backup_write / Backup_sync_all via OpenMemories-Platform
jni/platform/                  git submodule: ma1co/OpenMemories-Platform
res/                           layout, shape drawables, launcher icon
build.cmd                      full Windows build → RecipeLab.apk (+ copy to dist/)
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
git clone --recursive https://github.com/voxivoid/recipe-lab-sony-a6000.git
cd recipe-lab-sony-a6000
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
