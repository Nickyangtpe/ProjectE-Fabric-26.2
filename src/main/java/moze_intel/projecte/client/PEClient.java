package moze_intel.projecte.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.client.gui.FabricMenuScreenRegistry;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.components.DataComponentManager;
import moze_intel.projecte.events.TickEvents;
import moze_intel.projecte.gameObjs.entity.EntitySWRGProjectile;
import moze_intel.projecte.gameObjs.gui.AbstractCollectorScreen;
import moze_intel.projecte.gameObjs.gui.AbstractCondenserScreen;
import moze_intel.projecte.gameObjs.gui.AlchBagScreen;
import moze_intel.projecte.gameObjs.gui.AlchChestScreen;
import moze_intel.projecte.gameObjs.gui.GUIDMFurnace;
import moze_intel.projecte.gameObjs.gui.GUIEternalDensity;
import moze_intel.projecte.gameObjs.gui.GUIMercurialEye;
import moze_intel.projecte.gameObjs.gui.GUIRMFurnace;
import moze_intel.projecte.gameObjs.gui.GUIRelay;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import moze_intel.projecte.gameObjs.items.armor.GemFeet;
import moze_intel.projecte.gameObjs.items.armor.GemLegs;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.gameObjs.sound.MovingSoundSWRG;
import moze_intel.projecte.network.commands.client.DumpMissingEmc;
import moze_intel.projecte.rendering.ChestRenderer;
import moze_intel.projecte.rendering.EntitySpriteRenderer;
import moze_intel.projecte.rendering.LayerYue;
import moze_intel.projecte.rendering.PedestalRenderer;
import moze_intel.projecte.rendering.TransmutationRenderingOverlay;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.text.ItemNameSearchIndex;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.Load;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.Unload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

public final class PEClient {
  private static Object lastClientRecipeLevel;

  private PEClient() {}

  public static void initialize() {
    ClientCommandRegistrationCallback.EVENT.register(
        (ClientCommandRegistrationCallback)
            (dispatcher, context) ->
                dispatcher.register(
                    (LiteralArgumentBuilder)
                        ClientCommands.literal("projecte").then(DumpMissingEmc.register(context))));
    DataComponentManager.setClientRecipeHolderSupplier(
        () -> {
          Minecraft minecraft = Minecraft.getInstance();
          return minecraft.level != null
                  && minecraft.level.recipeAccess() instanceof RecipeManager recipeManager
              ? recipeManager.getRecipes()
              : List.of();
        });
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
        .registerReloadListener(new ItemNameSearchIndex());
    ClientKeyHelper.registerKeyBindings();
    registerScreens();
    registerRenderers();
    registerClientEvents();
    TransmutationRenderingOverlay.register();
  }

  private static void registerScreens() {
    FabricMenuScreenRegistry.register(
        PEContainerTypes.RM_FURNACE_CONTAINER.get(), GUIRMFurnace::new);
    FabricMenuScreenRegistry
        .<moze_intel.projecte.gameObjs.container.DMFurnaceContainer,
            GUIDMFurnace<moze_intel.projecte.gameObjs.container.DMFurnaceContainer>>
            register(PEContainerTypes.DM_FURNACE_CONTAINER.get(), GUIDMFurnace::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.CONDENSER_CONTAINER.get(), AbstractCondenserScreen.MK1::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.CONDENSER_MK2_CONTAINER.get(), AbstractCondenserScreen.MK2::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.ALCH_CHEST_CONTAINER.get(), AlchChestScreen::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.ALCH_BAG_CONTAINER.get(), AlchBagScreen::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.ETERNAL_DENSITY_CONTAINER.get(), GUIEternalDensity::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.TRANSMUTATION_CONTAINER.get(), GUITransmutation::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.RELAY_MK1_CONTAINER.get(), GUIRelay.GUIRelayMK1::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.RELAY_MK2_CONTAINER.get(), GUIRelay.GUIRelayMK2::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.RELAY_MK3_CONTAINER.get(), GUIRelay.GUIRelayMK3::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.COLLECTOR_MK1_CONTAINER.get(), AbstractCollectorScreen.MK1::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.COLLECTOR_MK2_CONTAINER.get(), AbstractCollectorScreen.MK2::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.COLLECTOR_MK3_CONTAINER.get(), AbstractCollectorScreen.MK3::new);
    FabricMenuScreenRegistry.register(
        PEContainerTypes.MERCURIAL_EYE_CONTAINER.get(), GUIMercurialEye::new);
  }

