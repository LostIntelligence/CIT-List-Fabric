package lc.cit.list;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CitListUnavailableScreen extends Screen {
    private final Screen parent;

    public CitListUnavailableScreen(Screen parent) {
        super(Component.translatable("screen.cit-list.unavailable.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
            Component.translatable("button.cit-list.back"),
                button -> Minecraft.getInstance().gui.setScreen(this.parent))
                .bounds(this.width / 2 - 50, this.height - 45, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.centeredText(this.font, "!", centerX, centerY - 54, 0xFFFF3333);
        context.centeredText(
                this.font,
            Component.translatable("screen.cit-list.unavailable.message"),
                centerX,
                centerY - 18,
                0xFFFFFFFF);
        context.centeredText(
                this.font,
                Component.translatable("screen.cit-list.unavailable.details"),
                centerX,
                centerY,
                0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
