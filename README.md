# MaximizeMTRMod

**A client-side performance mod for Minecraft Transit Railway (MTR 4.x)**

<div align="center">

<kbd>[English](README.md)</kbd> <kbd>[简体中文](README_CN.md)</kbd>

</div>

Reduces rendering load and data sync overhead for MTR 4 through distance-based culling, sync throttling, and chunk-loading smoothing. Drop-in client-side mod — no server-side installation required.

---

## Features

| Feature | How it works | Default |
|---------|-------------|---------|
| **Vehicle Culling** | Skip rendering trains beyond a configurable distance | 128 blocks |
| **Block Entity Culling** | Cull PSDs, signals, PIDS, station names at distance | 56 blocks |
| **Rail Culling** | Skip rail rendering when far; always show when holding MTR tools | 128 blocks |
| **Lift Culling** | Skip lift rendering beyond range via Accessor-read coordinates | 80 blocks |
| **Sync Throttling** | Reduce HTTP poll rate when no transit entities are nearby | 40 blk full / 12 tick throttle |
| **Chunk-Load Smoothing** | Detect trains heading into unloaded chunks, tighten render distance temporarily | 200ms check interval |
| **Frame Interlacing** | Alternate render distance every N frames, distant trains render less often | Every 2 frames |

---

## Installation

1. Install **Fabric Loader 0.14.0+** and **MTR 4.0.0+**
2. Put `mmtr-*.jar` in `.minecraft/mods/`
3. Launch the game

Requires: Fabric API

---

## Configuration

`config/mmtr.json` is generated on first launch:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `vehicleFullRenderDistance` | 40 | Full-detail train render range |
| `vehicleMaxRenderDistance` | 128 | Max train render distance |
| `blockEntityMaxRenderDistance` | 56 | PSD, signal, PIDS render distance |
| `liftMaxRenderDistance` | 80 | Lift render distance |
| `fullSyncDistance` | 40 | Full-speed sync range |
| `reducedSyncDistance` | 80 | Reduced sync range |
| `reducedSyncInterval` | 12 | Throttle interval (ticks) |
| `noSyncDistance` | 160 | Sync cutoff distance |
| `enableChunkPreloading` | true | Chunk-load smoothing |
| `enableRenderFrequencyReduction` | true | Frame interlacing |
| `distantVehicleFrameCycle` | 2 | Interlace cycle |
| `enableDistanceLOD` | true | Train/rail culling |
| `enableBlockEntityCulling` | true | Block entity culling |
| `enableDataSyncOptimization` | true | Sync throttling |
| `enableLiftCulling` | true | Lift culling |
| `enableRailCulling` | true | Rail culling |

---

## Building

```bash
./gradlew build
```

Output: `build/libs/mmtr-0.0.1.jar`

---

## Compatibility

Works with MTR 4.x. Compatible with Sodium, Iris, and Entity Culling — no rendering pipeline modifications.

## License

MIT
