package moze_intel.projecte.emc.mappers.recipe.special;

import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

@RecipeTypeMapper
public class ShulkerRecoloringMapper extends DisplayBasedCraftingMapper {
  @Override
  protected boolean matchesRecipe(RecipeHolder<?> recipeHolder) {
    return recipeHolder.id().identifier().getNamespace().equals("minecraft")
        && RecipeDisplayResultMatcher.hasMatchingResult(
            recipeHolder,
            stack ->
                stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof ShulkerBoxBlock shulker
                    && shulker.getColor() != null);
  }

  @Override
  protected boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    NSSItem nssShulker = NSSItem.createItem(Items.SHULKER_BOX);

    for (DyeColor color : Constants.COLORS) {
      TagKey<Item> dyeTag =
          TagKey.create(
              Registries.ITEM,
              Identifier.fromNamespaceAndPath("c", "dyes/" + color.getSerializedName()));
      mapper.addConversion(
          1,
          NSSItem.createItem((ItemLike) Blocks.DYED_SHULKER_BOX.pick(color)),
          EMCHelper.intMapOf(nssShulker, 1, NSSItem.createTag(dyeTag), 1));
    }

    return true;
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.tooltip();
  }
}
