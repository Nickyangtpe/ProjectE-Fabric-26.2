package moze_intel.projecte.events;

import java.util.EnumSet;
import java.util.Set;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import moze_intel.projecte.gameObjs.items.IFireProtector;
import moze_intel.projecte.handlers.InternalAbilities;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public final class TickEvents {
  private TickEvents() {}

  public static void playerTick(Player player) {
    IAlchBagProvider provider = PECapabilities.ALCH_BAG_CAPABILITY.find(player, null);
    if (provider != null) {
      Set<DyeColor> colorsChanged = EnumSet.noneOf(DyeColor.class);

      for (DyeColor color : getBagColorsPresent(player)) {
        IItemHandler inv = provider.getBag(color);
        int i = 0;

        for (int slots = inv.getSlots(); i < slots; i++) {
          ItemStack current = inv.getStackInSlot(i);
          IAlchBagItem alchBagItem = PECapabilities.ALCH_BAG_ITEM_CAPABILITY.find(current, null);
          if (alchBagItem != null && alchBagItem.updateInAlchBag(inv, player, current)) {
            colorsChanged.add(color);
          }
        }
      }

      if (player instanceof ServerPlayer serverPlayer) {
        if (serverPlayer.containerMenu instanceof AlchBagContainer container
            && serverPlayer.getItemInHand(container.hand).getItem() instanceof AlchemicalBag bag) {
          colorsChanged.remove(bag.color);
        }

        provider.sync(serverPlayer, colorsChanged);
      }
    }

    InternalAbilities.tick(player);
    if (!player.level().isClientSide() && player.isOnFire() && shouldPlayerResistFire(player)) {
      player.clearFire();
    }
  }

  public static void onServerTick(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      playerTick(player);
    }
  }

  public static void onClientTick(Player player) {
    if (player != null) {
      playerTick(player);
    }
  }

  public static boolean shouldPlayerResistFire(Player player) {
    for (EquipmentSlot slot :
        new EquipmentSlot[] {
          EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
        }) {
      ItemStack stack = player.getItemBySlot(slot);
      if (!stack.isEmpty()
          && stack.getItem() instanceof IFireProtector protector
          && protector.canProtectAgainstFire(stack, player)) {
        return true;
      }
    }

    return PlayerHelper.checkHotbarCurios(
        player,
        (p, stackx) ->
            stackx.getItem() instanceof IFireProtector protectorx
                && protectorx.canProtectAgainstFire(stackx, p));
  }

  private static Set<DyeColor> getBagColorsPresent(Player player) {
    Set<DyeColor> bagsPresent = EnumSet.noneOf(DyeColor.class);
    IItemHandler inv = Capabilities.ItemHandler.ENTITY.find(player, null);
    if (inv != null) {
      int i = 0;

      for (int slots = inv.getSlots(); i < slots; i++) {
        ItemStack stack = inv.getStackInSlot(i);
        if (!stack.isEmpty() && stack.getItem() instanceof AlchemicalBag bag) {
          bagsPresent.add(bag.color);
        }
      }
    }

    return bagsPresent;
  }
}
