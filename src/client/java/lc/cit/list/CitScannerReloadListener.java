package lc.cit.list;

import lc.cit.config.CitListConfig;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class CitScannerReloadListener implements ResourceManagerReloadListener {

    private static final Identifier ID =  Identifier.fromNamespaceAndPath("cit_list", "cit_scanner_reload");

    /** Call once during client init */
    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(ID, new CitScannerReloadListener());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        if (!CitListConfig.get().scanOnResourceReload) {
            return; 
        }
        CitScanner.refreshCache();
    }
}
