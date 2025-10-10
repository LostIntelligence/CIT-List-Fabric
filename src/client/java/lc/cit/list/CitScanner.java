package lc.cit.list;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CitScanner {

    private static final Gson GSON = new Gson();

    /**
     * Scans all resource packs for CIT definitions depending on renamed items.
     * Returns entries in format: ItemName_NameToTriggerCIT_ResourcePack
     */
    public static List<String> getAllCustomNameCITs() {
        List<String> result = new ArrayList<>();
        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

        try {rm.findResources("items", id -> true).forEach((id, res) -> System.out.println(id.toString()));

            // Get all model JSONs
            Map<Identifier, Resource> resources =
                   rm.findResources("items", id -> id.getPath().endsWith(".json"));
            System.out.println(resources.size());
            System.out.println("A");
            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                System.out.println("B");
                Resource res = entry.getValue();
                System.out.println("C");

                try (InputStreamReader reader =
                             new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {

                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root == null) continue;

                    // Recursively search for any object with "type": "minecraft:select"
                    List<JsonObject> selectBlocks = findSelectBlocks(root);

                    for (JsonObject model : selectBlocks) {
                        if (!model.has("property") || !model.has("component")) continue;

                        String property = model.get("property").getAsString();
                        String component = model.get("component").getAsString();

                        if (!"minecraft:component".equals(property)
                                || !"minecraft:custom_name".equals(component))
                            continue;

                        // Extract CIT cases
                        if (model.has("cases") && model.get("cases").isJsonArray()) {
                            JsonArray cases = model.getAsJsonArray("cases");

                            // Extract item name cleanly (strip models/item/ prefix)
                            String itemName = id.getPath()
                                    .replace("models/item/", "")
                                    .replace(".json", "");

                            String packName = res.getPack().getInfo().title().toString();

                            for (JsonElement el : cases) {
                                if (!el.isJsonObject()) continue;
                                JsonObject caseObj = el.getAsJsonObject();

                                if (caseObj.has("when")) {
                                    String when = caseObj.get("when").getAsString();
                                    result.add(itemName.replace("items/", "") + " - " + when + " - " + packName.replace("literal{", "").replace("}", ""));
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[CIT Scanner] Error parsing " + id + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println(e.toString());
            System.err.println("[CIT Scanner] Resource scan error: " + e.getMessage());
        }
        System.out.println(result.getFirst());
        return result;
    }

    /** Recursively finds all objects with "type": "minecraft:select". */
    private static List<JsonObject> findSelectBlocks(JsonElement element) {
        List<JsonObject> found = new ArrayList<>();
        if (element == null || element.isJsonNull()) return found;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && "minecraft:select".equals(obj.get("type").getAsString())) {
                found.add(obj);
            }
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                found.addAll(findSelectBlocks(e.getValue()));
            }
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                found.addAll(findSelectBlocks(e));
            }
        }
        return found;
    }
}
