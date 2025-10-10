package lc.cit.list;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.util.List;
import java.util.ArrayList;

public class TextureListScreen extends Screen {

    private final Screen parent;
    private final List<String> mappings = new ArrayList<>();

    @Override
    protected void init() {
        super.init();
        int y = 0;
        for (String s : mappings) {
            // Add dummy buttons that just display text

            this.addDrawableChild(ButtonWidget.builder(Text.literal(s), button -> {
            }).dimensions(0, y, 500, 20).build());
            y += 25;
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("exit"), button -> {
            close();
        }).dimensions(0, y, 500, 20).build());
    }

    public TextureListScreen(Screen parent) {
        super(Text.literal("Backpack Textures"));
        mappings.addAll(CitScanner.getAllCustomNameCITs());
        this.parent = parent;

        mappings.add("Example entry C → model_c.json");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // draws blur + widgets
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, this.title, this.width / 2, 20,
                0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