  private static void registerClientEvents() {
    ClientEntityEvents.ENTITY_LOAD.register(
        (Load)
            (entity, level) -> {
              LayerYue.track(entity);
              Minecraft minecraft = Minecraft.getInstance();
              if (entity instanceof EntitySWRGProjectile projectile
                  && minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft
                    .getSoundManager()
                    .play(new MovingSoundSWRG(projectile, level.getRandom()));
              }
            });
    ClientEntityEvents.ENTITY_UNLOAD.register((Unload) (entity, level) -> LayerYue.untrack(entity));
    ClientTickEvents.END_CLIENT_TICK.register(
        (EndTick)
            minecraft -> {
              if (minecraft.level == null) {
                lastClientRecipeLevel = null;
              } else if (lastClientRecipeLevel != minecraft.level) {
                lastClientRecipeLevel = minecraft.level;
                DataComponentManager.updateCachedValues(EMCMappingHandler::getStoredEmcValue);
              }

              if (minecraft.player != null) {
                TickEvents.onClientTick(minecraft.player);
                ItemStack boots = minecraft.player.getItemBySlot(EquipmentSlot.FEET);
                if (boots.getItem() instanceof GemFeet gemFeet) {
                  ClientInput input = minecraft.player.input;
                  gemFeet.applyClientMovement(
                      minecraft.player, input.keyPresses.jump(), input.getMoveVector().y);
                }

                ItemStack leggings = minecraft.player.getItemBySlot(EquipmentSlot.LEGS);
                if (leggings.getItem() instanceof GemLegs gemLegs) {
                  gemLegs.applyClientJumpAssist(minecraft.player);
                }
              }
            });
  }

  private static void registerRenderers() {
    BlockEntityRendererRegistry.register(
        PEBlockEntityTypes.ALCHEMICAL_CHEST.get(),
        context ->
            new ChestRenderer(
                context,
                PECore.rl("textures/block/alchemical_chest.png"),
                PEBlocks.ALCHEMICAL_CHEST));
    BlockEntityRendererRegistry.register(
        PEBlockEntityTypes.CONDENSER.get(),
        context ->
            new ChestRenderer(
                context, PECore.rl("textures/block/condenser_mk1.png"), PEBlocks.CONDENSER));
    BlockEntityRendererRegistry.register(
        PEBlockEntityTypes.CONDENSER_MK2.get(),
        context ->
            new ChestRenderer(
                context, PECore.rl("textures/block/condenser_mk2.png"), PEBlocks.CONDENSER_MK2));
    BlockEntityRendererRegistry.register(
        PEBlockEntityTypes.DARK_MATTER_PEDESTAL.get(), PedestalRenderer::new);
    LayerYue.register();
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.WATER_PROJECTILE.get(),
        context -> new EntitySpriteRenderer(context, PECore.rl("textures/entity/water_orb.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.LAVA_PROJECTILE.get(),
        context -> new EntitySpriteRenderer(context, PECore.rl("textures/entity/lava_orb.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.MOB_RANDOMIZER.get(),
        context -> new EntitySpriteRenderer(context, PECore.rl("textures/entity/randomizer.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.LENS_PROJECTILE.get(),
        context ->
            new EntitySpriteRenderer(context, PECore.rl("textures/entity/lens_explosive.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.FIRE_PROJECTILE.get(),
        context -> new EntitySpriteRenderer(context, PECore.rl("textures/entity/fireball.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.SWRG_PROJECTILE.get(),
        context -> new EntitySpriteRenderer(context, PECore.rl("textures/entity/lightning.png")));
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.NOVA_CATALYST_PRIMED.get(), TntRenderer::new);
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.NOVA_CATACLYSM_PRIMED.get(), TntRenderer::new);
    EntityRendererRegistry.register(
        (EntityType) PEEntityTypes.HOMING_ARROW.get(), TippableArrowRenderer::new);
  }
}
