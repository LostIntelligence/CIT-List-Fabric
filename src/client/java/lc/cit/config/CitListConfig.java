package lc.cit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CitListConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("cit-list.json");

    public boolean scanOnResourceReload = true;

    // ---------------- singleton ----------------
    private static CitListConfig INSTANCE;

    public static CitListConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    // ---------------- persistence ----------------
    private static CitListConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                return GSON.fromJson(Files.readString(CONFIG_PATH), CitListConfig.class);
            } catch (Exception ignored) {
            }
        }
        return new CitListConfig();
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException ignored) {
        }
    }
}
