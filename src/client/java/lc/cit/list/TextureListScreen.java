package lc.cit.list;

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
    private MappingsListWidget list;
    private static int column1X;
    private static int column2X;
    private static int column3X;
    private String[][] citArray;

    public TextureListScreen(Screen parent) {
        super(Component.literal("Renameable CIT Textures"));
        this.parent = parent;
        this.citArray = CitScanner.getAllCustomNameCITs();
    }

    @Override
    protected void init() {
        super.init();

        int padding = 8;
        column1X = padding;
        column2X = this.width / 3;
        column3X = 2 * this.width / 3;
        int fontHeight = this.font.lineHeight;
        int itemHeight = fontHeight + 4; // small padding
        int top = 20 + fontHeight + 2;
        int bottom = top + itemHeight;

        this.list = new MappingsListWidget(this.minecraft, this.width - 10, this.height - 60, top, bottom, column1X,
                column2X,
                column3X);

        for (int i = 0; i < citArray.length; i++) {
            String itemName = citArray[i][0];
            String newName = citArray[i][1];
            String packName = citArray[i][2];

            Identifier id = Identifier.fromNamespaceAndPath("minecraft", itemName);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null)
                continue;

            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(newName));

            this.list.addMapping(stack, itemName, newName, packName);
        }
        // Compute column positions
        calculateColumnPositions();

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

    private void calculateColumnPositions() {

        int max1 = 0;
        int max2 = 0;
        int max3 = 0;
        Minecraft mc = Minecraft.getInstance();
        for (MappingsListWidget.TextEntry entry : list.children()) {
            int w1 = mc.font.width(entry.itemName);
            int w2 = mc.font.width(entry.newName);
            int w3 = mc.font.width(entry.packName);

            max1 = Math.max(max1, w1);
            max2 = Math.max(max2, w2);
            max3 = Math.max(max3, w3);
        }

        // Convert padding in pixels (approx 5 chars)
        int padPixels = mc.font.width("AAAAA");

        column1X = 8; // fixed left margin
        column2X = column1X + max1 + padPixels + 20; // +20 for icon
        column3X = column2X + max2 + padPixels;
    }

    // ---------------------------------------------------------------
    // Scrollable list widget
    // ---------------------------------------------------------------
    private static class MappingsListWidget extends ObjectSelectionList<MappingsListWidget.TextEntry> {

        public MappingsListWidget(Minecraft client, int width, int height, int top, int bottom, int col1,
                int col2,
                int col3) {
            super(client, width, height, top, bottom);

        }

        public void addMapping(ItemStack stack, String itemName, String newName, String packName) {
            this.addEntry(new TextEntry(stack, itemName, newName, packName));
        }

        @Override
        public int getRowWidth() {
            return this.width - 12; // leave space for scrollbar
        }

        public static class TextEntry extends ObjectSelectionList.Entry<TextEntry> {

            private final ItemStack stack;
            private final String itemName;
            private final String newName;
            private final String packName;

            public TextEntry(ItemStack stack, String itemName, String newName, String packName) {
                this.stack = stack;
                this.itemName = itemName;
                this.newName = newName;
                this.packName = packName;
            }

            @Override
            public void renderContent(
                    GuiGraphics context,
                    int mouseX,
                    int mouseY,
                    boolean hovered,
                    float deltaTicks) {
                Minecraft mc = Minecraft.getInstance();
                int color = hovered ? 0xFFFFFFA0 : 0xFFFFFFFF;

                int entryHeight = getContentHeight();
                int textY = getY() + (entryHeight - mc.font.lineHeight) / 2;

                // --- ICON ---
                int iconX = column1X;
                int iconY = getY() + (entryHeight - 16) / 2;
                context.renderItem(stack, iconX, iconY);

                // --- COLUMN 1: original item name ---
                context.drawString(mc.font, itemName, column1X + 20, textY, color, false);

                // --- COLUMN 2: new name ---
                context.drawString(mc.font, newName, column2X, textY, color, false);

                // --- COLUMN 3: pack name ---
                context.drawString(mc.font, packName, column3X, textY, color, false);
            }

            @Override
            public Component getNarration() {
                return Component.literal(itemName + ", " + newName + ", " + packName);
            }
        }

    }
}
