package town.sheepy.townyAI.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.object.TownBlockType;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CreateTownCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /createtown <x> <z>");
            return false;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int z = Integer.parseInt(args[1]);
            World world = Bukkit.getWorlds().get(0); // Or hardcode your world name

            String fakePlayerName = "TownBot";
            String townName = "MyTown_" + x + "_" + z;

            TownyUniverse towny = TownyUniverse.getInstance();

            // Check if town already exists
            if (towny.hasTown(townName)) {
                sender.sendMessage("❌ A town with this name already exists!");
                return false;
            }

            Resident resident = towny.getResident(fakePlayerName);
            if (resident == null) {
                resident = new Resident(fakePlayerName);
                towny.registerResident(resident);
            }

            // Create and register town
            Town town = new Town(townName);
            town.setMayor(resident);
            towny.registerTown(town);
            resident.setTown(town);

            // Create town block
            TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world);
            WorldCoord coord = new WorldCoord(townyWorld.getName(), x, z);
            
            // Check if coordinates are already claimed
            if (townyWorld.hasTownBlock(coord)) {
                sender.sendMessage("❌ These coordinates are already claimed!");
                return false;
            }

            TownBlock townBlock = new TownBlock(coord);
            townBlock.setTown(town);
            townBlock.setType(TownBlockType.RESIDENTIAL);

            // Save everything in the correct order
            try {
                towny.getDataSource().saveResident(resident);
                towny.getDataSource().saveTown(town);
                towny.getDataSource().saveTownBlock(townBlock);
                
                // Force Towny to update its cache
                TownyAPI.getInstance().getDataSource().loadTownBlocks();
                TownyAPI.getInstance().getDataSource().loadTowns();
                
                sender.sendMessage("✅ Created town '" + townName + "' at chunk (" + x + ", " + z + ")");
                return true;
            } catch (Exception e) {
                // Clean up if something goes wrong
                if (town != null) towny.removeTown(town);
                if (resident != null) towny.removeResident(resident);
                throw e;
            }

        } catch (Exception e) {
            sender.sendMessage("❌ Failed to create town: " + e.getMessage());
            sender.sendMessage("Stack trace:");
            e.printStackTrace();
            // Print more detailed error information
            for (StackTraceElement element : e.getStackTrace()) {
                sender.sendMessage(element.toString());
            }
        }

        return false;
    }
}
