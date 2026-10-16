package moze_intel.projecte.api.data;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.world_transmutation.WorldTransmutationFile;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import moze_intel.projecte.compat.conditions.ICondition;
import moze_intel.projecte.compat.conditions.WithConditions;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WorldTransmutationProvider implements DataProvider {
  private final Map<Identifier, WorldTransmutationProvider.ConditionalBuilder> worldTransmutations =
      new LinkedHashMap<>();
  private final CompletableFuture<Provider> lookupProvider;
  private final PathProvider outputProvider;
  private final String modid;
  private final Set<BlockState> seenBlockStates = new ReferenceOpenHashSet();
  private final Set<Block> seenBlocks = new ReferenceOpenHashSet();

  protected WorldTransmutationProvider(
      PackOutput output, CompletableFuture<Provider> lookupProvider, String modid) {
    this.outputProvider = output.createPathProvider(Target.DATA_PACK, "pe_world_transmutations");
    this.lookupProvider = lookupProvider;
    this.modid = modid;
  }

  public final String getName() {
    return "World Transmutations: " + this.modid;
  }

  public CompletableFuture<?> run(CachedOutput output) {
    return this.lookupProvider
        .<Provider>thenApply(
            registries -> {
              this.worldTransmutations.clear();
              this.addWorldTransmutations(registries);
              return (Provider) registries;
            })
        .thenCompose(
            registries ->
                CompletableFuture.allOf(
                    this.worldTransmutations.entrySet().stream()
                        .map(
                            entry ->
                                DataProvider.saveStable(
                                    output,
                                    registries,
                                    WorldTransmutationFile.CONDITIONAL_CODEC,
                                    entry.getValue().build(),
                                    this.outputProvider.json(entry.getKey())))
                        .toArray(CompletableFuture[]::new)));
  }

  protected abstract void addWorldTransmutations(Provider var1);

  protected WorldTransmutationBuilder createTransmutationBuilder(
      Identifier id, ICondition... conditions) {
    Objects.requireNonNull(id, "World Transmutation Builder ID cannot be null.");
    if (this.worldTransmutations.containsKey(id)) {
      throw new RuntimeException(
          "World transmutation file '" + id + "' has already been registered.");
    }

    WorldTransmutationBuilder builder =
        new WorldTransmutationBuilder(this.seenBlocks, this.seenBlockStates);
    this.worldTransmutations.put(
        id, new WorldTransmutationProvider.ConditionalBuilder(builder, conditions));
    return builder;
  }

  private record ConditionalBuilder(WorldTransmutationBuilder builder, ICondition... conditions) {
    public Optional<WithConditions<WorldTransmutationFile>> build() {
      return Optional.of(new WithConditions<>(this.builder.build(), this.conditions));
    }
  }
}
