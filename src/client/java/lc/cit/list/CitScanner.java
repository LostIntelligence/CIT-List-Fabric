package lc.cit.list;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class CitScanner {

    private static final Gson GSON = new Gson();

    /**
     * Scans all resource packs for CIT definitions depending on renamed items.
     * Returns entries in format: ItemName_NameToTriggerCIT_ResourcePack
     */
    public static String[][] getAllCustomNameCITs() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        List<String[]> rows = new ArrayList<>();

        try {
            Map<Identifier, Resource> resources = rm.listResources("items", id -> id.getPath().endsWith(".json"));

            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                Resource res = entry.getValue();

                final String packName = extractPackName(res);

                try (InputStreamReader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root == null) continue;

                    // Optional pre-check to skip irrelevant JSON files
                    if (!root.toString().contains("select")) continue;

                    List<JsonObject> selectBlocks = findSelectBlocks(root);

                    for (JsonObject model : selectBlocks) {
                        if (!model.has("property") || !model.has("component")) continue;

                        String property = model.get("property").getAsString();
                        String component = model.get("component").getAsString();

                        // Normalize prefixes once
                        property = property.startsWith("minecraft:") ? property.substring(10) : property;
                        component = component.startsWith("minecraft:") ? component.substring(10) : component;

                        if (!"component".equals(property) || !"custom_name".equals(component)) continue;

                        if (!model.has("cases") || !model.get("cases").isJsonArray()) continue;

                        JsonArray cases = model.getAsJsonArray("cases");

                        // Efficient item name extraction
                        String path = id.getPath();
                        String itemName = path.substring(6, path.length() - 5); // remove "items/" and ".json"

                        for (JsonElement el : cases) {
                            if (!el.isJsonObject()) continue;

                            JsonObject caseObj = el.getAsJsonObject();
                            if (!caseObj.has("when")) continue;

                            JsonElement whenElement = caseObj.get("when");

                            if (whenElement.isJsonPrimitive()) {
                                rows.add(new String[]{ itemName, whenElement.getAsString(), packName });
                            } else if (whenElement.isJsonArray()) {
                                for (JsonElement we : whenElement.getAsJsonArray()) {
                                    if (we.isJsonPrimitive()) {
                                        rows.add(new String[]{ itemName, we.getAsString(), packName });
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[CIT Scanner] Error parsing " + id + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[CIT Scanner] Resource scan error: " + e.getMessage());
        }

        System.out.println("[CIT Scanner] Scan Finished, found " + rows.size() + " entries");
        return rows.toArray(new String[0][3]);
    }

    /** Recursively finds all objects with "type": "minecraft:select". */
    private static List<JsonObject> findSelectBlocks(JsonElement element) {
        List<JsonObject> found = new ArrayList<>();
        if (element == null || element.isJsonNull()) return found;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type")) {
                String type = obj.get("type").getAsString();
                if ("minecraft:select".equals(type) || "select".equals(type)) {
                    found.add(obj);
                }
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

    /** Extracts a clean resource pack name */
    private static String extractPackName(Resource res) {
        String title = res.source().location().title().toString();
        if (title.startsWith("literal{") && title.endsWith("}")) {
            return title.substring(8, title.length() - 1);
        }
        return title;
    }
}
