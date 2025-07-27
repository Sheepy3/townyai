package town.sheepy.townyAI.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.store.TownRegistry;

public class NewDayListener implements Listener {
    private final TownyAI plugin;
    private final TownRegistry registry;

    public NewDayListener(TownyAI plugin){
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
    }
    @EventHandler
    public void onNewDay(NewDayEvent event){
        plugin.getLogger().info("Towny Newday triggered - Updating AI towns");
        for (Town townObj : TownyUniverse.getInstance().getTowns()){
            String name = townObj.getName();
            plugin.getLogger().info(name);
        }


    }



}
