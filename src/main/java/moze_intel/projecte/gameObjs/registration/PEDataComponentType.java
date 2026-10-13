package moze_intel.projecte.gameObjs.registration;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PEDataComponentType<T> implements DataComponentType<T> {
  private final Identifier id;
  private final DataComponentType<T> delegate;
  @Nullable private PEDeferredHolder<DataComponentType<?>, DataComponentType<T>> holder;

  public PEDataComponentType(@NotNull Identifier id, @NotNull DataComponentType<T> delegate) {
    this.id = id;
    this.delegate = delegate;
  }

  public void bind(@NotNull PEDeferredHolder<DataComponentType<?>, DataComponentType<T>> holder) {
    this.holder = holder;
  }

  @NotNull
  public DataComponentType<T> get() {
    if (this.holder != null) {
      this.holder.get();
    }

    return this;
  }

  @NotNull
  public DataComponentType<T> value() {
    return this.get();
  }

  public boolean isBound() {
    return this.holder != null && this.holder.isBound();
  }

  @NotNull
  public Identifier getId() {
    return this.id;
  }

  @Nullable
  public ResourceKey<DataComponentType<?>> getKey() {
    return this.holder == null ? null : this.holder.getKey();
  }

  public Codec<T> codec() {
    return this.delegate.codec();
  }

  public boolean ignoreSwapAnimation() {
    return this.delegate.ignoreSwapAnimation();
  }

  public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
    return this.delegate.streamCodec();
  }

  @Override
  public String toString() {
    return "PEDataComponentType[" + this.id + "]";
  }
}
