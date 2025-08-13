package town.sheepy.townyAI.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Chunk;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.terrain.SchematicHelper;
import town.sheepy.townyAI.terrain.TerrainHelper;

public class PlaceStructureCommand implements CommandExecutor {
    private final TownyAI plugin;

    public PlaceStructureCommand(TownyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("§cUsage: /" + label + " <schematic> [rotation]");
            return true;
        }

        String schematic = args[0];
        int rotation = 0;
        try {
            if (args.length == 2) rotation = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cRotation must be an integer (0,90,180,270).");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        // Determine world block corner
        int originX = chunk.getX() << 4;
        int originZ = chunk.getZ() << 4;
        int originY = player.getLocation().getBlockY()-1;
        Location origin = new Location(chunk.getWorld(), originX, originY, originZ);

        sender.sendMessage("§aPlacing schematic '" + schematic + "' with rotation " + rotation);
        try {
            // Call the 4-arg helper
            TerrainHelper.flattenChunk(plugin, chunk, originY);
            SchematicHelper.pasteSchematicFromJar(plugin, schematic, origin, rotation);
            sender.sendMessage("§aSchematic placed.");
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to place schematic: " + ex);
            sender.sendMessage("§cError placing schematic: " + ex.getMessage());
        }
        return true;
    }
}
