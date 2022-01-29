package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ResoundingModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Resounding.env = EnvType.CLIENT;
        Resounding.blockSoundGroups = Arrays.stream(BlockSoundGroup.class.getDeclaredFields())
                .filter((f) -> {
                    try {
                        return Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                                && (f.get(null) instanceof BlockSoundGroup group) && !Resounding.redirectMap.containsKey(group);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .collect(Collectors.toMap(
                        (f) -> {
                            try {
                                return (BlockSoundGroup)f.get(null);
                            } catch (IllegalAccessException | ClassCastException e) {
                                e.printStackTrace();
                            }
                            return null;
                        },
                        (f) -> {
                            try {
                                return new Pair<>(f.getName(), (f.get(null) instanceof BlockSoundGroup g ? (Resounding.groupMap.containsKey(f.getName()) ?  Resounding.groupMap.get(f.getName()) : g.getBreakSound().getId().getPath().split("\\.")[1] ): "not a group"));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            return new Pair<>("", "");
                        }));
        Resounding.groupSoundBlocks = Arrays.stream(BlockSoundGroup.class.getDeclaredFields())
                .filter((f) -> {
                    try {
                        return Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                                && (f.get(null) instanceof BlockSoundGroup group) && !Resounding.redirectMap.containsKey(group);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).map((f)-> {
                    BlockSoundGroup b;
                    try { b = (BlockSoundGroup)f.get(null); }
                    catch (IllegalAccessException | ClassCastException e) { e.printStackTrace(); b = null;}
                    return new Pair<>(f.getName(),b);
                }).filter((f) -> f.getRight() != null)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        ConfigManager.registerAutoConfig();
    }
}
