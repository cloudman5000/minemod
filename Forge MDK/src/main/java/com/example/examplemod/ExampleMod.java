package com.example.examplemod;

import com.example.examplemod.block.CorruptingBlock;
import com.example.examplemod.client.GlitchRenderer;
import com.example.examplemod.client.screen.HackingTerminalScreen;
import com.example.examplemod.entity.GlitchEntity;
import com.example.examplemod.terminal.block.HackingTerminalBlock;
import com.example.examplemod.terminal.menu.HackingTerminalMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Blocks
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // === HORROR MOD CONTENT ===

    // The Corrupting Block - spreads like a virus, consumes the world
    public static final RegistryObject<Block> CORRUPTING_BLOCK = BLOCKS.register("corrupting_block",
            CorruptingBlock::new);
    public static final RegistryObject<Item> CORRUPTING_BLOCK_ITEM = ITEMS.register("corrupting_block",
            () -> new BlockItem(CORRUPTING_BLOCK.get(), new Item.Properties()));

    // The Glitch - an entity that shouldn't exist. Teleports. Watches. Hunts.
    public static final RegistryObject<EntityType<GlitchEntity>> GLITCH = ENTITY_TYPES.register("glitch",
            () -> EntityType.Builder.<GlitchEntity>of(GlitchEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("glitch"));

    public static final RegistryObject<Item> GLITCH_SPAWN_EGG = ITEMS.register("glitch_spawn_egg",
            () -> new ForgeSpawnEggItem(GLITCH, 0x0a0a0f, 0x4aff15, new Item.Properties()));

    // Hacking terminal block for restoring reality.
    public static final RegistryObject<Block> HACKING_TERMINAL_BLOCK = BLOCKS.register("hacking_terminal",
            () -> new HackingTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f, 10.0f)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Item> HACKING_TERMINAL_ITEM = ITEMS.register("hacking_terminal",
            () -> new BlockItem(HACKING_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<MenuType<HackingTerminalMenu>> HACKING_TERMINAL_MENU =
            MENU_TYPES.register("hacking_terminal",
                    () -> IForgeMenuType.create(HackingTerminalMenu::new));

    // Horror creative tab
    public static final RegistryObject<CreativeModeTab> HORROR_TAB = CREATIVE_MODE_TABS.register("horror_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> CORRUPTING_BLOCK_ITEM.get().getDefaultInstance())
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.examplemod.horror_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(CORRUPTING_BLOCK_ITEM.get());
                        output.accept(HACKING_TERMINAL_ITEM.get());
                        output.accept(GLITCH_SPAWN_EGG.get());
                    })
                    .build());

    public ExampleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributes);
        modEventBus.addListener(this::onSpawnPlacementRegister);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("THE GLITCH AWAITS...");
    }

    private void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        event.register(
                GLITCH.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(GLITCH.get(), GlitchEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CORRUPTING_BLOCK_ITEM);
            event.accept(HACKING_TERMINAL_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Corruption spreads. The Glitch watches.");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                EntityRenderers.register(GLITCH.get(), GlitchRenderer::new);
                MenuScreens.register(HACKING_TERMINAL_MENU.get(), HackingTerminalScreen::new);
            });
            LOGGER.info("You are being watched.");
        }
    }
}
