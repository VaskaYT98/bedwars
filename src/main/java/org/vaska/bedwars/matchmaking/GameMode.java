package org.vaska.bedwars.matchmaking;

/**
 * Supported game modes. Each mode defines players per team.
 */
public enum GameMode {
    SOLO(1, 16),
    DOUBLES(2, 16),
    TRIPLES(3, 12),
    QUADS(4, 16);

    private final int playersPerTeam;
    private final int maxPlayers;

    GameMode(int playersPerTeam, int maxPlayers) {
        this.playersPerTeam = playersPerTeam;
        this.maxPlayers = maxPlayers;
    }

    public int getPlayersPerTeam() { return playersPerTeam; }
    public int getMaxPlayers() { return maxPlayers; }
}
