package org.vaska.bedwars.matchmaking;

import org.vaska.bedwars.arena.Arena;

import java.util.*;

/**
 * Simple team balancer: assigns team indices from 1..teams aiming to balance sizes.
 */
public final class TeamBalancer {
    public static int assignTeam(Arena arena) {
        int teams = arena.getTeams();
        // choose the team with fewest players
        int best = 1;
        int bestSize = Integer.MAX_VALUE;
        for (int i = 1; i <= teams; i++) {
            final int teamIndex = i; // добавили final переменную
            int size = (int) arena.getTeamAssignments().values().stream()
                    .filter(t -> t == teamIndex)
                    .count();
            if (size < bestSize) {
                bestSize = size;
                best = i;
            }
        }
        return teams;
    }
}