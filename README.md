# VajraFX

Klientský mod pro **GregTech: New Horizons** (Minecraft 1.7.10), který Vajře přidává efekty těžení: energetický paprsek ze špičky při levém tlačítku a průhlednou pulse wave při pravém tlačítku.

*English: client-side Minecraft 1.7.10 mod for GT: New Horizons. Adds a pixel-art energy beam from the Vajra's tip while mining (left click) and a see-through pulse wave on right click. Affects only the Vajra; no server install needed.*

## Co mod dělá

**Levé tlačítko: paprsek**
- Paprsek vychází přímo ze špičky Vajry v ruce a vede do bloku v zaměřovači.
- Má zářící jádro s proudící pixelovou energií, modrou auru, dvě spirálová vlákna, záblesk na hrotu a záři s kroužkem v místě dopadu.
- Vajra při těžení použije animovanou texturu (proudící energie ve smyčce a v krystalu).
- Vypíná běžné „mlácení“ rukou a poskakování Vajry po každém vytěženém bloku.
- Po puštění tlačítka animace doběhne do konce cyklu a paprsek plynule pohasne.

**Pravé tlačítko: pulse wave**
- Vyletí ve stejném snímku, kdy klikneš, ale jen tehdy, když pravým tlačítkem opravdu zasáhneš blok.
- Průhledné pixelové kroužky letí ze špičky přesně do místa kliku a skončí zábleskem s rázovým kroužkem.
- Při rychlém klikání poletí víc vln najednou.

S jakýmkoli jiným předmětem než Vajrou se nic nemění.

## Požadavky

- Minecraft 1.7.10 s Forge, GT: New Horizons (vyzkoušeno na 2.9.0-beta-3 s Javou 25)
- Jen klient: na server se mod instalovat nemusí

## Instalace

1. Stáhni `VajraFX-<verze>.jar` z [Releases](../../releases).
2. Vlož ho do složky `mods` ve své GTNH instanci (starší verze VajraFX smaž).
3. Spusť hru. Při prvním spuštění se vytvoří `config/vajrafx.cfg`.

Po zhruba 2 sekundách těžení Vajrou napíše mod jednou do chatu diagnostický řádek `[VajraFX] ...`. Když vše funguje, vypni ho v configu přes `debugChat=false`.

## Nastavení (`config/vajrafx.cfg`)

Změny se projeví po restartu hry.

| Sekce | Položka | Výchozí | Popis |
|---|---|---|---|
| `beam` | `beamEnabled` | `true` | Zapne nebo vypne paprsek |
| `beam` | `beamThickness` | `2.5` | Tloušťka paprsku u hrotu |
| `beam` | `beamOpacity` | `1.0` | Průhlednost paprsku (nižší = průhlednější) |
| `beam` | `beamHelix` | `true` | Spirálová vlákna kolem paprsku |
| `pulse` | `pulseEnabled` | `true` | Zapne nebo vypne pulse wave |
| `pulse` | `pulseTicks` | `6` | Doba letu vlny k bloku (1 tick = 0,05 s) |
| `pulse` | `pulseRadius` | `0.3` | Velikost vlny u bloku (v blocích) |
| `pulse` | `pulseOpacity` | `0.2` | Průhlednost vlny (nižší = průhlednější) |
| `pulse` | `pulseRecoilDistance`, `pulseRecoilAngle` | `0.08`, `6` | Cuknutí Vajry při výstřelu |
| `pose` | `aimOffsetX/Y/Z`, `aimRotY/Z` | | Poloha a natočení Vajry při těžení |
| `pose` | `recoilDistance`, `recoilAngle` | `0.05`, `4` | Cuknutí při vytěžení bloku |
| `tip` | `tipU`, `tipV` | `0.02`, `0.02` | Kde je špička na textuře Vajry (0 = levý / horní okraj) |
| `tip` | `tipDepth` | `0.6` | Jak daleko před kamerou paprsek začíná |
| `tip` | `fallbackTipX`, `fallbackTipY` | `0.66`, `0.52` | Záložní poloha špičky na obrazovce, pokud nefunguje renderer v ruce |
| `debug` | `debugChat` | `true` | Diagnostický řádek v chatu |

## Vlastní textury

Textury jde nahradit vlastním resource packem se stejnými cestami:

- `assets/vajrafx/textures/items/vajra_active_0.png` až `vajra_active_7.png`: Vajra při těžení (8 snímků, 1 snímek za tick)
- `assets/vajrafx/textures/items/vajra_charge_2.png`: záblesk krystalu při výstřelu pulse wave
- `assets/vajrafx/textures/fx/beam_core.png`, `beam_glow.png`, `flare.png`, `ring.png`: efekty paprsku a vlny

## Jak to funguje

- Vajru mod pozná podle třídy `ToolVajra`, takže GregTech nepotřebuje jako závislost při sestavení.
- Vlastní `IItemRenderer` vykreslí Vajru v ruce a zjistí, kde na obrazovce je její špička.
- Efekty se kreslí v průchodu světa (`RenderWorldLastEvent`). Poloha špičky se převede na bod těsně před kamerou, takže paprsek i vlna vyjdou přesně z hrotu. Tenhle způsob funguje i s Angelicou, která v GTNH mění vykreslování.

## Sestavení ze zdrojáků

Projekt je postavený na šabloně GTNH ExampleMod (RetroFuturaGradle).

- Gradle potřebuje Javu 25, sestavení navíc používá toolchainy Java 17 a Java 8 (Gradle je umí stáhnout sám).
- `./gradlew build`, hotový `.jar` je v `build/libs/`.

## Licence

[MIT](LICENSE) © 2026 Sixxo
