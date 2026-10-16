package moze_intel.projecte.api.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.conversion.CustomConversionFile;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CustomConversionProvider implements DataProvider {
  private final Map<Identifier, CustomConversionBuilder> customConversions = new LinkedHashMap<>();
  private final CompletableFuture<Provider> lookupProvider;
  private final PathProvider outputProvider;
  private final String modid;

  protected CustomConversionProvider(
      PackOutput output, CompletableFuture<Provider> lookupProvider, String modid) {
    this.outputProvider = output.createPathProvider(Target.DATA_PACK, "pe_custom_conversions");
    this.lookupProvider = lookupProvider;
    this.modid = modid;
  }

  public CompletableFuture<?> run(CachedOutput output) {
    return this.lookupProvider
        .<Provider>thenApply(
            registries -> {
              this.customConversions.clear();
              this.addCustomConversions(registries);
              return (Provider) registries;
            })
        .thenCompose(
            registries ->
                CompletableFuture.allOf(
                    this.customConversions.entrySet().stream()
                        .map(
                            entry ->
                                DataProvider.saveStable(
                                    output,
                                    registries,
                                    CustomConversionFile.CODEC,
                                    entry.getValue().build(),
                                    this.outputProvider.json(entry.getKey())))
                        .toArray(CompletableFuture[]::new)));
  }

  protected abstract void addCustomConversions(Provider var1);

  protected CustomConversionBuilder createConversionBuilder(Identifier id) {
    Objects.requireNonNull(id, "Custom Conversion Builder ID cannot be null.");
    if (this.customConversions.containsKey(id)) {
      throw new RuntimeException("Custom conversion '" + id + "' has already been registered.");
    }

    CustomConversionBuilder conversionBuilder = new CustomConversionBuilder();
    this.customConversions.put(id, conversionBuilder);
    return conversionBuilder;
  }

  public final String getName() {
    return "Custom EMC Conversions: " + this.modid;
  }
}
