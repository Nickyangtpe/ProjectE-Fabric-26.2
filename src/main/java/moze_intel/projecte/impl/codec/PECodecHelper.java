package moze_intel.projecte.impl.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.MapCodec.ResultFunction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ProjectERegistries;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.codec.MapProcessor;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.items.ItemStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PECodecHelper implements IPECodecHelper {
  private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final MethodHandle HANDLER_STACK_FIELD =
      (MethodHandle)
          Util.make(
              () -> {
                try {
                  Field field = ItemStackHandler.class.getDeclaredField("stacks");
                  field.setAccessible(true);
                  return MethodHandles.lookup().unreflectGetter(field);
                } catch (ReflectiveOperationException roe) {
                  throw new RuntimeException("Couldn't get getter MethodHandle for stacks", roe);
                }
              });
  private static final Codec<ItemStack> LENIENT_STACK_CODEC =
      ItemStack.CODEC.promotePartial(
          error -> PECore.LOGGER.error("Tried to load invalid item: '{}'", error));
  private static final Codec<ItemStack> LENIENT_OPTIONAL_STACK_CODEC =
      ExtraCodecs.optionalEmptyMap(LENIENT_STACK_CODEC.orElse(ItemStack.EMPTY))
          .xmap(
              stack -> stack.orElse(ItemStack.EMPTY),
              stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
  public static final Codec<ItemStackHandler> MUTABLE_HANDLER_CODEC =
      LENIENT_OPTIONAL_STACK_CODEC
          .listOf()
          .flatComapMap(
              list -> {
                NonNullList<ItemStack> itemList = NonNullList.createWithCapacity(list.size());
                itemList.addAll(list);
                return new ItemStackHandler(itemList);
              },
              handler -> {
                try {
                  // invokeExact requires the call-site return type to match the
                  // reflected field exactly. ItemStackHandler.stacks is a
                  // NonNullList, not merely a List; using the interface type
                  // here makes the JVM reject the handle during player saves.
                  NonNullList<ItemStack> stacks =
                      (NonNullList<ItemStack>)
                          HANDLER_STACK_FIELD.invokeExact((ItemStackHandler) handler);
                  return DataResult.success(
                      (java.util.List<ItemStack>) stacks);
                } catch (Throwable t) {
                  return DataResult.error(t::getMessage);
                }
              });
  private final Codec<Long> NON_NEGATIVE_LONG =
      this.longRangeWithMessage(
          0L, Long.MAX_VALUE, value -> "Value must be non-negative: " + value);
  private final Codec<Long> POSITIVE_LONG =
      this.longRangeWithMessage(1L, Long.MAX_VALUE, value -> "Value must be positive: " + value);
  private final Codec<BigInteger> BIG_INT =
      Codec.STRING.xmap(
          val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);
  private final Codec<BigInteger> NON_NEGATIVE_BIG_INT =
      this.bigIntRangeWithMessage(
          BigInteger.ZERO, null, value -> "Value must be non-negative: " + value);
  private final Codec<BigInteger> POSITIVE_BIG_INT =
      this.bigIntRangeWithMessage(
          BigInteger.ONE, null, value -> "Value must be non-negative: " + value);
  private final MapCodec<NormalizedSimpleStack> NSS_MAP_CODEC =
      ProjectERegistries.NSS_SERIALIZER
          .byNameCodec()
          .dispatchMap(NormalizedSimpleStack::codec, Function.identity());
  private final Codec<NormalizedSimpleStack> NSS_CODEC = this.NSS_MAP_CODEC.codec();

  @Override
  public Codec<NormalizedSimpleStack> nssCodec() {
    return this.NSS_CODEC;
  }

  @Override
  public MapCodec<NormalizedSimpleStack> nssMapCodec() {
    return this.NSS_MAP_CODEC;
  }

  @Override
  public Codec<Long> nonNegativeLong() {
    return this.NON_NEGATIVE_LONG;
  }

  @Override
  public Codec<Long> positiveLong() {
    return this.POSITIVE_LONG;
  }

  @Override
  public Codec<Long> longRangeWithMessage(long min, long max, Function<Long, String> errorMessage) {
    return Codec.LONG.validate(
        value ->
            value.compareTo(min) >= 0 && value.compareTo(max) <= 0
                ? DataResult.success(value)
                : DataResult.error(() -> errorMessage.apply(value)));
  }

  @Override
  public Codec<BigInteger> nonNegativeBigInt() {
    return this.NON_NEGATIVE_BIG_INT;
  }

  @Override
  public Codec<BigInteger> positiveBigInt() {
    return this.POSITIVE_BIG_INT;
  }

  @Override
  public Codec<BigInteger> bigIntRangeWithMessage(
      @Nullable BigInteger min,
      @Nullable BigInteger max,
      Function<BigInteger, String> errorMessage) {
    return min == null && max == null
        ? this.BIG_INT
        : this.BIG_INT.validate(
            value ->
                min != null && value.compareTo(min) < 0 || max != null && value.compareTo(max) > 0
                    ? DataResult.error(() -> errorMessage.apply(value))
                    : DataResult.success(value));
  }

  @Override
  public <K, V> Codec<Map<K, V>> lenientKeyUnboundedMap(
      MapCodec<K> keyCodec, MapCodec<V> elementCodec, MapProcessor<K, V> processor) {
    return PEUnboundedMapCodec.create(keyCodec, elementCodec, processor, true);
  }

  @Override
  public <K, V> Codec<Map<K, V>> unboundedMap(
      MapCodec<K> keyCodec, MapCodec<V> elementCodec, MapProcessor<K, V> processor) {
    return PEUnboundedMapCodec.create(keyCodec, elementCodec, processor, false);
  }

  @Override
  public <TYPE> MapCodec<TYPE> orElseWithLog(
      MapCodec<TYPE> codec, final TYPE fallback, final Supplier<String> onError) {
    return codec.mapResult(
        new ResultFunction<TYPE>() {
          public <T> DataResult<TYPE> apply(
              DynamicOps<T> ops, MapLike<T> input, DataResult<TYPE> result) {
            if (result.isError()) {
              PECore.LOGGER.error(onError.get(), ((Error) result.error().orElseThrow()).message());
              return DataResult.success(fallback);
            } else {
              return result;
            }
          }

          public <T> RecordBuilder<T> coApply(
              DynamicOps<T> ops, TYPE input, RecordBuilder<T> builder) {
            return builder;
          }

          @Override
          public String toString() {
            return "projecte:OrElseWithLog[" + onError + " " + fallback + "]";
          }
        });
  }

  public static <TYPE> boolean writeToFile(
      Provider registries, Path path, Codec<TYPE> codec, TYPE value, String fileDescription) {
    DataResult<JsonElement> result =
        codec.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), value);
    if (result.isError()) {
      PECore.LOGGER.error(
          "Failed to convert {} to json: {}",
          fileDescription,
          ((Error) result.error().orElseThrow()).message());
      return false;
    }

    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      PRETTY_GSON.toJson((JsonElement) result.getOrThrow(), writer);
      return true;
    } catch (IOException e) {
      PECore.LOGGER.error("Failed to write {} file: {}", new Object[] {fileDescription, path, e});
      return false;
    }
  }

  public static <TYPE> Optional<TYPE> readFromFile(
      Provider registries, Path path, Codec<TYPE> codec, String fileDescription) {
    if (Files.exists(path)) {
      try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        return read(registries, reader, codec, fileDescription);
      } catch (IOException e) {
        PECore.LOGGER.error("Couldn't access {} file: {}", new Object[] {fileDescription, path, e});
      }
    }

    return Optional.empty();
  }

  public static <TYPE> Optional<TYPE> read(
      Provider registryAccess, Reader reader, Codec<TYPE> codec, String description) {
    return read(
        registryAccess.createSerializationContext(JsonOps.INSTANCE), reader, codec, description);
  }

  public static <TYPE> Optional<TYPE> read(
      DynamicOps<JsonElement> ops, Reader reader, Codec<TYPE> codec, String description) {
    JsonElement json;
    try {
      json = JsonParser.parseReader(reader);
    } catch (JsonParseException e) {
      PECore.LOGGER.error("Couldn't parse {}", description, e);
      return Optional.empty();
    }

    DataResult<TYPE> result = codec.parse(ops, json);
    if (result.isError()) {
      PECore.LOGGER.error(
          "Couldn't parse {}: {}", description, ((Error) result.error().orElseThrow()).message());
      return Optional.empty();
    } else {
      return result.result();
    }
  }
}
