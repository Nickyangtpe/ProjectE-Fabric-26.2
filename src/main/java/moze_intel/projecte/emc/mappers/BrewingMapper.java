package moze_intel.projecte.emc.mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSFluid;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.mixin.PotionBrewingAccessor;
import moze_intel.projecte.mixin.PotionBrewingMixAccessor;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

@EMCMapper
public class BrewingMapper implements IEMCMapper<NormalizedSimpleStack, Long> {
  @Override
  public void addMappings(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      ReloadableServerResources serverResources,
      RegistryAccess registryAccess,
      ResourceManager resourceManager) {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) {
      PECore.LOGGER.error("Failed to get server and potion data when trying to map potions");
    } else {
      PotionBrewing potionBrewing = server.potionBrewing();
      Set<ItemInfo> allReagents = this.mapAllReagents(potionBrewing);
      Set<ItemInfo> allInputs = this.mapAllInputs(potionBrewing);
      mapper.addConversion(
          1,
          NSSItem.createItem(PotionContents.createItemStack(Items.POTION, Potions.WATER)),
          EMCHelper.intMapOf(
              NSSItem.createItem(Items.GLASS_BOTTLE), 1, NSSFluid.createTag(FluidTags.WATER), 333));
      int recipeCount = 0;

      for (ItemInfo inputInfo : allInputs) {
        ItemStack validInput = inputInfo.createStack();
        NormalizedSimpleStack nssInput = inputInfo.toNSS();

        for (ItemInfo reagentInfo : allReagents) {
          ItemStack output = potionBrewing.mix(reagentInfo.createStack(), validInput);
          if (!output.isEmpty()) {
            mapper.addConversion(
                3 * output.getCount(),
                NSSItem.createItem(output),
                EMCHelper.intMapOf(nssInput, 3, reagentInfo.toNSS(), 1));
            recipeCount++;
          }
        }
      }

      PECore.debugLog("{} Statistics:", this.getName());
      PECore.debugLog("Found {} Brewing Recipes", recipeCount);
    }
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_BREWING_MAPPER.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_BREWING_MAPPER.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_BREWING_MAPPER.tooltip();
  }

  @Nullable
  private static ItemStack[] getMatchingStacks(Ingredient ingredient) {
    try {
      return ingredient
          .items()
          .map(holder -> new ItemStack((ItemLike) holder.value()))
          .toArray(ItemStack[]::new);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private Set<ItemInfo> mapAllReagents(PotionBrewing potionBrewing) {
    PotionBrewingAccessor access = (PotionBrewingAccessor) potionBrewing;
    Set<ItemInfo> allReagents = new HashSet<>();
    this.addReagents(allReagents, access.projecte$getContainerMixes());
    this.addReagents(allReagents, access.projecte$getPotionMixes());
    return allReagents;
  }

  private void addReagents(Set<ItemInfo> allReagents, List<?> mixes) {
    for (Object mix : mixes) {
      Ingredient ingredient = ((PotionBrewingMixAccessor) mix).projecte$getIngredient();
      ItemStack[] matchingStacks = getMatchingStacks(ingredient);
      if (matchingStacks != null) {
        for (ItemStack stack : matchingStacks) {
          allReagents.add(ItemInfo.fromStack(stack));
        }
      }
    }
  }

  private Set<ItemInfo> mapAllInputs(PotionBrewing potionBrewing) {
    Set<ItemInfo> allInputs = new HashSet<>();
    Set<ItemInfo> containers = new HashSet<>();
    PotionBrewingAccessor access = (PotionBrewingAccessor) potionBrewing;

    for (Ingredient potionContainer : access.projecte$getContainers()) {
      ItemStack[] matchingStacks = getMatchingStacks(potionContainer);
      if (matchingStacks != null) {
        for (ItemStack input : matchingStacks) {
          containers.add(ItemInfo.fromStack(input));
        }
      }
    }

    for (Potion potion : BuiltInRegistries.POTION.stream().toList()) {
      Holder<Potion> potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
      PotionContents contents = new PotionContents(potionHolder);

      for (ItemInfo container : containers) {
        ItemStack stack = container.createStack();
        stack.set(DataComponents.POTION_CONTENTS, contents);
        allInputs.add(ItemInfo.fromStack(stack));
      }
    }

    return allInputs;
  }
}
