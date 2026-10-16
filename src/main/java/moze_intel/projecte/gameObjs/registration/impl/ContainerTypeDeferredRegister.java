package moze_intel.projecte.gameObjs.registration.impl;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import java.util.function.Supplier;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.gameObjs.registration.INamedEntry;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContainerTypeDeferredRegister extends PEDeferredRegister<MenuType<?>> {
  private static final StreamCodec<RegistryFriendlyByteBuf, byte[]> BYTE_ARRAY_CODEC =
      StreamCodec.of(
          (buf, data) -> buf.writeBytes(data),
          buf -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
          });

  public ContainerTypeDeferredRegister(String modid) {
    super(Registries.MENU, modid, ContainerTypeRegistryObject::new);
  }

  public <CONTAINER extends AbstractContainerMenu, BE extends BlockEntity>
      ContainerTypeRegistryObject<CONTAINER> register(
          INamedEntry nameProvider,
          Class<BE> blockEntityClass,
          ContainerTypeDeferredRegister.IBlockEntityContainerFactory<CONTAINER, BE> factory) {
    return this.registerFactory(
        nameProvider.getName(),
        (id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass)));
  }

  public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER> register(
      INamedEntry nameProvider,
      ContainerTypeDeferredRegister.IContainerFactory<CONTAINER> factory) {
    return this.registerFactory(nameProvider.getName(), factory);
  }

  public <CONTAINER extends AbstractContainerMenu>
      ContainerTypeRegistryObject<CONTAINER> registerFactory(
          String name, ContainerTypeDeferredRegister.IContainerFactory<CONTAINER> factory) {
    return this.registerMenu(
        name,
        () ->
            new ExtendedMenuType<CONTAINER, byte[]>(
                (id, inv, data) -> {
                  FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer((byte[]) data));

                  try {
                    return factory.create(id, inv, buf);
                  } finally {
                    buf.release();
                  }
                },
                BYTE_ARRAY_CODEC));
  }

  public <CONTAINER extends AbstractContainerMenu>
      ContainerTypeRegistryObject<CONTAINER> registerMenu(
          String name, Supplier<MenuType<CONTAINER>> supplier) {
    return (ContainerTypeRegistryObject<CONTAINER>)
        super.<MenuType<CONTAINER>>register(name, supplier);
  }

  public static boolean openMenu(Player player, MenuProvider provider, BlockPos pos) {
    return openMenu(player, provider, payload -> payload.writeBlockPos(pos));
  }

  public static boolean openMenu(
      final Player player, final MenuProvider provider, FriendlyByteBuf payload) {
    if (player instanceof ServerPlayer serverPlayer) {
      final byte[] data = new byte[payload.readableBytes()];
      payload.getBytes(payload.readerIndex(), data);
      serverPlayer.openMenu(
          new ExtendedMenuProvider<byte[]>() {
            public byte[] getScreenOpeningData(ServerPlayer ignored) {
              return data;
            }

            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
              return provider.createMenu(id, inventory, player);
            }

            public Component getDisplayName() {
              return provider.getDisplayName();
            }
          });
      return true;
    } else {
      return false;
    }
  }

  public static boolean openMenu(
      Player player, MenuProvider provider, Consumer<FriendlyByteBuf> payloadWriter) {
    FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.buffer());

    try {
      payloadWriter.accept(payload);
      return openMenu(player, provider, payload);
    } finally {
      payload.release();
    }
  }

  private static <BE extends BlockEntity> BE getBlockEntityFromBuf(
      FriendlyByteBuf buf, Class<BE> type) {
    if (buf == null) {
      throw new IllegalArgumentException("Null packet buffer");
    } else if (FMLEnvironment.dist.isDedicatedServer()) {
      throw new UnsupportedOperationException("This method is only supported on the client.");
    } else {
      BlockPos pos = buf.readBlockPos();
      BE blockEntity = WorldHelper.getBlockEntity(type, Minecraft.getInstance().level, pos);
      if (blockEntity == null) {
        throw new IllegalStateException(
            "Client could not locate block entity at "
                + pos
                + " for block entity container. This is likely caused by a mod breaking client side"
                + " block entity lookup");
      } else {
        return blockEntity;
      }
    }
  }

  @FunctionalInterface
  public interface IBlockEntityContainerFactory<
      CONTAINER extends AbstractContainerMenu, BE extends BlockEntity> {
    CONTAINER create(int var1, Inventory var2, BE var3);
  }

  @FunctionalInterface
  public interface IContainerFactory<CONTAINER extends AbstractContainerMenu> {
    CONTAINER create(int var1, Inventory var2, FriendlyByteBuf var3);
  }
}
