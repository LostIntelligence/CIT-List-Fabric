package lc.cit.list;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class CITListClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
  	CitScannerReloadListener.register();
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.
		KeyMapping.Category CATEGORY = new KeyMapping.Category(
				Identifier.fromNamespaceAndPath("cit-list", "openlist_category"));

		KeyMapping openCitList = KeyBindingHelper.registerKeyBinding(
				new KeyMapping(
						"key.cit-list.openlist", // The translation key for the key mapping.
						InputConstants.Type.KEYSYM, // // The type of the keybinding; KEYSYM for keyboard, MOUSE for
													// mouse.
						GLFW.GLFW_KEY_J, // The GLFW keycode of the key.
						CATEGORY // The category of the mapping.
				));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openCitList.consumeClick()) {
				if (client.player != null) {
					Minecraft.getInstance().setScreen(new TextureListScreen(client.screen));
				}
			}
		});

		// When Mod is ready
		System.out.println("[CIT List] Initialized");
	}
}