package moze_intel.projecte.emc.mappers.recipe;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSFake;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.common.ModConfigSpec;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.AnnotationHelper;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

@EMCMapper
public class CraftingMapper implements IEMCMapper<NormalizedSimpleStack, Long> {
  private final Map<String, BooleanSupplier> enabledRecipeMappers = new HashMap<>();
  private final List<IRecipeTypeMapper> recipeMappers = AnnotationHelper.getRecipeTypeMappers();

  private boolean isRecipeMapperEnabled(IRecipeTypeMapper mapper) {
    BooleanSupplier supplier = this.enabledRecipeMappers.get(mapper.getName());
    return supplier == null || supplier.getAsBoolean();
  }

  @Override
  public void addConfigOptions(ModConfigSpec.Builder configBuilder) {
    for (IRecipeTypeMapper recipeMapper : this.recipeMappers) {
      configBuilder
          .comment(recipeMapper.getDescription())
          .translation(recipeMapper.getTranslationKey())
          .push(recipeMapper.getConfigPath());
      ModConfigSpec.BooleanValue enabled =
          PEConfigTranslations.MAPPING_RECIPE_TYPE_MAPPER_ENABLED
              .applyToBuilder(configBuilder)
              .define("enabled", recipeMapper.isAvailable());
      this.enabledRecipeMappers.put(recipeMapper.getName(), enabled::get);
      recipeMapper.addConfigOptions(configBuilder);
      configBuilder.pop();
    }
  }

  @Override
  public void addMappings(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      ReloadableServerResources serverResources,
      RegistryAccess registryAccess,
      ResourceManager resourceManager) {
    NSSFake.setCurrentNamespace("craftingMapper");
    Reference2ObjectMap<ResourceKey<RecipeType<?>>, CraftingMapper.RecipeCountInfo> recipeCount =
        new Reference2ObjectOpenHashMap();
    Set<ResourceKey<RecipeType<?>>> canNotMap = new ReferenceOpenHashSet();
    RecipeManager recipeManager = serverResources.getRecipeManager();
    CraftingMapper.NSSFakeGroupManager fakeGroupManager =
        new CraftingMapper.NSSFakeGroupManager(mapper);

    for (Entry<ResourceKey<RecipeType<?>>, RecipeType<?>> entry :
        BuiltInRegistries.RECIPE_TYPE.entrySet()) {
      ResourceKey<RecipeType<?>> typeRegistryKey = entry.getKey();
      RecipeType<?> recipeType = entry.getValue();
      boolean wasHandled = false;
      List<RecipeHolder<?>> recipes = null;
      List<RecipeHolder<?>> unhandled = new ArrayList<>();

      for (IRecipeTypeMapper recipeMapper : this.recipeMappers) {
        if (this.isRecipeMapperEnabled(recipeMapper) && recipeMapper.canHandle(recipeType)) {
          if (recipes == null) {
            recipes =
                recipeManager.getRecipes().stream()
                    .filter(recipe -> recipe.value().getType() == recipeType)
                    .toList();
          }

          int numHandled = 0;

          for (RecipeHolder<?> recipeHolder : recipes) {
            try {
              if (recipeMapper.handleRecipe(
                  mapper, recipeHolder, registryAccess, fakeGroupManager)) {
                numHandled++;
              } else {
                unhandled.add(recipeHolder);
              }
            } catch (Exception e) {
              PECore.LOGGER.error(
                  LogUtils.FATAL_MARKER,
                  "A fatal error occurred while trying to map the recipe: {}",
                  recipeHolder.id());
              throw e;
            }
          }

          if (numHandled > 0 || recipes.isEmpty()) {
            CraftingMapper.RecipeCountInfo recipeCountInfo =
                (CraftingMapper.RecipeCountInfo) recipeCount.get(typeRegistryKey);
            if (recipeCountInfo != null) {
              recipeCountInfo.setUnhandled(unhandled);
            } else {
              recipeCount.put(
                  typeRegistryKey, new CraftingMapper.RecipeCountInfo(recipes.size(), unhandled));
            }

            wasHandled = true;
            if (unhandled.isEmpty()) {
              break;
            }

            recipes = unhandled;
            unhandled = new ArrayList<>();
          }
        }
      }

      if (!wasHandled) {
        canNotMap.add(typeRegistryKey);
      }
    }

    PECore.debugLog("{} Statistics:", this.getName());
    Iterator<
            it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<
                ResourceKey<RecipeType<?>>, CraftingMapper.RecipeCountInfo>>
        iterator = Reference2ObjectMaps.fastIterator(recipeCount);

    while (iterator.hasNext()) {
      it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<
              ResourceKey<RecipeType<?>>, CraftingMapper.RecipeCountInfo>
          entry = iterator.next();
      Identifier typeRegistryName = ((ResourceKey) entry.getKey()).identifier();
      CraftingMapper.RecipeCountInfo countInfo = (CraftingMapper.RecipeCountInfo) entry.getValue();
      int total = countInfo.getTotalRecipes();
      List<RecipeHolder<?>> unhandled = countInfo.getUnhandled();
      PECore.debugLog(
          "Found and handled {} of {} Recipes of Type {}",
          total - unhandled.size(),
          total,
          typeRegistryName);
      if (!unhandled.isEmpty()) {
        PECore.debugLog("Unhandled Recipes of Type {}:", typeRegistryName);

        for (RecipeHolder<?> recipeHolder : unhandled) {
          PECore.debugLog(
              "Name: {}, Recipe class: {}",
              recipeHolder.id(),
              recipeHolder.value().getClass().getName());
        }
      }
    }

    for (ResourceKey<RecipeType<?>> typeRegistryKey : canNotMap) {
      PECore.debugLog("Could not map any Recipes of Type: {}", typeRegistryKey.identifier());
    }

    NSSFake.resetNamespace();
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER.tooltip();
  }

