package lc.cit.list;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;

public class ListIntegrationModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> Minecraft.getInstance().level == null
                ? new CitListUnavailableScreen(parent)
                : new TextureListScreen(parent);
    }
}