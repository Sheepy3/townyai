package town.sheepy.townyAI.workflow;
import org.bukkit.command.CommandSender;

public interface Workflow {

    /** Returns the unique code this workflow listens on. */
    String getCode();

    /**initializes workflow*/
    void start();



    /**
     * Called each time someone runs `/ack <code>`.
     * @param sender the issuer of the /ack
     * @return true if the workflow is now finished and should be removed
     */
    boolean onAck(CommandSender sender);


}
