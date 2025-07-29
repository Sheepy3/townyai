package town.sheepy.townyAI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;
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

        // Get the player's current chunk
        var chunk = p.getLocation().getChunk();

        // Compute the non‑tree height
        int height = TerrainHelper.chunkHeightNoTree(chunk);

        // Report back
        p.sendMessage(String.format(
                "§aChunkStats: chunk (%d,%d) non‑tree height = %d",
                chunk.getX(), chunk.getZ(), height
        ));
        return true;
    }
}
