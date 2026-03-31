BedWars plugin (Paper 1.20–1.21)

This repository contains a feature-rich BedWars plugin scaffolded for Paper servers. Features implemented:

- Arena system with YAML templates and instance cloning
- Party & matchmaking with team balancing
- Shop (YAML-driven) with GUI
- SQLite player stats (HikariCP)
- AFK manager, anti-cheat basics, region protection
- Citizens optional integration for NPC join
- Anti-dupe protections: PDC marking, hopper/trade/craft protections, chunk/world unload purge
- Rejoin persistence and basic world cloning optimizer (snapshot stub)

Quick run (build with Maven):

```bash
mvn -f /Project/bedwars clean package
```

Commands (examples):

- `/bw join` — join matchmaking
- `/party invite|accept|leave` — party commands
- `/bw purchases` — admin view purchases
- `/bw npc list|create|remove|gui` — admin NPC management
- `/bw generator setrate <type> <seconds>` — admin generator rate

Configuration files: `config.yml`, `shop.yml`, `arenas/*.yml` under plugin data folder.

Testing & CI: simple JUnit test and GitHub Actions workflow included.
