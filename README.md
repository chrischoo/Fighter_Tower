# Fighter Tower

An Android tower-merge defense game, rendered in true 3D (OpenGL ES 2.0). Merge same-tier
towers on a grid into heavier, harder-hitting ones to fight off endless waves of enemies.
Every time the player levels up, later waves get tougher — the core difficulty hook.

## Gameplay

- **Merge grid**: towers sit on a 4x3 grid. Drag one tower onto an identical-tier tower to
  merge them into the next tier (Scout -> Gunner -> Cannon -> Heavy Cannon -> Siege Tower ->
  Railgun -> Juggernaut -> Titan). Drag onto an empty cell to just relocate it.
- **Buy Tower**: spend gold to place a new tier-1 Scout Turret on any empty cell.
- **Waves**: enemies spawn from the back of the lane and march toward your base. Any tower
  within range of an enemy on the lane fires at it automatically.
- **Leveling**: killing enemies grants gold and XP. Leveling up raises the stats of every
  subsequent wave's enemies on top of the normal per-wave difficulty ramp — so staying strong
  requires continuing to merge up, not just surviving.
- **Game over**: if the base's health reaches zero, the run ends; tap Restart to try again.

## Project layout

This is a two-module Gradle project, split specifically so the game rules can be verified
without an Android toolchain:

- **`game-core/`** — pure Kotlin/JVM module, zero Android dependencies. Contains every gameplay
  rule: `TowerTier` (stats per tier), `GridBoard` (placement/merge logic), `Enemy` /
  `WaveGenerator` (wave and difficulty scaling by wave number *and* player level),
  `PlayerProgress` (gold/XP/leveling), and `GameEngine` (the per-frame simulation tick that ties
  it all together and emits `GameEvent`s). Has a full unit test suite.
- **`app/`** — the actual Android application: a hand-written OpenGL ES 2.0 renderer (procedural
  cube/pyramid/plane meshes, a single flat-shaded lighting shader, a camera looking down at the
  board), a touch controller that unprojects screen taps into grid cells for drag-to-merge, a
  `GameViewModel` + Jetpack Compose HUD (level/XP/gold/wave/base-health, buy button, game-over
  screen), and `MainActivity` wiring it together.

## Building and running

### `game-core` — verified in this environment

```
./gradlew :game-core:test --configure-on-demand
```

All 21 unit tests pass here (merge logic, wave/level difficulty scaling, XP/leveling, and a
full `GameEngine` tick simulation including a "no defenses eventually lose" scenario and a
"a strong tower clears early waves and levels up the player" scenario).

`--configure-on-demand` is required when running only `:game-core` tasks, because this
repository's network sandbox cannot reach `dl.google.com` (see the limitation below) and Gradle
would otherwise try to evaluate the Android Gradle Plugin for `:app` even though `:game-core`
doesn't need it.

### `app` — build in Android Studio (or any machine with normal internet access)

This module was **not** compiled in this development environment — see below — but is ready to
open and run:

1. Open the repository root in Android Studio (Ladybird/Koala or newer).
2. Let Gradle sync (it will download the Android Gradle Plugin, AndroidX/Compose libraries, and
   an Android SDK platform if you don't already have `compileSdk 34` installed).
3. Run the `app` configuration on a device or emulator (`minSdk 26`).

Or from the command line, once you have a normal Android SDK + internet access:

```
./gradlew :app:assembleDebug
```

### Known limitation of this development environment

The sandbox this project was built in blocks outbound access to `dl.google.com` (Google's Maven
repository, which hosts the Android Gradle Plugin and all AndroidX/Compose libraries) and has no
Android SDK or emulator installed. That means the `app` module's OpenGL renderer, touch input,
and Compose UI could not be compiled or visually verified here — only carefully written and
cross-checked by hand. `game-core`, which holds all of the actual game rules, has no such
dependency and is fully built and tested in this environment. Please build/run `app` locally and
report back anything that looks off — happy to iterate.

## Possible next steps

- Tune tower/enemy balance further (currently: a single fully-merged Titan tower holds for the
  early game but eventually falls behind, by design — the intent is that you need multiple
  merged towers to keep pace with leveling).
- Add a drag "ghost"/highlight while dragging a tower before release.
- Swap the placeholder procedural geometry for real 3D models/textures.
- Persist high score / best wave reached across app restarts.
