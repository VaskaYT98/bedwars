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

/bw:
join — присоединиться к матчу.
arena — управление аренами: create, setregion, pos1, pos2, save, edit, addteam, teamsetspawn, teamsetbed, editsave, editcancel.
purchases — просмотр записей покупок (админ).
npc — NPC управление: pos1, pos2, list, create, remove (и gui через Citizens).
generator setrate <type> <seconds> — задать скорость генератора (админ).
lobby setspawn|tp — управление лобби спавном.
portal set|list|remove — управление порталами.
/party:
invite <player> — пригласить.
accept <player> — принять.
leave — покинуть.
disband — распустить (лидер).



winstreak: %bw_winstreak%
kills: %bw_kills%
deaths: %bw_deaths%
wins: %bw_wins%
losses: %bw_losses%
beds_broken: %bw_beds_broken%
rank: %bw_rank% (информация может подгружаться асинхронно)
level: %bw_level% (вычисляется по стрикам/киллам)
top_*: топы, формат top_<metric>_<index>, напр. %bw_top_wins_1% или %bw_top_kills_3%
map: %bw_map% — текущая арена/шаблон игрока (или lobby)
