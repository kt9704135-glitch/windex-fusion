# Windex Client Recode — PvP Edition (Fabric 1.21.11)

This is a **work-in-progress PvP rebuild**. It currently includes visible-inventory Auto Totem, Auto Sprint, Trigger Bot, and a Target HUD with a Prestige-inspired Arctic GUI. Features from other supplied clients have **not** all been ported. This is an original implementation, not a combined or repacked set of cracked JARs. Grim or other anticheat bypass compatibility has not been validated.

Arctic Edition adds the icy-blue/white interface and animated menu snow. Prestige-inspired combat modules are planned but **not yet integrated**; this is currently the Fusion scouting foundation.

Single-client scouting mod prototype written from scratch for Minecraft Java **1.21.11**. It is not a bundle, patch, or repack of the five supplied cracked JAR files. Those binaries were inspected only for compatibility and feature ideas; no executable code from them is included. All five report Fabric 1.21.11, but at least two contain overlapping classes that would collide if simply unpacked and merged.

## How to build

- Requirements: Java **21**, Gradle **9.2.1**, Fabric Loader **0.19.0+**, Fabric API **0.141.6+1.21.11**.
- Import the project with IntelliJ IDEA or VS Code as a Gradle project; run `gradle build`.
- Alternatively, upload this entire folder to a GitHub repository and run **Actions > Build Windex Fusion > Run workflow**. The GitHub Actions workflow installs the JDK and Gradle, builds the mod, and uploads the resulting JAR.
- Install only the `windex-fusion-0.1.0.jar` from `build/libs/` into the `mods` folder of a matching Fabric 1.21.11 installation, together with Fabric API. Do **not** install the five input JARs alongside it expecting their features to be included.

> The Fabric-dependent build could not be executed in the artifact environment because Gradle and reachable Fabric/Minecraft artifact repositories were unavailable. This source project is therefore **not yet confirmed to compile or launch in-game**. The standard-Java detection logic did pass its self-tests. Build errors from the workflow should be fixed before calling this a release.

## Features implemented

| Module | Current behavior | Reliability |
| --- | --- | --- |
| Spawner finder | Correlates repeated ordinary smoke + flame within the same block. Also marks a spawner block entity if the server sent it. | Particle candidate is **unverified**; received block entity is direct evidence. |
| Player radar | Shows player entities the client actually knows about; a separate weak marker collects repeated light updates. | Known entity is confirmed; light updates **do not establish player presence**. |
| Sus surface | Scans received underground blocks incrementally, picks the strongest construction-evidence column, and pins the surface at the **same X/Z**, never a neighboring chunk. | False positives from ruins, naturally formed blocks, and structures remain possible. |
| Subsurface overlay | Always-on radar shows loaded subsurface clues and distances, including clues beneath deepslate. | HUD overlay, **not** genuine through-wall 3D rendering. |
| Cave analyzer | Marks chunks with significant sampled underground air volume. | Natural caves are common; this is a coarse first-pass analyzer. |
| Terrain analyzer | Marks concentrations of construction-like surface blocks in sampled chunks. | Does not reconstruct original terrain or know the world seed. |
| Storage radar | Identifies storage block entities already present in received chunk data. | Does not reveal unopened storages the server has hidden. |

The scanner is deliberately bounded by the `scanBlocksPerTick` budget. It never asks the server for special information, sends no bypass packets, and does not claim to defeat server-side anti-xray or reveal players/spawners that were never transmitted to the client.

## GUI and config

- **Right Shift** opens the screen. **Esc** closes it.
- Tabs: **Finders**, **Analyzers**, **Config**.
- Colour-accented translucent dark background, packaged Noto Sans font for headings, module toggles, radar, and confidence/coordinate labels.
- Auto-saved file: `.minecraft/config/windex-fusion.json`.
- Profiles: **DONUT** (conservative starter), **PERFORMANCE**, **BALANCED**, and **DEEP**.

**DONUT profile (unverified server-specific starter settings):**

```json
{
  "scanRadiusChunks": 4,
  "scanBlocksPerTick": 650,
  "susMinEvidence": 7,
  "spawnerMinEachParticle": 4,
  "spawnerWindowSeconds": 30,
  "lightUpdatesForAlert": 9,
  "lightWindowSeconds": 25,
  "renderDistanceBlocks": 192,
  "maxHudMarkers": 8,
  "showHud": true,
  "showCoordinates": true,
  "accentRgb": 10185983,
  "preset": "DONUT"
}
```

The live config also includes an `enabled` object with seven module toggles and uses a numeric RGB accent. The GUI modifies and saves real settings; adjusting JSON manually lets you control scan blocks per tick, render range, or particle evidence window.

## What *cannot* be promised

- There is no verified working **DonutSMP anti-xray, fake-deepslate, or player bypass** in this prototype. A client cannot reliably recover arbitrary objects if the server never sends any distinguishing information.
- Spawner smoke/flame is not a unique signature. The code avoids some known torches and fires, requires multiple samples, and labels an unconfirmed candidate clearly.
- Light updates may be caused by non-player effects and do not prove a hidden player.
- The five cracked client binaries cannot safely or legally be treated as universally mergeable original source. Their UI, modules, and bypasses are **not** copied into this output.
- The project has not been gameplay-tested on DonutSMP. The server may prohibit these sorts of modifications; check its rules before use.

## Pure Java self-test (no Minecraft dependencies)

From the root of this project, compile the four files in `src/main/java/dev/windex/fusion/analysis/` except `WorldAnalyzer.java`, together with `src/test/java/dev/windex/fusion/analysis/AnalysisSelfTest.java`, then run `dev.windex.fusion.analysis.AnalysisSelfTest`. It checks paired-particle thresholds, expiry windows, underground-to-surface coordinate projection, activity windows, and marker-distance filtering.

Noto Sans is © Google LLC, included under the SIL Open Font License 1.1; see `NOTO-FONT-LICENSE.txt`.
