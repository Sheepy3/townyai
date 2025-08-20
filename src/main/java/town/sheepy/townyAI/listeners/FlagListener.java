package town.sheepy.townyAI.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

// FlagWar event: adjust the import if your package differs
import io.github.townyadvanced.flagwar.events.CellAttackEvent;

import town.sheepy.townyAI.TownyAI;

public final class FlagListener implements Listener {
    private final TownyAI plugin;

    public FlagListener(TownyAI plugin) {
        this.plugin = plugin;
    }

    /** Fires when an attack flag is placed (i.e., a Towny cell is under attack). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagPlaced(CellAttackEvent event) {
        Block flag = event.getFlagBlock();
        Location loc = flag.getLocation();

        TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
        Town town = (tb != null) ? tb.getTownOrNull() : null;
        String townName = (town != null) ? town.getName() : "<no-town>";

        plugin.getLogger().info(String.format(
                "[WAR] Flag placed at %s (%d,%d,%d) attacking town: %s",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), townName
        ));

        // no workflow start yet — this is just for detection testing
    }
}
