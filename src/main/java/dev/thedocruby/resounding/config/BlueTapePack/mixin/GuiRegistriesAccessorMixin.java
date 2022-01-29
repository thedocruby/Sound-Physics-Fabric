package dev.thedocruby.resounding.config.BlueTapePack.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(AutoConfig.class)
public interface GuiRegistriesAccessorMixin {
    @Accessor(value = "guiRegistries", remap = false)
    static Map<Class<? extends ConfigData>, GuiRegistry> getGuiRegistries() {
        throw new AssertionError();
    }
}
