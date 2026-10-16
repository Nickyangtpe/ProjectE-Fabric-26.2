package moze_intel.projecte.gameObjs.registration.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.UnaryOperator;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import moze_intel.projecte.gameObjs.registration.PEDataComponentType;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentType.Builder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

@MethodsReturnNonnullByDefault
public class DataComponentTypeDeferredRegister extends PEDeferredRegister<DataComponentType<?>> {
  private static final Codec<Long> POSITIVE_LONG =
      Codec.LONG.validate(
          val ->
              val > 0L
                  ? DataResult.success(val)
                  : DataResult.error(() -> "Value must be positive: " + val));
  private static final Codec<Double> POSITIVE_DOUBLE =
      Codec.DOUBLE.validate(
          val ->
              val > 0.0
                  ? DataResult.success(val)
                  : DataResult.error(() -> "Value must be positive: " + val));
  private final String namespace;

  public DataComponentTypeDeferredRegister(String namespace) {
    super(Registries.DATA_COMPONENT_TYPE, namespace);
    this.namespace = namespace;
  }

  public <TYPE> PEDataComponentType<TYPE> simple(
      String name, UnaryOperator<Builder<TYPE>> operator) {
    return this.create(name, operator.apply(DataComponentType.builder()).build());
  }

  public PEDataComponentType<Boolean> registerBoolean(String name) {
    return this.simple(
        name, builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
  }

  public PEDataComponentType<Byte> registerByte(String name, byte min, byte max) {
    return this.simple(
        name,
        builder ->
            builder
                .persistent(
                    Codec.BYTE.validate(
                        val ->
                            val >= min && val <= max
                                ? DataResult.success(val)
                                : DataResult.error(
                                    () ->
                                        "Value: "
                                            + val
                                            + " must be between "
                                            + min
                                            + " and "
                                            + max)))
                .networkSynchronized(ByteBufCodecs.BYTE));
  }

  public PEDataComponentType<Integer> registerNonNegativeInt(String name) {
    return this.simple(
        name,
        builder ->
            builder
                .persistent(ExtraCodecs.POSITIVE_INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));
  }

  public PEDataComponentType<Long> registerNonNegativeLong(String name) {
    return this.simple(
        name,
        builder -> builder.persistent(POSITIVE_LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
  }

  public PEDataComponentType<Double> registerNonNegativeDouble(String name) {
    return this.simple(
        name,
        builder -> builder.persistent(POSITIVE_DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
  }

  @NotNull
  private <TYPE> PEDataComponentType<TYPE> create(
      String name, @NotNull DataComponentType<TYPE> delegate) {
    PEDataComponentType<TYPE> type =
        new PEDataComponentType<>(Identifier.fromNamespaceAndPath(this.namespace, name), delegate);
    PEDeferredHolder<DataComponentType<?>, DataComponentType<TYPE>> holder =
        this.register(name, () -> type);
    type.bind(holder);
    return type;
  }
}
