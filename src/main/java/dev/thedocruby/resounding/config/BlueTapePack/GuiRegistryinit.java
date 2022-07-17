package dev.thedocruby.resounding.config.BlueTapePack;

import dev.thedocruby.resounding.config.ResoundingConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.config.BlueTapePack.mixin.GuiRegistriesAccessorMixin.getGuiRegistries;

@SuppressWarnings("ALL") // its the blue tape after all
public class GuiRegistryinit {

    private GuiRegistryinit() {}

    public static void register() {
        Map<Class<? extends ConfigData>, GuiRegistry> guiRegistries = getGuiRegistries();
        GuiRegistry registry = new GuiRegistry();



        registry.registerPredicateProvider((i13n, field, config, defaults, registry1) -> {
            List<Object> configValue = new ArrayList<>(((Map<Object, Object>) Utils.getUnsafely(field, config)).values());
            Class<?> fieldTypeParam = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[1];
            Object defaultElemValue = Utils.constructUnsafely(fieldTypeParam);
            String remainingI13n = i13n.substring(0, i13n.indexOf(".option") + ".option".length());
            String classI13n = String.format("%s.%s", remainingI13n, fieldTypeParam.getSimpleName());
            return Collections.singletonList(new NestedListListEntry(Text.translatable(i13n), configValue, false, (Supplier)null, (abstractConfigListEntries) -> {
            }, () -> {
                Map ll = (Map<Object, Object>) Utils.getUnsafely(field, defaults);
                return ll == null ? List.of() : new ArrayList<>(ll.values());
            }, Text.literal(""), false, true, (elem, nestedListListEntry) -> {
                if (elem == null) {
                    Object newDefaultElemValue = Utils.constructUnsafely(fieldTypeParam);
                    return new MultiElementListEntry(Text.translatable(classI13n), newDefaultElemValue, getChildren(classI13n, fieldTypeParam, newDefaultElemValue, defaultElemValue, registry1), true);
                } else {
                    return new MultiElementListEntry(Text.translatable(classI13n), elem, getChildren(classI13n, fieldTypeParam, elem, defaultElemValue, registry1), true);

                }
            }));
            }, (field) -> Map.class.isAssignableFrom(field.getType()));



        guiRegistries.put(ResoundingConfig.class, registry);
    }

    @SuppressWarnings("unchecked")
    private static List<AbstractConfigListEntry<?>> getChildren(String i13n, @NotNull Class<?> fieldType, Object iConfig, Object iDefaults, GuiRegistryAccess guiProvider) {

        return (List) Arrays.stream(fieldType.getDeclaredFields()).map((iField) -> {
            String iI13n = String.format("%s.%s", i13n, iField.getName());
            return guiProvider.getAndTransform(iI13n, iField, iConfig, iDefaults, guiProvider);
        }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
    }

}
