package lc.cit.list;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class CitScanner {
    // ---------- logging ---------
    private static final AtomicInteger FILES_PROCESSED = new AtomicInteger();

    // ---------- Cache ----------
    private static volatile ResultCache CACHE = ResultCache.EMPTY;
    private static final AtomicBoolean SCANNING = new AtomicBoolean(false);

    public static String[][] getCachedResults() {
        return CACHE.results;
    }

    public static boolean isLoaded() {
        return CACHE.loaded;
    }

    public static void refreshCache() {
        FILES_PROCESSED.set(0);

        if (!SCANNING.compareAndSet(false, true)) {
            return;
        }

        CACHE = ResultCache.loading();

        Thread t = new Thread(CitScanner::scanAsync, "CitScanner-Thread");
        t.setDaemon(true);
        t.start();
    }

    // ---------- Async scan ----------
    private static void scanAsync() {
        long start = System.currentTimeMillis();

        try {
            ResultCache result = scanAll();
            CACHE = result;

            System.out.println("[CIT Scanner] Loaded "
                    + result.results.length
                    + " entries in "
                    + (System.currentTimeMillis() - start)
                    + "ms");
            System.out.println("[CIT Scanner] Files proccessed: "
                    + FILES_PROCESSED.get()
                    + " in "
                    + (System.currentTimeMillis() - start)
                    + "ms");

        } finally {
            SCANNING.set(false);
        }
    }

    // ---------- Core scan ----------
    private static ResultCache scanAll() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        Map<Identifier, Resource> resources = rm.listResources("items", id -> id.getPath().endsWith(".json"));

        if (resources.isEmpty()) {
            return ResultCache.EMPTY_LOADED;
        }

        int threads = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "CitScanner-Worker");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        ConcurrentLinkedQueue<String[]> rows = new ConcurrentLinkedQueue<>();
        ConcurrentHashMap<Object, String> packNames = new ConcurrentHashMap<>();

        try {
            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                pool.execute(() -> scanResource(entry.getKey(), entry.getValue(), rows, packNames));
            }
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException ignored) {
            }
        }

        String[][] result = rows.toArray(new String[0][3]);
        return new ResultCache(result, true);
    }

    // ---------- Single resource scan ----------
    private static void scanResource(
            Identifier id,
            Resource res,
            ConcurrentLinkedQueue<String[]> out,
            ConcurrentHashMap<Object, String> packNames) {
        FILES_PROCESSED.incrementAndGet();
        String path = id.getPath();
        if (!path.startsWith("items/")) {
            return;
        }

        String itemName = path.substring(6, path.length() - 5).intern();
        String packName = packNames.computeIfAbsent(
                res.source(),
                k -> extractPackName(res).intern());

        try (JsonReader reader = new JsonReader(
                new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.LENIENT);
            scanJson(reader, itemName, packName, out);

        } catch (Exception ignored) {
        }
    }

    // ---------- Streaming JSON logic ----------
    private static void scanJson(
            JsonReader reader,
            String item,
            String pack,
            ConcurrentLinkedQueue<String[]> out) throws Exception {

        Deque<Boolean> selectStack = new ArrayDeque<>();

        reader.beginObject();

        while (reader.hasNext()) {
            readAny(reader, item, pack, out, selectStack);
        }

        reader.endObject();
    }

    private static void readAny(
            JsonReader reader,
            String item,
            String pack,
            ConcurrentLinkedQueue<String[]> out,
            Deque<Boolean> selectStack) throws Exception {

        JsonToken token = reader.peek();

        switch (token) {
            case BEGIN_OBJECT -> readObject(reader, item, pack, out, selectStack);
            case BEGIN_ARRAY -> readArray(reader, item, pack, out, selectStack);
            default -> reader.skipValue();
        }
    }

    private static void readObject(
            JsonReader reader,
            String item,
            String pack,
            ConcurrentLinkedQueue<String[]> out,
            Deque<Boolean> selectStack) throws Exception {

        boolean isSelect = false;
        boolean valid = false;

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();

            if ("type".equals(name)) {
                String type = reader.nextString();
                isSelect = "select".equals(type) || "minecraft:select".equals(type);
                selectStack.push(isSelect);
            } else if (isSelect && "property".equals(name)) {
                valid = reader.nextString().endsWith("component");
            } else if (isSelect && valid && "component".equals(name)) {
                valid = reader.nextString().endsWith("custom_name");
            } else if (isSelect && valid && "cases".equals(name)) {
                readCases(reader, item, pack, out);
            } else {
                readAny(reader, item, pack, out, selectStack);
            }
        }

        reader.endObject();

        if (!selectStack.isEmpty()) {
            selectStack.pop();
        }
    }

    private static void readArray(
            JsonReader reader,
            String item,
            String pack,
            ConcurrentLinkedQueue<String[]> out,
            Deque<Boolean> selectStack) throws Exception {

        reader.beginArray();

        while (reader.hasNext()) {
            readAny(reader, item, pack, out, selectStack);
        }

        reader.endArray();
    }

    private static void readCases(
            JsonReader reader,
            String item,
            String pack,
            ConcurrentLinkedQueue<String[]> out) throws Exception {

        reader.beginArray();

        while (reader.hasNext()) {
            reader.beginObject();

            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("when".equals(name)) {
                    if (reader.peek() == JsonToken.STRING) {
                        out.add(new String[] { item, reader.nextString(), pack });
                    } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonToken.STRING) {
                                out.add(new String[] { item, reader.nextString(), pack });
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();
        }

        reader.endArray();
    }

    // ---------- Helpers ----------
    private static String extractPackName(Resource res) {
        String title = res.source().location().title().toString();
        // Treat all non-literal packs as server packs
        if (!title.startsWith("literal{")) {
            return "Server Pack";
        }

        // Normal client / mod / builtin packs
        if (title.startsWith("literal{") && title.endsWith("}")) {
            return title.substring(8, title.length() - 1);
        }
        return title;
    }

    // ---------- Cache holder ----------
    private record ResultCache(String[][] results, boolean loaded) {
        static final ResultCache EMPTY = new ResultCache(new String[0][3], false);
        static final ResultCache EMPTY_LOADED = new ResultCache(new String[0][3], true);

        static ResultCache loading() {
            return new ResultCache(EMPTY.results, false);
        }
    }
}
