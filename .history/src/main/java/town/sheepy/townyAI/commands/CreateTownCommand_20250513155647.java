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

            Resident resident = towny.getResident(fakePlayerName);
            if (resident == null) {
                resident = new Resident(fakePlayerName);
                towny.registerResident(resident);
            }

            Town town = new Town(townName);
            town.setMayor(resident);
            towny.registerTown(town);
            resident.setTown(town);

            TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world);
            WorldCoord coord = new WorldCoord(townyWorld.getName(), x, z);
            TownBlock townBlock = new TownBlock(coord);
            townBlock.setTown(town);
            townBlock.setType(TownBlockType.RESIDENTIAL);
            town.addTownBlock(townBlock);
            townyWorld.addTownBlock(townBlock);


            towny.getDataSource().saveResident(resident);
            towny.getDataSource().saveTown(town);
            towny.getDataSource().saveTownBlock(townBlock);

            sender.sendMessage("✅ Created town '" + townName + "' at chunk (" + x + ", " + z + ")");
            return true;

        } catch (Exception e) {
            sender.sendMessage("❌ Failed to create town: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
