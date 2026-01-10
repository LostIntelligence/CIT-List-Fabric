package lc.cit.list;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TextureListScreen extends Screen {
    private final Screen parent;
    private final List<String> mappings;
    private final List<String> itemlist;
    private final List<String> conditionList;
    private MappingsListWidget list;

    public TextureListScreen(Screen parent) {
        super(Component.literal("Renameable CIT Textures"));
        this.parent = parent;
        BundleWrapper bundle = CitScanner.getAllCustomNameCITs();
        this.mappings = bundle.formatedStringLines;
        this.itemlist = bundle.itemNames;
        this.conditionList = bundle.toRenameTrigger;
    }

    @Override
    protected void init() {
        super.init();

        int fontHeight = this.font.lineHeight;
        int itemHeight = fontHeight + 4; // small padding
        int top = 20;
        int bottom = top + itemHeight;

        this.list = new MappingsListWidget(this.minecraft, this.width - 10, this.height - 50, top, bottom);

        for (int i = 0; i < mappings.size(); i++) {
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", itemlist.get(i));
            Item item = BuiltInRegistries.ITEM.getValue(id);
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(conditionList.get(i)));
            this.list.addMapping(Component.literal(mappings.get(i)), stack);

        }

        this.addWidget(list);
        this.setInitialFocus(list);

        Button exitButton = Button.builder(
                Component.literal("Exit"),
                button -> {
                    System.out.println("Exit button clicked");
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(this.width / 2 - 50, this.height - 25, 100, 20)
                .createNarration(supplier -> Component.literal("Exit button"))
                .build();

        this.addRenderableWidget(exitButton);

    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (this.list.mouseDragged(click, offsetX, offsetY)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        // Click c = new Click(mouseX, mouseY, new MouseInput(button,0));
        if (super.mouseClicked(click, doubled)) {
            return true; // ✅ buttons and other widgets get priority
        }
        return this.list.mouseClicked(click, doubled); // ✅ fallback to list
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {

        // Title
        context.drawCenteredString(
                this.font, this.title, this.width / 2, 3, 0xFFFFFFFF);

        int headerY = 20;
        int padding = 8;
        int column1X = padding;
        int column2X = this.width / 3;
        int column3X = 2 * this.width / 3;

        this.list.render(context, mouseX, mouseY, delta);
        // List Header
        context.fill(0, headerY - 2, this.width, headerY + this.font.lineHeight + 2, 0xFF333333); // dark
                                                                                                          // background
        context.drawString(this.font, "Item to Rename", column1X, headerY, 0xFFFFFFFF, true);
        context.drawString(this.font, "New Name", column2X, headerY, 0xFFFFFFFF, true);
        context.drawString(this.font, "Resourcepack", column3X, headerY, 0xFFFFFFFF, true);


        context.fill(0, this.height - 30, this.width, this.height - 30 + 3, 0xFFAAAAAA); // light gray line
        // Everything Else
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ---------------------------------------------------------------
    // Scrollable list widget
    // ---------------------------------------------------------------
    private static class MappingsListWidget extends ObjectSelectionList<MappingsListWidget.TextEntry> {
        public MappingsListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, height, top, bottom);

        }

        public void addMapping(Component text, ItemStack stack) {
            this.addEntry(new TextEntry(text, stack));
        }

        @Override
        public int getRowWidth() {
            return this.width - 12; // leave space for scrollbar
        }

        public static class TextEntry extends ObjectSelectionList.Entry<TextEntry> {
            private final Component text;
            private final ItemStack stack;

            public TextEntry(Component text, ItemStack stack) {
                this.text = text;
                this.stack = stack;
            }

            @Override
            public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
                Minecraft mc = Minecraft.getInstance();
                int color = hovered ? 0xFFFFFFA0 : 0xFFFFFFFF;
                int entryHeight = getContentHeight();
                int entryWidth = getContentWidth();
                int textY = getY() + (entryHeight - mc.font.lineHeight) / 2;

                int iconX = getX() + 4;
                int iconY = getY() + (entryHeight - 16) / 2;

                context.renderItem(stack, iconX, iconY);

                // ✅ Ensure text width fits in visible area
                String visible = mc.font.plainSubstrByWidth(text.getString(), entryWidth - 10);
                int textX = iconX + 20; // leave room for icon + padding
                context.drawString(mc.font, visible, textX, textY, color, false);
            }

            @Override
            public Component getNarration() {
                return text;
            }

        }
    }
}
