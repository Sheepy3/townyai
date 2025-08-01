package town.sheepy.townyAI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.growth.Growth;
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
            p.sendMessage("§aChunkStats: (" + chunk.getX() + "," + chunk.getZ() +
                    ") non‑tree height = " + height);
            return true;
        }

        String townName = args[0];
        var score = Growth.gradeChunk(chunk, townName, plugin.getRegistry());

        p.sendMessage("§eChunkStats for town §6" + townName + "§e at chunk (" +
                chunk.getX() + "," + chunk.getZ() + "):");
        p.sendMessage(String.format("  › Distance bonus: %.3f", score.distScore()));
        p.sendMessage(String.format("  › Biome penalty: %.3f", score.biomePenalty()));
        p.sendMessage(String.format("  › Height bonus:   %.3f", score.heightBonus()));
        p.sendMessage(String.format("  › Total score:    %.3f", score.totalScore()));
        return true;



    }
}
