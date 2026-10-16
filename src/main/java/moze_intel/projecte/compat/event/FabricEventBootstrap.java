package moze_intel.projecte.compat.event;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.InvocationTargetException;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.wrapper.PlayerMainInvWrapper;
import moze_intel.projecte.events.PlayerEvents;
import moze_intel.projecte.events.TickEvents;
import moze_intel.projecte.gameObjs.blocks.Pedestal;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import moze_intel.projecte.network.commands.EMCCMD;
import moze_intel.projecte.network.commands.KnowledgeCMD;
import moze_intel.projecte.network.commands.RemoveEmcCMD;
import moze_intel.projecte.network.commands.ResetEmcCMD;
import moze_intel.projecte.network.commands.SetEmcCMD;
import moze_intel.projecte.network.commands.ShowBagCMD;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.fabric.api.registry.FuelValueEvents.BuildCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class FabricEventBootstrap {
  private static boolean initialized;

  private FabricEventBootstrap() {}

  public static synchronized void initialize() {
    if (!initialized) {
      initialized = true;
      PECapabilities.KNOWLEDGE_CAPABILITY
          .lookup()
          .registerFallback(
              (entity, context) ->
                  entity instanceof Player player ? new KnowledgeImpl(player) : null);
      PECapabilities.ALCH_BAG_CAPABILITY
          .lookup()
          .registerFallback(
              (entity, context) ->
                  entity instanceof Player player ? new AlchBagImpl(player) : null);
      Capabilities.ItemHandler.ENTITY
          .lookup()
          .registerFallback(
              (entity, context) ->
                  entity instanceof Player player
                      ? new PlayerMainInvWrapper(player.getInventory())
                      : null);
      FuelValueEvents.BUILD.register(
          (BuildCallback)
              (builder, context) -> {
                int alchemicalCoal = 6400;
                int mobiusFuel = alchemicalCoal * 4;
                int aeternalisFuel = mobiusFuel * 4;
                builder.add(PEItems.ALCHEMICAL_COAL, alchemicalCoal);
                builder.add(PEBlocks.ALCHEMICAL_COAL, alchemicalCoal * 9);
                builder.add(PEItems.MOBIUS_FUEL, mobiusFuel);
                builder.add(PEBlocks.MOBIUS_FUEL, mobiusFuel * 9);
                builder.add(PEItems.AETERNALIS_FUEL, aeternalisFuel);
                builder.add(PEBlocks.AETERNALIS_FUEL, aeternalisFuel * 9);
              });
      CommandRegistrationCallback.EVENT.register(
          (dispatcher, context, selection) ->
              dispatcher.register(
                  Commands.literal("projecte")
                      .then(RemoveEmcCMD.register(context))
                      .then(ResetEmcCMD.register(context))
                      .then(SetEmcCMD.register(context))
                      .then(ShowBagCMD.register(context))
                      .then(EMCCMD.register(context))
                      .then(KnowledgeCMD.register(context))));
      UseBlockCallback.EVENT.register(
          (UseBlockCallback)
              (player, level, hand, hitResult) -> {
                ItemStack stack = player.getItemInHand(hand);
                return (InteractionResult)
                    (stack.getItem() instanceof PhilosophersStone stone
                        ? stone.onItemUseFirst(stack, new UseOnContext(player, hand, hitResult))
                        : InteractionResult.PASS);
              });
      ServerPlayerEvents.AFTER_RESPAWN.register(
          (AfterRespawn) (oldPlayer, newPlayer, alive) -> PlayerEvents.respawnEvent(newPlayer));
      ServerPlayerEvents.JOIN.register(PlayerEvents::playerConnect);
      ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
          PlayerEvents::playerChangeDimension);
      ServerLivingEntityEvents.ALLOW_DAMAGE.register(PlayerEvents::allowDamage);
      PlayerBlockBreakEvents.BEFORE.register(
          (Before)
              (level, player, pos, state, blockEntity) ->
                  !(state.getBlock() instanceof Pedestal pedestal
                      && pedestal.cancelCreativeBreak(level, pos, player, state)));
      ToolModeBreakEvents.register();
      ServerTickEvents.END_SERVER_TICK.register(TickEvents::onServerTick);
      ServerTickEvents.START_LEVEL_TICK.register(PlayerEvents::onLevelTick);
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
        invokeClientRegistration("moze_intel.projecte.events.ArchangelSmiteClientEvents");
        invokeClientRegistration("moze_intel.projecte.events.ToolTipEvent");
      }
    }
  }

  private static void invokeClientRegistration(String className) {
    try {
      Class.forName(className).getMethod("register").invoke(null);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Unable to register ProjectE Fabric client callback " + className, e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "ProjectE Fabric client callback registration failed for " + className, e.getCause());
    }
  }
}
