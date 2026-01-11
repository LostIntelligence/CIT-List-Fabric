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
        List<String> resultItem = new ArrayList<>();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        List<String> nameList = new ArrayList<>();
        List<String> conditionList = new ArrayList<>();
        List<String> packList = new ArrayList<>();
        String[][] allResults = null;
        try {
            // rm.findResources("items", id -> true).forEach((id, res) ->
            // System.out.println(id.toString()));
            // Get all model JSONs
            Map<Identifier, Resource> resources = rm.listResources("items", id -> id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                Resource res = entry.getValue();

                try (InputStreamReader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {

                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root == null)
                        continue;

                    // Recursively search for any object with "type": "minecraft:select"
                    List<JsonObject> selectBlocks = findSelectBlocks(root);

                    for (JsonObject model : selectBlocks) {
                        if (!model.has("property") || !model.has("component"))
                            continue;

                        String property = model.get("property").getAsString();
                        String component = model.get("component").getAsString();

                        if (!(property.equals("minecraft:component") || property.equals("component")))
                            continue;
                        if (!(component.equals("minecraft:custom_name") || component.equals("custom_name")))
                            continue;

                        // Extract CIT cases
                        if (model.has("cases") && model.get("cases").isJsonArray()) {
                            JsonArray cases = model.getAsJsonArray("cases");

                            String itemName = id.getPath()
                                    .replace("items/", "")
                                    .replace(".json", "");

                            String packName = res.source().location().title().toString();
                            packName = packName.replace("literal{", "").replace("}", "");

                            for (JsonElement el : cases) {
                                if (!el.isJsonObject())
                                    continue;
                                JsonObject caseObj = el.getAsJsonObject();

                                if (caseObj.has("when")) {
                                    String when = caseObj.get("when").getAsString();
                                    nameList.add(itemName);
                                    conditionList.add(when);
                                    packList.add(packName);
                                    String ite = id.getPath().replace(".json", "");
                                    ite = ite.replaceFirst(".*/", "");
                                    resultItem.add(ite);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[CIT Scanner] Error parsing " + id + ": " + e.getMessage());
                }
            }
            if (nameList.size() > 0) {
                allResults = new String[nameList.size()][3];
                for (int i = 0; i < nameList.size(); i++) {
                   allResults[i][0] = nameList.get(i);
                   allResults[i][1] = conditionList.get(i);
                   allResults[i][2] = packList.get(i);
                }
            }

        } catch (Exception e) {
            System.err.println("[CIT Scanner] Resource scan error: " + e.getMessage());
        }
        System.out.println("[CIT Scanner] Scan Finished");
        return allResults;
    }

    /** Recursively finds all objects with "type": "minecraft:select". */
    private static List<JsonObject> findSelectBlocks(JsonElement element) {
        List<JsonObject> found = new ArrayList<>();
        if (element == null || element.isJsonNull())
            return found;

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
}
