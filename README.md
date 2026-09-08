# PP Select — persistent film-look recipes for the Sony A6000

A PlayMemories Camera App for the **Sony ILCE-6000 (A6000)** that stores film-simulation-style colour recipes
directly in the camera's settings, so they apply to **every photo and video mode** and survive power cycles.
Live preview inside the app; one button writes the recipe; power-cycle applies it.

> The A6000 has no Picture Profile menu and cannot store PP gamma / colour-depth values (the UI was never compiled
> into its firmware; the controller code exists but has no storage). Recipes here use only what this body stores
> persistently: Creative Style base, saturation / contrast / sharpness, white balance (mode, Kelvin, A-B, G-M) and
> the hidden alternate colour matrix that `PP_NO=3` switches on (~+45 % chroma). They are approximations of film
> looks, not clones of anyone's Picture-Profile recipes.

## Controls

| key | action |
|---|---|
| LEFT / RIGHT | previous / next recipe — applied to the live view immediately |
| UP / DOWN | select a row (full overlay) to tweak a value manually |
| DISP (or Fn) | overlay: full → one-line bar → hidden |
| ENTER | **write** the staged values to the settings store + sync |
| PLAY | stage factory values (standard, 0/0/0, WB auto) |
| shutter | take a photo with the previewed look (half-press = AF) |
| MENU / trash | exit (live preview reverts; stored values stay) |

Changes written with ENTER take effect after a **power-cycle**.

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
  `color-compensation-for-white-balance`, `rgb-matrix` (Q10 fixed point, 1.0 = 1024).

## Safety

* Only writes bytes whose factory values are known; **PLAY → ENTER** restores them. Backup protection must be off
  (it is by default on this body; OpenMemories-Tweak can toggle it).
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

Install with [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE): camera in mass-storage USB mode, then

```
python pmca-console.py install -f PPSelect.apk
```

## Credits

Built on [ma1co](https://github.com/ma1co)'s OpenMemories-Platform (MIT, git submodule) and Sony-PMCA-RE.
Recipe names follow the popular Sony film-simulation list; the values are original approximations for this body.

## License

MIT (this repository). OpenMemories-Platform: MIT, © 2017 ma1co.
