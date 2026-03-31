package org.vaska.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Provides fast world cloning / reset by copying folder and creating the world.
 */
public class WorldResetService {
    private final JavaPlugin plugin;

    public WorldResetService(JavaPlugin plugin) { this.plugin = plugin; }

    public java.util.concurrent.CompletableFuture<World> cloneWorldAsync(String templateFolderName, String newWorldName) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                Path templatesDir = plugin.getDataFolder().toPath().resolve("templates");
                Path source = templatesDir.resolve(templateFolderName);

                if (!Files.exists(source)) {
                    source = plugin.getServer().getWorldContainer().toPath().resolve(templateFolderName);
                }

                if (!Files.exists(source)) throw new IOException("Source world not found: " + source);

                Path destPath = plugin.getServer().getWorldContainer().toPath().resolve(newWorldName);

                if (Files.exists(destPath)) {
                    destPath = plugin.getServer().getWorldContainer().toPath()
                            .resolve(newWorldName + "-" + UUID.randomUUID().toString().substring(0, 6));
                }

                final Path finalDest = destPath;
                copyRecursive(source, finalDest);

                java.util.concurrent.CompletableFuture<World> createdFuture = new java.util.concurrent.CompletableFuture<>();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    World w = new WorldCreator(finalDest.getFileName().toString()).createWorld();
                    createdFuture.complete(w);
                });

                return createdFuture.get();

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void copyRecursive(Path source, Path target) throws IOException {
        if (!Files.exists(source)) throw new IOException("Source world not found: " + source);
        Files.walk(source).forEach(s -> {
            try {
                Path rel = source.relativize(s);
                Path dst = target.resolve(rel);
                if (Files.isDirectory(s)) {
                    if (!Files.exists(dst)) Files.createDirectories(dst);
                } else {
                    Files.copy(s, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
