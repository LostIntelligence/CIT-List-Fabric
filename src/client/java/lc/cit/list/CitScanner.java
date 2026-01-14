package lc.cit.list;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CitScanner {

    // ---------- Cache (safe publication) ----------
    private static volatile ResultCache CACHE = ResultCache.EMPTY;

    public static String[][] getCachedResults() {
        return CACHE.results;
    }

    public static boolean isLoaded() {
        return CACHE.loaded;
    }

    public static void refreshCache() {
        CACHE = ResultCache.loading();

        Thread thread = new Thread(() -> {
            ResultCache newCache = scanAll();
            CACHE = newCache;
            System.out.println("[CIT Scanner] Cache refreshed, entries: " + newCache.results.length);
        }, "CitScanner-Thread");

        thread.setDaemon(true);
        thread.start();
    }

    // ---------- Core scanning ----------
    private static ResultCache scanAll() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        List<String[]> rows = new ArrayList<>();

        // Cache pack names per resource source
        Map<Object, String> packNameCache = new HashMap<>();

        try {
            Map<Identifier, Resource> resources =
                    rm.listResources("items", id -> id.getPath().endsWith(".json"));

            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                Resource res = entry.getValue();

                JsonObject root;
                try (InputStreamReader reader =
                             new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    root = GSON.fromJson(reader, JsonObject.class);
                } catch (Exception e) {
                    continue;
                }

                if (root == null) {
                    continue;
                }

                // Extract pack name once per pack
                Object packKey = res.source();
                String packName = packNameCache.computeIfAbsent(
                        packKey, k -> extractPackName(root, res)
                );

                List<JsonObject> selectBlocks = new ArrayList<>(2);
                findSelectBlocks(root, selectBlocks);

                if (selectBlocks.isEmpty()) {
                    continue;
                }

                String path = id.getPath();
                if (!path.startsWith("items/")) {
                    continue;
                }

                String itemName = path.substring(6, path.length() - 5);

                for (JsonObject model : selectBlocks) {
                    if (!isCustomNameSelector(model)) {
                        continue;
                    }

                    JsonArray cases = model.getAsJsonArray("cases");
                    for (JsonElement el : cases) {
                        if (!el.isJsonObject()) continue;
                        JsonObject caseObj = el.getAsJsonObject();
                        if (!caseObj.has("when")) continue;

                        JsonElement when = caseObj.get("when");
                        if (when.isJsonPrimitive()) {
                            rows.add(new String[]{
                                    itemName,
                                    when.getAsString(),
                                    packName
                            });
                        } else if (when.isJsonArray()) {
                            for (JsonElement we : when.getAsJsonArray()) {
                                if (we.isJsonPrimitive()) {
                                    rows.add(new String[]{
                                            itemName,
                                            we.getAsString(),
                                            packName
                                    });
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CIT Scanner] Resource scan error: " + e.getMessage());
        }

        return new ResultCache(rows.toArray(new String[0][3]), true);
    }

    // ---------- Helpers ----------

    private static final Gson GSON = new Gson();

    /**
     * Accumulator-style traversal to avoid list churn.
     */
    private static void findSelectBlocks(JsonElement element, List<JsonObject> out) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            JsonElement typeEl = obj.get("type");
            if (typeEl != null && typeEl.isJsonPrimitive()) {
                String type = typeEl.getAsString();
                if ("minecraft:select".equals(type) || "select".equals(type)) {
                    out.add(obj);
                }
            }

            for (JsonElement child : obj.asMap().values()) {
                findSelectBlocks(child, out);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                findSelectBlocks(e, out);
            }
        }
    }

    private static boolean isCustomNameSelector(JsonObject model) {
        if (!model.has("property") || !model.has("component") || !model.has("cases")) {
            return false;
        }

        String property = normalize(model.get("property").getAsString());
        String component = normalize(model.get("component").getAsString());

        return "component".equals(property)
                && "custom_name".equals(component)
                && model.get("cases").isJsonArray();
    }

    private static String normalize(String s) {
        return s.startsWith("minecraft:") ? s.substring(10) : s;
    }

    /**
     * Uses already-parsed JSON, avoids reopening the resource.
     */
    private static String extractPackName(JsonObject root, Resource res) {
        try {
            JsonObject pack = root.getAsJsonObject("pack");
            if (pack != null && pack.has("description")) {
                JsonElement desc = pack.get("description");
                if (desc.isJsonPrimitive()) {
                    return desc.getAsString();
                }
                if (desc.isJsonObject() && desc.getAsJsonObject().has("text")) {
                    return desc.getAsJsonObject().get("text").getAsString();
                }
                return desc.toString();
            }
        } catch (Exception ignored) {
        }

        String title = res.source().location().title().toString();
        if (title.startsWith("literal{") && title.endsWith("}")) {
            return title.substring(8, title.length() - 1);
        }
        return title;
    }

    // ---------- Immutable cache holder ----------
    private record ResultCache(String[][] results, boolean loaded) {
        static final ResultCache EMPTY = new ResultCache(new String[0][3], false);

        static ResultCache loading() {
            return new ResultCache(EMPTY.results, false);
        }
    }
}
