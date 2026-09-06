# Fighter Tower

An Android tower-merge defense game, rendered in true 3D (OpenGL ES 2.0) with real 3D models.
Merge same-tier towers into heavier, harder-hitting ones to fight off waves of enemies converging
on your base from three lanes. Every time the player levels up, later waves get tougher — the
core difficulty hook — and defeated enemies drop Diamonds that fund permanent upgrades in the
Laboratory.

## Gameplay

- **Central base, three lanes**: the base sits in the middle of the board. Three lanes fan out
  from it in different directions, each with its own spawn point at the far end.
- **Tower spots**: each lane has several build spots running alongside it. Drag one tower onto an
  identical-tier tower — on *any* spot, on *any* lane — to merge them into the next tier (Scout ->
  Gunner -> Cannon -> Heavy Cannon -> Siege Tower -> Railgun -> Juggernaut -> Titan). Drag onto an
  empty spot to just relocate it. Merging isn't restricted to a single lane, so you can
  consolidate towers from different lanes into one heavy hitter.
- **Buy Tower**: spend gold to place a new tower on any empty spot, starting at whatever tier the
  Laboratory's starting-tier upgrade has unlocked.
- **Waves**: enemies spawn at one of the three lane spawn points and march inward toward the
  central base. Combat range is real 2D distance, not "same lane only" — a tower fires at
  whichever in-range enemy is closest, on any lane. Because the lanes converge at the base, a
  strong, centrally-placed tower can end up helping defend more than one lane, which is part of
  why merging across spots/lanes is worthwhile.
- **Leveling**: killing enemies grants gold and XP. Leveling up raises the stats of every
  subsequent wave's enemies on top of the normal per-wave difficulty ramp — so staying strong
  requires continuing to merge up, not just surviving.
- **Diamonds and the Laboratory**: kills also drop a pile of Diamonds where the enemy died; tap it
  within 10 seconds to collect it into a wallet that persists across games (unlike Gold). From the
  game-over screen, Home leads to the Laboratory, where Diamonds buy permanent upgrades — a higher
  starting tower tier and a bullet damage bonus — applied to every future run.
- **Game over**: if the base's health reaches zero, the run ends; Retry starts over immediately
  with the same Laboratory upgrades, or Home goes to the Laboratory first.

## Project layout

This is a two-module Gradle project, split specifically so the game rules can be verified without
an Android toolchain:

- **`game-core/`** — pure Kotlin/JVM module, zero Android dependencies. Contains every gameplay
  rule: `TowerTier` (stats per tier), `GridBoard` (placement/merge logic, addressed by
  `GridPos(lane, slot)`), `LaneLayout` (the three-lanes-converging-on-a-base geometry, shared by
  gameplay *and* rendering so combat range and visuals never disagree), `Enemy` / `WaveGenerator`
  (wave and difficulty scaling by wave number *and* player level, with enemies assigned
  round-robin across the three lanes), `PlayerProgress` (per-run gold/XP/leveling),
  `DiamondDrop` / `PermanentProgress` / `LabCatalog` (the Diamond economy and Laboratory
  upgrades, which persist across runs), and `GameEngine` (the per-frame simulation tick —
  including real 2D-distance combat targeting — that ties it all together and emits
  `GameEvent`s). Has a full unit test suite.
- **`app/`** — the actual Android application: an OpenGL ES 2.0 renderer that draws real 3D OBJ
  models (castle, towers, weapons, a UFO-style enemy — Kenney's Tower Defense Kit, CC0) with a
  textured shader, plus cheap procedural meshes for the ground/lanes and effects; the camera is
  aimed at the central base so it renders screen-center with the three lanes fanning out around
  it. A touch controller unprojects screen taps, finds the nearest tower spot for drag-to-merge,
  and separately hit-tests dropped Diamond piles for tap-to-collect. `GameViewModel` + Jetpack
  Compose handle the HUD (level/XP/gold/Diamonds/wave/base-health, buy button, game-over screen)
  and the Laboratory screen, with `PermanentProgressStore` persisting the Diamond wallet and
  upgrades to `SharedPreferences`. Its `WorldLayout` reads positions straight from `game-core`'s
  `LaneLayout` rather than keeping a separate coordinate system.

## Building and running

### `game-core` — verified in this environment

```
./gradlew :game-core:test --configure-on-demand
```

All unit tests pass here: merge logic, wave/level difficulty scaling, XP/leveling, the Diamond
drop/collect/expire lifecycle, Laboratory upgrade purchasing, the three-lane geometry
(`LaneLayout` — lanes converge near the base, a spot can reach an adjacent lane's enemy once it's
close in, but not a still-distant one, and every spot stays within the lane length), and full
`GameEngine` tick simulations including a "no defenses eventually lose" scenario and a "a strong
tower clears early waves and levels up the player" scenario.

`--configure-on-demand` is required when running only `:game-core` tasks, because this
repository's network sandbox cannot reach `dl.google.com` (see the limitation below) and Gradle
would otherwise try to evaluate the Android Gradle Plugin for `:app` even though `:game-core`
doesn't need it.

### `app` — build in Android Studio (or any machine with normal internet access)

This module was **not** compiled in this development environment — see below — but is ready to
open and run:

1. Open the repository root in Android Studio.
2. Let Gradle sync (it will download the Android Gradle Plugin, AndroidX/Compose libraries, and
   an Android SDK platform if you don't already have `compileSdk` installed, per `app/build.gradle.kts`).
3. Run the `app` configuration on a device or emulator (`minSdk 26`).

Or from the command line, once you have a normal Android SDK + internet access:

```
./gradlew :app:assembleDebug
```

### Known limitations of this development environment

- **No Android build.** The sandbox this project was built in blocks outbound access to
  `dl.google.com` (Google's Maven repository, which hosts the Android Gradle Plugin and all
  AndroidX/Compose libraries) and has no Android SDK or emulator installed. That means the `app`
  module's OpenGL renderer, touch input, and Compose UI could not be compiled or visually verified
  here — only carefully written and cross-checked by hand. `game-core`, which holds all of the
  actual game rules, has no such dependency and is fully built and tested in this environment.
- **Gradle daemon JVM pin.** `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to a
  JetBrains-vendor JDK 21, auto-downloaded via `api.foojay.io` on first use — also blocked in this
  sandbox, and no matching JDK is installed locally (only OpenJDK 21). Verifying `game-core`'s
  tests here required a local, uncommitted override of that file's `toolchainVendor`; it should
  work as-is anywhere with normal internet access or a JetBrains JDK already installed.

Please build/run `app` locally and report back anything that looks off — happy to iterate.

## Possible next steps

- Tune tower/enemy balance further, now with three simultaneous lanes to defend instead of one.
- Add a drag "ghost"/highlight while dragging a tower before release.
- Persist high score / best wave reached across app restarts, alongside the existing Diamond wallet.
