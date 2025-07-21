package town.sheepy.townyAI.store;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class TownRegistry {
    private final File file;
    private final YamlConfiguration cfg;

    public TownRegistry(JavaPlugin plugin){

        //ensures plugin data folder exists
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(),"towns.yml");

        //load or create towns.yml
        if (!file.exists()){
            //make new file
            cfg = new YamlConfiguration();
            save();

        }else{
            //load existing file
            cfg = YamlConfiguration.loadConfiguration(file);
        }
    }

    public boolean addTown(String name, int chunkX, int chunkY){
        String key = "towns." + name.toLowerCase();
        if (cfg.contains(key)) return false; //false if town with name already exists

        cfg.set(key + ".name", name);
        cfg.set(key + ".x", chunkX);
        cfg.set(key + ".z", chunkY);
        save();
        return true;
    }

    public void save(){
        try{
            cfg.save(file);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
