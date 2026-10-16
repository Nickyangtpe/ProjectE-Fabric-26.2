package moze_intel.projecte.network;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.function.IntFunction;
import moze_intel.projecte.compat.items.ItemStackHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamCodec.CodecOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class PEStreamCodecs {
  public static final StreamCodec<ByteBuf, BigInteger> EMC_VALUE =
      ByteBufCodecs.STRING_UTF8.map(
          emc -> emc.isEmpty() ? BigInteger.ZERO : new BigInteger(emc), BigInteger::toString);
  public static final StreamCodec<ByteBuf, InteractionHand> INTERACTION_HAND =
      ByteBufCodecs.BOOL.map(
          bool -> bool ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
          hand -> hand == InteractionHand.MAIN_HAND);
  public static final StreamCodec<ByteBuf, Vec3> VEC_3 =
      StreamCodec.composite(
          ByteBufCodecs.DOUBLE,
          Vec3::x,
          ByteBufCodecs.DOUBLE,
          Vec3::y,
          ByteBufCodecs.DOUBLE,
          Vec3::z,
          Vec3::new);

  private PEStreamCodecs() {}

  public static StreamCodec<RegistryFriendlyByteBuf, ItemStackHandler> handlerStreamCodec(
      final int handlerSize) {
    return new StreamCodec<RegistryFriendlyByteBuf, ItemStackHandler>() {
      public void encode(
          @NotNull RegistryFriendlyByteBuf buffer, @NotNull ItemStackHandler handler) {
        for (int slot = 0; slot < handlerSize; slot++) {
          ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, handler.getStackInSlot(slot));
        }
      }

      @NotNull
      public ItemStackHandler decode(@NotNull RegistryFriendlyByteBuf buffer) {
        ItemStackHandler locks = new ItemStackHandler(handlerSize);

        for (int slot = 0; slot < handlerSize; slot++) {
          locks.setStackInSlot(slot, (ItemStack) ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }

        return locks;
      }
    };
  }

  public static <B extends ByteBuf, V> CodecOperation<B, V, V[]> array(IntFunction<V[]> factory) {
    return codec -> array(factory, codec, Integer.MAX_VALUE);
  }

  public static <B extends ByteBuf, V> StreamCodec<B, V[]> array(
      final IntFunction<V[]> factory, final StreamCodec<? super B, V> codec, final int maxSize) {
    return new StreamCodec<B, V[]>() {
      public V @NotNull [] decode(@NotNull B buffer) {
        V[] array = (V[]) ((Object[]) factory.apply(ByteBufCodecs.readCount(buffer, maxSize)));

        for (int i = 0; i < array.length; i++) {
          array[i] = (V) codec.decode(buffer);
        }

        return array;
      }

      public void encode(@NotNull B buffer, V @NotNull [] array) {
        ByteBufCodecs.writeCount(buffer, array.length, maxSize);

        for (V v : array) {
          codec.encode(buffer, v);
        }
      }
    };
  }
}
