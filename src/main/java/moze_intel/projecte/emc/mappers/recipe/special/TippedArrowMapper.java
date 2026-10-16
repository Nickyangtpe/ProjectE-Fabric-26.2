package moze_intel.projecte.emc.mappers.recipe.special;

import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeHolder;

@RecipeTypeMapper
public class TippedArrowMapper extends DisplayBasedCraftingMapper {
  private static final Identifier TIPPED_ARROW = BuiltInRegistries.ITEM.getKey(Items.TIPPED_ARROW);
  private static final Identifier LINGERING_POTION =
      BuiltInRegistries.ITEM.getKey(Items.LINGERING_POTION);

  @Override
  protected boolean matchesRecipe(RecipeHolder<?> recipeHolder) {
    return recipeHolder.id().identifier().getNamespace().equals("minecraft")
        && RecipeDisplayResultMatcher.hasMatchingResult(
            recipeHolder, stack -> stack.is(Items.TIPPED_ARROW));
  }

  @Override
  protected boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    NSSItem nssArrow = NSSItem.createItem(Items.ARROW);
    List<Potion> potions = BuiltInRegistries.POTION.stream().toList();

    for (Potion potion : potions) {
      Holder<Potion> potionType = BuiltInRegistries.POTION.wrapAsHolder(potion);
      PotionContents potionContents = new PotionContents(potionType);
      mapper.addConversion(
          8,
          this.createItem(TIPPED_ARROW, potionContents),
          EMCHelper.intMapOf(nssArrow, 8, this.createItem(LINGERING_POTION, potionContents), 1));
    }

    PECore.debugLog("{} Statistics:", this.getName());
    PECore.debugLog("Found {} Tipped Arrow Recipes", potions.size());
    return true;
  }

  private NSSItem createItem(Identifier item, PotionContents contents) {
    return NSSItem.createItem(
        item, DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, contents).build());
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_TIPPED_ARROW.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_TIPPED_ARROW.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_TIPPED_ARROW.tooltip();
  }
}
