package moze_intel.projecte.gameObjs.items;

import moze_intel.projecte.api.item.ITransmutationTablet;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TransmutationTablet extends ItemPE implements ITransmutationTablet {
  public TransmutationTablet(Properties props) {
    super(props);
  }

  @NotNull
  public InteractionResult use(
      @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    if (!level.isClientSide()) {
      this.openContainer(player, hand, player.getInventory().getSelectedSlot());
    }

    return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
  }

  @Override
  public void openContainer(Player player, InteractionHand hand, int selected) {
    ContainerTypeDeferredRegister.openMenu(
        player,
        new TransmutationTablet.ContainerProvider(hand),
        buf -> {
          buf.writeBoolean(true);
          buf.writeEnum(hand);
          buf.writeByte(player.getInventory().getSelectedSlot());
        });
  }

  @Override
  public void openContainer(Player player) {
    ContainerTypeDeferredRegister.openMenu(
        player, new TransmutationTablet.ContainerProvider(null), buf -> buf.writeBoolean(false));
  }

  private record ContainerProvider(@Nullable InteractionHand hand) implements MenuProvider {
    public AbstractContainerMenu createMenu(
        int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
      return this.hand == null
          ? new TransmutationContainer(windowId, playerInventory)
          : new TransmutationContainer(
              windowId, playerInventory, this.hand, playerInventory.getSelectedSlot());
    }

    @NotNull
    public Component getDisplayName() {
      return PELang.TRANSMUTATION_TRANSMUTE.translate();
    }
  }
}
