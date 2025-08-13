package town.sheepy.townyAI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.towngrowth.ClaimStrategy;
import town.sheepy.townyAI.terrain.TerrainHelper;

public class ChunkStatsCommand implements CommandExecutor {
    private final TownyAI plugin;

    public ChunkStatsCommand(TownyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }

        var chunk = p.getLocation().getChunk(); //player chunk
        if (args.length == 0) {
            int height = TerrainHelper.chunkHeightNoTree(chunk);
            int exposed = TerrainHelper.countExposedWaterOrIce(chunk);
            boolean watery = exposed > 32;
            int seafloor = TerrainHelper.chunkHeightNoWater(chunk);
            p.sendMessage("§aChunkStats: (" + chunk.getX() + "," + chunk.getZ() +
                    ") non‑tree height = " + height);
            p.sendMessage("  › exposed water/ice columns: " + exposed + (watery ? " §c(>32 → water chunk)" : ""));
            p.sendMessage("  › seafloor minY (ignore water/ice): " + seafloor);
            return true;
        }

        String townName = args[0];
        var score = ClaimStrategy.gradeChunk(chunk, townName, plugin.getRegistry());

        p.sendMessage("§eChunkStats for town §6" + townName + "§e at chunk (" +
                chunk.getX() + "," + chunk.getZ() + "):");
        p.sendMessage(String.format("  › Distance bonus: %.3f", score.distScore()));
        p.sendMessage(String.format("  › Biome penalty: %.3f", score.biomePenalty()));
        p.sendMessage(String.format("  › Height bonus:   %.3f", score.heightBonus()));
        p.sendMessage(String.format("  › Total score:    %.3f", score.totalScore()));
        return true;



    }
}
