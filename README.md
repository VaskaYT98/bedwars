# bedwars
comands
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
