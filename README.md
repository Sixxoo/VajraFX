# VajraFX

A client-side mod for **GregTech: New Horizons** (Minecraft 1.7.10) that adds mining effects to the Vajra: an energy beam from the tip on left click and a see-through pulse wave on right click.

## Features

- **Left click:** an energy beam from the Vajra's tip to the block you are mining
- **Right click on a block:** an instant see-through pulse wave to the spot you clicked
- **Animated Vajra** texture while mining
- **Smooth mining:** no arm hammering, the beam fades out when you let go

Only the Vajra is affected.

## Requirements

- Minecraft 1.7.10 with Forge, GT: New Horizons (tested on 2.9.0-beta-3 with Java 25)
- Client only: no need to install it on the server

## Installation

1. Download the latest `vajrafx-<version>.jar` from [Releases](https://github.com/Sixxoo/VajraFX/releases/latest).
2. Put it into the `mods` folder of your GTNH instance (remove any older VajraFX versions).
3. Start the game. `config/vajrafx.cfg` is created on the first launch.

After about 2 seconds of mining with the Vajra, the mod prints one diagnostic line `[VajraFX] ...` in chat. Once everything works, turn it off in the config with `debugChat=false`.

## Configuration (`config/vajrafx.cfg`)

Changes take effect after restarting the game.

| Section | Option | Default | Description |
|---|---|---|---|
| `beam` | `beamEnabled` | `true` | Turns the beam on or off |
| `beam` | `beamThickness` | `2.5` | Beam thickness at the tip |
| `beam` | `beamOpacity` | `1.0` | Beam opacity (lower = more see-through) |
| `beam` | `beamHelix` | `true` | Spiralling strands around the beam |
| `pulse` | `pulseEnabled` | `true` | Turns the pulse wave on or off |
| `pulse` | `pulseTicks` | `6` | Time for the wave to reach the block (1 tick = 0.05 s) |
| `pulse` | `pulseRadius` | `0.3` | Size of the wave at the block (in blocks) |
| `pulse` | `pulseOpacity` | `0.2` | Wave opacity (lower = more see-through) |
| `pulse` | `pulseRecoilDistance`, `pulseRecoilAngle` | `0.08`, `6` | Vajra kick when firing |
| `pose` | `aimOffsetX/Y/Z`, `aimRotY/Z` | | Vajra position and rotation while mining |
| `pose` | `recoilDistance`, `recoilAngle` | `0.05`, `4` | Kick when a block breaks |
| `tip` | `tipU`, `tipV` | `0.02`, `0.02` | Where the tip is on the Vajra texture (0 = left / top edge) |
| `tip` | `tipDepth` | `0.6` | How far in front of the camera the beam starts |
| `tip` | `fallbackTipX`, `fallbackTipY` | `0.66`, `0.52` | Fallback tip position on screen if the hand renderer does not run |
| `debug` | `debugChat` | `true` | Diagnostic line in chat |

## License

[MIT](LICENSE) © 2026 Sixxo
