package moze_intel.projecte.compat.registries;

import com.mojang.serialization.Lifecycle;
import java.util.Objects;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

public final class RegistryBuilder<T> {
  private final ResourceKey<Registry<T>> registryKey;
  @Nullable private Identifier defaultKey;

  public RegistryBuilder(ResourceKey<Registry<T>> registryKey) {
    this.registryKey = Objects.requireNonNull(registryKey);
  }

  public RegistryBuilder<T> defaultKey(@Nullable Identifier defaultKey) {
    this.defaultKey = defaultKey;
    return this;
  }

  public Registry<T> create() {
    Registry<T> registry;
    if (this.defaultKey == null) {
      registry = new MappedRegistry(this.registryKey, Lifecycle.stable());
    } else {
      registry =
          new DefaultedMappedRegistry(
              this.defaultKey.toString(), this.registryKey, Lifecycle.stable(), false);
    }

    DeferredRegister.registerExternal(this.registryKey, registry);
    return registry;
  }
}
