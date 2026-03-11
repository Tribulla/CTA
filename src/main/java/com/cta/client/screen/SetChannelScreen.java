package com.cta.client.screen;

import com.cta.network.PacketHandler;
import com.cta.network.SetChannelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SetChannelScreen extends Screen {

    private final BlockPos peripheralPos;
    private final BlockPos targetPos;
    private EditBox channelInput;

    public SetChannelScreen(BlockPos peripheralPos, BlockPos targetPos) {
        super(Component.literal("Wireless Connector"));
        this.peripheralPos = peripheralPos;
        this.targetPos = targetPos;
    }

    public static void open(BlockPos peripheralPos, BlockPos targetPos) {
        Minecraft.getInstance().setScreen(new SetChannelScreen(peripheralPos, targetPos));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        channelInput = new EditBox(this.font, centerX - 75, centerY - 10, 150, 20, Component.literal("Channel"));
        channelInput.setMaxLength(32);
        channelInput.setValue("");
        this.addRenderableWidget(channelInput);
        this.setFocused(channelInput);

        this.addRenderableWidget(Button.builder(Component.literal("Connect"), button -> {
            sendAndClose();
        }).bounds(centerX - 50, centerY + 18, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, "Wireless Connector", this.width / 2, this.height / 2 - 35, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Channel Name:", this.width / 2, this.height / 2 - 23, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (channelInput.isFocused()) {
            return channelInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) {
            sendAndClose();
            return true;
        }
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (channelInput.isFocused()) {
            return channelInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendAndClose() {
        String name = channelInput.getValue().trim();
        if (!name.isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new SetChannelPacket(peripheralPos, targetPos, name));
        }
        this.onClose();
    }
}
