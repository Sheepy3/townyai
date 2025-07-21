package town.sheepy.townyAI.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.exceptions.TownyException;

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
            World world = Bukkit.getWorlds().get(0);

            String fakePlayerName = "TownBot";
            String townName = "MyTown_" + x + "_" + z;

            TownyUniverse towny = TownyUniverse.getInstance();
            TownyAPI townyAPI = TownyAPI.getInstance();

            // Check if town already exists
            if (townyAPI.getTown(townName) != null) {
                sender.sendMessage("❌ A town with this name already exists!");
                return false;
            }

            // Get or create resident
            Resident resident = townyAPI.getResident(fakePlayerName);
            if (resident == null) {
                resident = new Resident(fakePlayerName);
                towny.registerResident(resident);
            }

            // Create town
            Town town = new Town(townName);
            town.setMayor(resident);
            town.setBoard("Welcome to " + townName);
            town.setTaxes(0.0);
            town.setPublic(true);
            towny.registerTown(town);
            resident.setTown(town);

            // Create town block
            TownyWorld townyWorld = townyAPI.getTownyWorld(world);
            WorldCoord coord = new WorldCoord(townyWorld.getName(), x, z);

            // Check if coordinates are already claimed
            if (townyWorld.hasTownBlock(coord)) {
                sender.sendMessage("❌ These coordinates are already claimed!");
                return false;
            }

            // Create and set up town block
            TownBlock townBlock = new TownBlock(coord);
            townBlock.setTown(town);
            townBlock.setType(TownBlockType.RESIDENTIAL);

            // Save everything
            try {
                towny.getDataSource().saveResident(resident);
                towny.getDataSource().saveTown(town);
                towny.getDataSource().saveTownBlock(townBlock);
                
                // Force Towny to update its cache
                towny.getDataSource().loadTownBlocks();
                towny.getDataSource().loadTowns();
                
                sender.sendMessage("✅ Created town '" + townName + "' at chunk (" + x + ", " + z + ")");
                return true;
            } catch (Exception e) {
                // Clean up if something goes wrong
                if (town != null) {
                    towny.getDataSource().removeTown(town);
                    towny.getDataSource().removeTownBlocks(town);
                }
                if (resident != null) {
                    towny.getDataSource().removeResident(resident);
                }
                throw e;
            }

        } catch (Exception e) {
            sender.sendMessage("❌ Failed to create town: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
