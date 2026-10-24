package moze_intel.projecte.network.packets.to_server;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.api.item.ITransmutationTablet;
import moze_intel.projecte.compat.capabilities.ItemCapability;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.network.handling.IPayloadContext;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.items.armor.GemArmorBase;
import moze_intel.projecte.gameObjs.items.armor.GemChest;
import moze_intel.projecte.gameObjs.items.armor.GemFeet;
import moze_intel.projecte.gameObjs.items.armor.GemHelmet;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.integration.curios.TransmutationTableCurios;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record KeyPressPKT(PEKeybind key) implements IPEPacket {
  public static final Type<KeyPressPKT> TYPE = new Type(PECore.rl("key_press"));
  public static final StreamCodec<ByteBuf, KeyPressPKT> STREAM_CODEC =
      PEKeybind.STREAM_CODEC.map(KeyPressPKT::new, KeyPressPKT::key);

  @NotNull
  public Type<KeyPressPKT> type() {
    return TYPE;
  }

  @Override
  public void handle(IPayloadContext context) {
    Player player = context.player();
    if (!player.isSpectator()) {
      if (this.key == PEKeybind.HELMET_TOGGLE) {
        ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helm.isEmpty() && helm.is(PEItems.GEM_HELMET.get())) {
          GemHelmet.toggleNightVision(helm, player);
        }
      } else if (this.key == PEKeybind.BOOTS_TOGGLE) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.isEmpty() && boots.is(PEItems.GEM_BOOTS.get())) {
          GemFeet.toggleStepAssist(boots, player);
        }
      } else {
        if (this.key == PEKeybind.TRANSMUTATION_TABLET) {
          if (!(player instanceof ServerPlayer)) {
            return;
          }

          Optional<IItemHandler> curiosInv = TransmutationTableCurios.getCuriosInventory(player);
          if (curiosInv.isEmpty()) {
            return;
          }

          IItemHandler curios = curiosInv.get();

          for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (stack.getItem() instanceof ITransmutationTablet tablet) {
              tablet.openContainer(player);
              break;
            }
          }
        }

        for (InteractionHand hand : InteractionHand.values()) {
          ItemStack stack = player.getItemInHand(hand);
          switch (this.key) {
            case CHARGE:
              if (tryPerformCapability(
                  player,
                  stack,
                  hand,
                  PECapabilities.CHARGE_ITEM_CAPABILITY,
                  IItemCharge::changeCharge)) {
                return;
              }

              if (hand == InteractionHand.MAIN_HAND
                  && isSafe(stack)
                  && GemArmorBase.hasAnyPiece(player)) {
                boolean active =
                    !(Boolean) player.getAttachedOrElse(PEAttachmentTypes.GEM_ARMOR_STATE, false);
                player.setAttached(PEAttachmentTypes.GEM_ARMOR_STATE, active);
                ILangEntry langEntry = active ? PELang.GEM_ACTIVATE : PELang.GEM_DEACTIVATE;
                PlayerHelper.sendOverlayMessage(player, langEntry.translate());
                return;
              }
              break;
            case EXTRA_FUNCTION:
              if (tryPerformCapability(
                  player,
                  stack,
                  hand,
                  PECapabilities.EXTRA_FUNCTION_ITEM_CAPABILITY,
                  IExtraFunction::doExtraFunction)) {
                return;
              }

              if (hand == InteractionHand.MAIN_HAND
                  && isSafe(stack)
                  && (Boolean) player.getAttachedOrElse(PEAttachmentTypes.GEM_ARMOR_STATE, false)) {
                ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                if (!chestplate.isEmpty()
                    && chestplate.is(PEItems.GEM_CHESTPLATE.get())
                    && PlayerHelper.checkCooldown(
                        player,
                        PEItems.GEM_CHESTPLATE.get(),
                        ProjectEConfig.server.cooldown.player.gemChest)) {
                  GemChest.doExplode(player);
                  return;
                }
              }
              break;
            case FIRE_PROJECTILE:
              IProjectileShooter projectile =
                  PECapabilities.PROJECTILE_SHOOTER_ITEM_CAPABILITY.find(stack, null);
              if (projectile != null
                  && PlayerHelper.checkCooldown(
                      player, stack.getItem(), ProjectEConfig.server.cooldown.player.projectile)
                  && projectile.shootProjectile(player, stack, hand)) {
                PlayerHelper.swingItem(player, hand);
                return;
              }

              if (hand == InteractionHand.MAIN_HAND
                  && isSafe(stack)
                  && (Boolean) player.getAttachedOrElse(PEAttachmentTypes.GEM_ARMOR_STATE, false)) {
                ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                if (!helmet.isEmpty() && helmet.is(PEItems.GEM_HELMET.get())) {
                  GemHelmet.doZap(player);
                  return;
                }
              }
              break;
            case MODE:
              if (tryPerformCapability(
                  player,
                  stack,
                  hand,
                  PECapabilities.MODE_CHANGER_ITEM_CAPABILITY,
                  IModeChanger::changeMode)) {
                return;
              }
          }
        }
      }
    }
  }

  private static <CAPABILITY> boolean tryPerformCapability(
      Player player,
      ItemStack stack,
      InteractionHand hand,
      ItemCapability<CAPABILITY, Void> capability,
      KeyPressPKT.CapabilityProcessor<CAPABILITY> processor) {
    CAPABILITY impl = capability.find(stack, null);
    return impl != null && processor.process(impl, player, stack, hand);
  }

  private static boolean isSafe(ItemStack stack) {
    return ProjectEConfig.server.misc.unsafeKeyBinds.get() || stack.isEmpty();
  }

  @FunctionalInterface
  private interface CapabilityProcessor<CAPABILITY> {
    boolean process(CAPABILITY var1, Player var2, ItemStack var3, InteractionHand var4);
  }

  private static class TransmutationTabletContainerProvider implements MenuProvider {
    public AbstractContainerMenu createMenu(
        int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
      return new TransmutationContainer(windowId, playerInventory);
    }

    @NotNull
    public Component getDisplayName() {
      return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }
  }
}
