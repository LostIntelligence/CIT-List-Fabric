package lc.cit.list;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
public class ListIntegrationModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // We don’t return a real config, just our custom list screen
        return parent -> new TextureListScreen(parent);
    }
}