package town.sheepy.townyAI;
import org.bukkit.plugin.java.JavaPlugin;
import town.sheepy.townyAI.commands.ChunkStatsCommand;
import town.sheepy.townyAI.listeners.NewDayListener;
import town.sheepy.townyAI.workflow.TownInitWorkflow;
import java.util.*;

//custom commands
import town.sheepy.townyAI.commands.AckCommand;
import town.sheepy.townyAI.commands.CreateTownCommand;

import town.sheepy.townyAI.store.TownRegistry;
import town.sheepy.townyAI.workflow.Workflow;

public final class TownyAI extends JavaPlugin {
    private TownRegistry registry;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("TownyAI is now enabled!");
        registry = new TownRegistry(this);

        this.getCommand("createtown")
                .setExecutor(new CreateTownCommand(this));
        this.getCommand("ack")
                .setExecutor(new AckCommand(this));
        this.getCommand("chunkstats")
                .setExecutor(new ChunkStatsCommand(this));

        getServer().getPluginManager()
                .registerEvents(new NewDayListener(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("TownyAI is shutting down.");
    }

    public TownRegistry getRegistry() { return registry; }
    private final Map<String, Workflow> workflows = new HashMap<>();

    public void registerWorkflow(Workflow wf) {
        workflows.put(wf.getCode(), wf);
        wf.start();
    }
    public Workflow getWorkflow(String code) {
        return workflows.get(code);
    }
    public void removeWorkflow(String code) {
        workflows.remove(code);
    }

}

