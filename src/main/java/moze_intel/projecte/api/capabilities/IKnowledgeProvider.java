package moze_intel.projecte.api.capabilities;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.math.BigInteger;
import java.util.Set;
import java.util.function.IntFunction;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.compat.items.IItemHandler;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IKnowledgeProvider {
  boolean hasFullKnowledge();

  void setFullKnowledge(boolean var1);

  void clearKnowledge();

  default boolean hasKnowledge(@NotNull ItemStack stack) {
    return !stack.isEmpty() && this.hasKnowledge(ItemInfo.fromStack(stack));
  }

  boolean hasKnowledge(@NotNull ItemInfo var1);

  default boolean addKnowledge(@NotNull ItemStack stack) {
    return !stack.isEmpty() && this.addKnowledge(ItemInfo.fromStack(stack));
  }

  boolean addKnowledge(@NotNull ItemInfo var1);

  default boolean removeKnowledge(@NotNull ItemStack stack) {
    return !stack.isEmpty() && this.removeKnowledge(ItemInfo.fromStack(stack));
  }

  boolean removeKnowledge(@NotNull ItemInfo var1);

  @NotNull
  Set<ItemInfo> getKnowledge();

  @NotNull
  IItemHandler getInputAndLocks();

  BigInteger getEmc();

  void setEmc(BigInteger var1);

  void sync(@NotNull ServerPlayer var1);

  void syncEmc(@NotNull ServerPlayer var1);

  void syncKnowledgeChange(@NotNull ServerPlayer var1, ItemInfo var2, boolean var3);

  void syncInputAndLocks(
      @NotNull ServerPlayer var1, IntList var2, IKnowledgeProvider.TargetUpdateType var3);

  void receiveInputsAndLocks(Int2ObjectMap<ItemStack> var1);

  enum TargetUpdateType {
    NONE,
    IF_NEEDED,
    ALL;

    public static final IntFunction<IKnowledgeProvider.TargetUpdateType> BY_ID =
        ByIdMap.continuous(Enum::ordinal, values(), OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, IKnowledgeProvider.TargetUpdateType> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
  }
}
