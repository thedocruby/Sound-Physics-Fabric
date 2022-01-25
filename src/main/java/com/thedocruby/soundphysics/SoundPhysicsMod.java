package com.thedocruby.soundphysics;

import com.thedocruby.soundphysics.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;


import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/*
    Notes for developers:
    !!! Import settings.zip to your IDEA !!! - important notices are marked like that
    ψ This is for anchors that you'd like to jump to from the scroll bar ψ
    //rm is for temporary test code like "System.out.println(var) //rm" (actually */  //rm)
//*/

public class SoundPhysicsMod implements ModInitializer {
    public static Map<BlockSoundGroup, Pair<String, String>> blockSoundGroups;
    public static Map<String, BlockSoundGroup> groupSoundBlocks;
    @Override
    public void onInitialize()
    {
        blockSoundGroups = Arrays.stream(BlockSoundGroup.class.getDeclaredFields())
                .filter((f) -> {
                    try {
                        return Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                                && (f.get(null) instanceof BlockSoundGroup group) && !redirectMap.containsKey(group);
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
                                 return new Pair<>(f.getName(), (f.get(null) instanceof BlockSoundGroup g ? (groupMap.containsKey(f.getName()) ?  groupMap.get(f.getName()) : g.getBreakSound().getId().getPath().split("\\.")[1] ): "not a group"));
                             } catch (IllegalAccessException e) {
                                 e.printStackTrace();
                             }
                             return new Pair<>("", "");
                         }));
        groupSoundBlocks = Arrays.stream(BlockSoundGroup.class.getDeclaredFields())
                .filter((f) -> {
                    try {
                        return Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                                && (f.get(null) instanceof BlockSoundGroup group) && !redirectMap.containsKey(group);
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

    public static final Map<BlockSoundGroup, BlockSoundGroup> redirectMap = //<editor-fold desc="Map.ofEntries()">
            Map.ofEntries(  // first becomes second
                    entry(BlockSoundGroup.MOSS_CARPET, BlockSoundGroup.MOSS_BLOCK),
                    entry(BlockSoundGroup.AMETHYST_CLUSTER, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.SMALL_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.MEDIUM_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.LARGE_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.POINTED_DRIPSTONE, BlockSoundGroup.DRIPSTONE_BLOCK),
                    entry(BlockSoundGroup.FLOWERING_AZALEA, BlockSoundGroup.AZALEA),
                    entry(BlockSoundGroup.DEEPSLATE_BRICKS, BlockSoundGroup.POLISHED_DEEPSLATE),
                    entry(BlockSoundGroup.COPPER, BlockSoundGroup.METAL),
                    entry(BlockSoundGroup.ANVIL, BlockSoundGroup.METAL),
                    entry(BlockSoundGroup.NETHER_SPROUTS, BlockSoundGroup.ROOTS),
                    entry(BlockSoundGroup.WEEPING_VINES_LOW_PITCH, BlockSoundGroup.WEEPING_VINES),
                    entry(BlockSoundGroup.LILY_PAD, BlockSoundGroup.WET_GRASS),
                    entry(BlockSoundGroup.NETHER_GOLD_ORE, BlockSoundGroup.NETHERRACK),
                    entry(BlockSoundGroup.NETHER_ORE, BlockSoundGroup.NETHERRACK),
                    entry(BlockSoundGroup.CALCITE, BlockSoundGroup.STONE),
                    entry(BlockSoundGroup.GILDED_BLACKSTONE, BlockSoundGroup.STONE),
                    entry(BlockSoundGroup.SMALL_DRIPLEAF, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.BIG_DRIPLEAF, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.SPORE_BLOSSOM, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.GLOW_LICHEN, BlockSoundGroup.VINE),
                    entry(BlockSoundGroup.HANGING_ROOTS, BlockSoundGroup.VINE),
                    entry(BlockSoundGroup.ROOTED_DIRT, BlockSoundGroup.GRAVEL),
                    entry(BlockSoundGroup.WART_BLOCK, BlockSoundGroup.NETHER_WART),
                    entry(BlockSoundGroup.CROP, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.BAMBOO_SAPLING, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.SWEET_BERRY_BUSH, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.SCAFFOLDING, BlockSoundGroup.BAMBOO),
                    entry(BlockSoundGroup.LODESTONE, BlockSoundGroup.NETHERITE),
                    entry(BlockSoundGroup.LADDER, BlockSoundGroup.WOOD)
            );//</editor-fold>
    public static final Map<String, String> groupMap = //<editor-fold desc="Map.ofEntries()">
            Map.ofEntries(
                    entry("field_11528", "Coral"					),		// Coral              		(coral_block)
                    entry("field_11529", "Gravel, Dirt"			),    	// Gravel, Dirt       		(gravel, rooted_dirt)
                    entry("field_27197", "Amethyst"				),    	// Amethyst           		(amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
                    entry("field_11526", "Sand"					),    	// Sand               		(sand)
                    entry("field_27196", "Candle Wax"				),    	// Candle Wax         		(candle)
                    entry("field_22140", "Weeping Vines"			),    	// Weeping Vines      		(weeping_vines, weeping_vines_low_pitch)
                    entry("field_22141", "Soul Sand"				),    	// Soul Sand          		(soul_sand)
                    entry("field_22142", "Soul Soil"				),    	// Soul Soil          		(soul_soil)
                    entry("field_22143", "Basalt"					),    	// Basalt             		(basalt)
                    entry("field_22145", "Netherrack"				),    	// Netherrack         		(netherrack, nether_ore, nether_gold_ore)
                    entry("field_22146", "Nether Brick"			),    	// Nether Brick       		(nether_bricks)
                    entry("field_21214", "Honey"					),    	// Honey              		(honey_block)
                    entry("field_22149", "Bone"					),    	// Bone               		(bone_block)
                    entry("field_17581", "Nether Wart"			),    	// Nether Wart        		(nether_wart, wart_block)
                    entry("field_11535", "Grass, Crops, Foliage"	),    	// Grass, Crops, Foliage  	(grass, crop, bamboo_sapling, sweet_berry_bush)
                    entry("field_11533", "Metal"					),    	// Metal              		(metal, copper, anvil)
                    entry("field_11534", "Aquatic Foliage"		),    	// Aquatic Foliage    		(wet_grass, lily_pad)
                    entry("field_11537", "Glass, Ice"				),    	// Glass, Ice         		(glass)
                    entry("field_28116", "Sculk Sensor"			),    	// Sculk Sensor       		(sculk_sensor)
                    entry("field_22138", "Nether Foliage"			),    	// Nether Foliage     		(roots, nether_sprouts)
                    entry("field_22139", "Shroomlight"			),    	// Shroomlight        		(shroomlight)
                    entry("field_24119", "Chain"					),    	// Chain              		(chain)
                    entry("field_29033", "Deepslate"				),    	// Deepslate          		(deepslate)
                    entry("field_11547", "Wood"					),    	// Wood               		(wood, ladder)
                    entry("field_29035", "Deepslate Tiles"		),    	// Deepslate Tiles    		(deepslate_tiles)
                    entry("field_11544", "Stone, Blackstone"		),    	// Stone, Blackstone  		(stone, calcite, gilded_blackstone)
                    entry("field_11545", "Slime"					),    	// Slime              		(slime_block)
                    entry("field_29036", "Polished Deepslate"		),    	// Polished Deepslate 		(polished_deepslate, deepslate_bricks)
                    entry("field_11548", "Snow"					),    	// Snow               		(snow)
                    entry("field_28702", "Azalea Leaves"			),    	// Azalea Leaves      		(azalea_leaves)
                    entry("field_11542", "Bamboo"					),    	// Bamboo             		(bamboo, scaffolding)
                    entry("field_18852", "Mushroom Stems"			),    	// Mushroom Stems     		(stem)
                    entry("field_11543", "Wool"					),    	// Wool               		(wool)
                    entry("field_23083", "Dry Foliage"			),    	// Dry Foliage        		(vine, hanging_roots, glow_lichen)
                    entry("field_28694", "Azalea Bush"			),    	// Azalea Bush        		(azalea)
                    entry("field_28692", "Lush Cave Foliage"		),    	// Lush Cave Foliage       	(cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
                    entry("field_22150", "Netherite"				),    	// Netherite          		(netherite_block, lodestone)
                    entry("field_22151", "Ancient Debris"			),    	// Ancient Debris     		(ancient_debris)
                    entry("field_22152", "Nether Fungus Stem"		),    	// Nether Fungus Stem 		(nether_stem)
                    entry("field_27884", "Powder Snow"			),    	// Powder Snow        		(powder_snow)
                    entry("field_27202", "Tuff"					),    	// Tuff               		(tuff)
                    entry("field_28697", "Moss"					),    	// Moss               		(moss, moss_carpet)
                    entry("field_22153", "Nylium"					),    	// Nylium             		(nylium)
                    entry("field_22154", "Nether Mushroom"		),    	// Nether Mushroom      	(fungus)
                    entry("field_17734", "Lanterns"				),    	// Lanterns           		(lantern)
                    entry("field_28060", "Dripstone"				),    	// Dripstone          		(dripstone_block, pointed_dripstone)
                    entry("DEFAULT"    , "Default Material"		)     	// Default Material   		()
            );/*</editor-fold>*/

}