  private static class NSSFakeGroupManager implements INSSFakeGroupManager {
    private static final Function<Object2IntMap<NormalizedSimpleStack>, String> MAP_DESCRIPTOR =
        map ->
            map.object2IntEntrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getIntValue())
                .collect(Collectors.joining(", "));
    private static final boolean DEBUG_GROUP_CONTENTS = false;
    private final Map<Object2IntMap<NormalizedSimpleStack>, INSSFakeGroupManager.FakeGroupData>
        ingredientGroupsWithCount = new HashMap<>();
    private final Map<Object2IntMap<NormalizedSimpleStack>, INSSFakeGroupManager.FakeGroupData>
        groupsWithCount = new HashMap<>();
    private final IMappingCollector<NormalizedSimpleStack, Long> mapper;
    private int fakeIndex;

    public NSSFakeGroupManager(IMappingCollector<NormalizedSimpleStack, Long> mapper) {
      this.mapper = mapper;
    }

    @Override
    public INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroup(
        Object2IntMap<NormalizedSimpleStack> normalizedSimpleStacks,
        boolean representsIngredient,
        boolean skipConversions) {
      return this.getOrCreateFakeGroup(
          normalizedSimpleStacks,
          representsIngredient,
          skipConversions,
          Object2IntOpenHashMap::new);
    }

    @Override
    public INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroupDirect(
        Object2IntMap<NormalizedSimpleStack> normalizedSimpleStacks,
        boolean representsIngredient,
        boolean skipConversions) {
      return this.getOrCreateFakeGroup(
          normalizedSimpleStacks, representsIngredient, skipConversions, UnaryOperator.identity());
    }

    private INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroup(
        Object2IntMap<NormalizedSimpleStack> stacks,
        boolean representsIngredient,
        boolean skipConversions,
        UnaryOperator<Object2IntMap<NormalizedSimpleStack>> copyFunction) {
      Map<Object2IntMap<NormalizedSimpleStack>, INSSFakeGroupManager.FakeGroupData> groups =
          representsIngredient ? this.ingredientGroupsWithCount : this.groupsWithCount;
      INSSFakeGroupManager.FakeGroupData data = groups.get(stacks);
      if (data != null) {
        return data;
      }

      String description = Integer.toString(this.fakeIndex++);
      NormalizedSimpleStack dummy = NSSFake.create(description);
      groups.put(copyFunction.apply(stacks), new INSSFakeGroupManager.FakeGroupData(dummy, false));
      if (!skipConversions) {
        if (representsIngredient) {
          Iterator<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<NormalizedSimpleStack>>
              iterator = Object2IntMaps.fastIterator(stacks);

          while (iterator.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<NormalizedSimpleStack> entry =
                iterator.next();
            this.mapper.addConversion(
                1,
                dummy,
                EMCHelper.intMapOf((NormalizedSimpleStack) entry.getKey(), entry.getIntValue()));
          }
        } else {
          this.mapper.addConversion(1, dummy, stacks);
        }
      }

      return new INSSFakeGroupManager.FakeGroupData(dummy, true);
    }
  }

  private static class RecipeCountInfo {
    private final int totalRecipes;
    private List<RecipeHolder<?>> unhandled;

    private RecipeCountInfo(int totalRecipes, List<RecipeHolder<?>> unhandled) {
      this.totalRecipes = totalRecipes;
      this.unhandled = unhandled;
    }

    public int getTotalRecipes() {
      return this.totalRecipes;
    }

    public void setUnhandled(List<RecipeHolder<?>> unhandled) {
      this.unhandled = unhandled;
    }

    public List<RecipeHolder<?>> getUnhandled() {
      return this.unhandled;
    }
  }
}
