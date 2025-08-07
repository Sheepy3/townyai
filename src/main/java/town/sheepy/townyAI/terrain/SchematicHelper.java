package town.sheepy.townyAI.terrain;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import town.sheepy.townyAI.TownyAI;
import town.sheepy.townyAI.towngrowth.TownGrowthHelper.*;
import static town.sheepy.townyAI.towngrowth.TownGrowthHelper.rotationOffset;

import java.io.*;



public class SchematicHelper {
    public static void pasteSchematicFromJar(
            TownyAI plugin,
            String resourcePath,
            Location targetLoc,
            int rotationDegrees
    ) throws Exception {
        // 1. Extract resource to disk (or reuse existing)
        File schematic = ensureSchematicFile(plugin, resourcePath);

        // 2. Detect format from the File
        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            throw new IllegalArgumentException("Unknown schematic format: " + schematic.getName());
        }

        // 3. Read the schematic from the File, not the JAR stream
        Clipboard clipboard;
        try (FileInputStream fis = new FileInputStream(schematic)) {
            clipboard = format.getReader(fis).read();
        }

        World weWorld = BukkitAdapter.adapt(targetLoc.getWorld());

        ClipboardHolder holder = new ClipboardHolder(clipboard);

        //schematic rotation
        AffineTransform transform = new AffineTransform().rotateY(rotationDegrees);
        holder.setTransform(transform);
        IntVector2 off = rotationOffset(rotationDegrees);
        EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(weWorld)
                .build();

        Operation pasteOp = holder
                .createPaste(editSession)
                .to(BlockVector3.at(
                        targetLoc.getBlockX() + off.x(),
                        targetLoc.getBlockY()+1,
                        targetLoc.getBlockZ() + off.z()
                ))
                .ignoreAirBlocks(false)
                .build();
        Operations.complete(pasteOp);

        editSession.close();
    }
    public static void pasteSchematicFromJar( //overload if no rotation is passed
            TownyAI plugin,
            String resourcePath,
            Location targetLoc
    ) throws Exception {
        // simply delegate to the full API with 0 degrees
        pasteSchematicFromJar(plugin, resourcePath, targetLoc, 0);
    }

    public static File ensureSchematicFile(JavaPlugin plugin, String resourcePath) throws IOException {
        String name = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        File out = new File(plugin.getDataFolder(), name);
        if (!out.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource(resourcePath);
                 OutputStream outStream = new FileOutputStream(out)) {
                if (in == null) {
                    throw new FileNotFoundException("Resource not found: " + resourcePath);
                }
                in.transferTo(outStream);
            }
        }
        return out;
    }
}
