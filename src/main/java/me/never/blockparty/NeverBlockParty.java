package me.never.blockparty;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class NeverBlockParty extends JavaPlugin {
    private Game game;

    @Override public void onEnable() {
        saveDefaultConfig();
        String name = getConfig().getString("world.name", "blockparty");
        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldCreator creator = new WorldCreator(name);
            creator.generator(new VoidGenerator());
            world = creator.createWorld();
        }
        Arena arena = new Arena(this, world);
        arena.build();
        game = new Game(this, arena);
        BlockPartyCommand command = new BlockPartyCommand(game);
        getCommand("blockparty").setExecutor(command);
        getCommand("blockparty").setTabCompleter(command);
        getLogger().info("NeverBlockParty 1.0.0 enabled.");
    }

    @Override public void onDisable() { if (game != null) game.shutdown(); }

    static final class VoidGenerator extends ChunkGenerator {
        @Override public void generateNoise(org.bukkit.WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, org.bukkit.generator.chunk.ChunkData data) {}
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateBedrock() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
    }
}