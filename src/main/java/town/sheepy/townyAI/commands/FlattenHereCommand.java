package town.sheepy.townyAI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.terrain.TerrainHelper;

public class FlattenHereCommand implements CommandExecutor {
    private final TownyAI plugin;

    public FlattenHereCommand(TownyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }
        if (!sender.hasPermission("townyai.admin")) {
            sender.sendMessage("§cYou don't have permission to use this.");
            return true;
        }

        int groundY;
        if (args.length >= 1) {
            try {
                groundY = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUsage: /" + label + " [y]");
                return true;
            }
        } else {
            groundY = p.getLocation().getBlockY();
        }

        var chunk = p.getLocation().getChunk();
        sender.sendMessage("§eFlattening chunk §7(" + chunk.getX() + "," + chunk.getZ() + ")§e to Y=" + groundY + " …");

        // Uses water-aware flatten (platformTop_1 + platformLegs_1 when watery)
        TerrainHelper.flattenChunk(plugin, chunk, groundY);

        sender.sendMessage("§aDone.");
        return true;
    }
}
