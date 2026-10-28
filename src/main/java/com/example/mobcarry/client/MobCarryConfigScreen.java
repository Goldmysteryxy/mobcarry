package com.example.mobcarry.client;

import com.example.mobcarry.MobCarryConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class MobCarryConfigScreen extends Screen {
    private final Screen parent;
    private int y;

    public MobCarryConfigScreen(Screen parent) {
        super(Component.translatable("mobcarry.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MobCarryConfig c = MobCarryConfig.get();
        this.addRenderableWidget(new StringWidget(0, 15, this.width, 9, this.title, this.font).alignCenter());

        y = 40;
        toggle("enabled", c.enabled, v -> c.enabled = v);
        toggle("requireSneaking", c.requireSneaking, v -> c.requireSneaking = v);
        toggle("requireEmptyHand", c.requireEmptyHand, v -> c.requireEmptyHand = v);
        toggle("allowPassiveMobs", c.allowPassiveMobs, v -> c.allowPassiveMobs = v);
        toggle("makePersistent", c.makePersistent, v -> c.makePersistent = v);
        toggle("protectFromSun", c.protectFromSun, v -> c.protectFromSun = v);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void toggle(String key, boolean value, Consumer<Boolean> setter) {
        this.addRenderableWidget(CycleButton.onOffBuilder(value).create(
                this.width / 2 - 100, y, 200, 20,
                Component.translatable("mobcarry.config." + key),
                (button, v) -> {
                    setter.accept(v);
                    MobCarryConfig.save();
                }));
        y += 24;
    }

    @Override
    public void onClose() {
        MobCarryConfig.save();
        this.minecraft.setScreen(parent);
    }
}
