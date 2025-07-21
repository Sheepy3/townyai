package town.sheepy.townyAI.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.workflow.TownInitWorkflow;
import town.sheepy.townyAI.workflow.Workflow;

public class AckCommand implements CommandExecutor {
    private final TownyAI plugin;

    public AckCommand(TownyAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command cmd,
            String label,
            String[] args
    ) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /ack <code>");
            return true;
        }

        String code = args[0];
        Workflow wf = plugin.getWorkflow(code);
        if (wf == null) {
            sender.sendMessage("§cNo workflow for code: " + code);
            return true;
        }

        boolean done = wf.onAck(sender);
        if (done) {
            plugin.removeWorkflow(code);
        }
        return true;
    }
}
