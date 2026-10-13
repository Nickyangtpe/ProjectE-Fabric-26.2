package moze_intel.projecte.emc.mappers.recipe.special;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import org.jetbrains.annotations.Nullable;

@RecipeTypeMapper
public class SuspiciousStewMapper extends DisplayBasedCraftingMapper {
  private static final Identifier SUSPICIOUS_STEW =
      BuiltInRegistries.ITEM.getKey(Items.SUSPICIOUS_STEW);
  private static final TagKey<Item> SMALL_FLOWERS =
      TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "small_flowers"));

  @Override
  protected boolean matchesRecipe(RecipeHolder<?> recipeHolder) {
    return recipeHolder.id().identifier().getNamespace().equals("minecraft")
        && RecipeDisplayResultMatcher.hasMatchingResult(
            recipeHolder, stack -> stack.is(Items.SUSPICIOUS_STEW));
  }

  @Override
  protected boolean handleRecipe(
      IMappingCollector<NormalizedSimpleStack, Long> mapper,
      RegistryAccess registryAccess,
      INSSFakeGroupManager fakeGroupManager) {
    Iterable<Holder<Item>> flowersTag = BuiltInRegistries.ITEM.getTagOrEmpty(SMALL_FLOWERS);
    NSSItem nssBowl = NSSItem.createItem(Items.BOWL);
    NSSItem nssRedMushroom = NSSItem.createItem(Items.RED_MUSHROOM);
    NSSItem nssBrownMushroom = NSSItem.createItem(Items.BROWN_MUSHROOM);
    Map<SuspiciousStewEffects, Object2IntMap<NormalizedSimpleStack>> knownEffects = new HashMap<>();

    for (Holder<Item> flowerHolder : flowersTag) {
      Item flower = (Item) flowerHolder.value();
      SuspiciousEffectHolder effectHolder = SuspiciousEffectHolder.tryGet(flower);
      knownEffects
          .computeIfAbsent(
              effectHolder == null ? null : effectHolder.getSuspiciousEffects(),
              ignored -> new Object2IntOpenHashMap())
          .put(NSSItem.createItem(flower), 1);
    }

    for (Entry<SuspiciousStewEffects, Object2IntMap<NormalizedSimpleStack>> entry :
        knownEffects.entrySet()) {
      Object2IntMap<NormalizedSimpleStack> flowers = entry.getValue();
      NSSItem nssStew = this.createStew(entry.getKey());
      NormalizedSimpleStack nssFlower;
      if (flowers.size() == 1) {
        nssFlower = (NormalizedSimpleStack) flowers.keySet().iterator().next();
      } else {
        nssFlower = fakeGroupManager.getOrCreateFakeGroupDirect(flowers, true).dummy();
      }

      mapper.addConversion(
          1,
          nssStew,
          EMCHelper.intMapOf(nssBowl, 1, nssRedMushroom, 1, nssBrownMushroom, 1, nssFlower, 1));
    }

    PECore.debugLog("{} Statistics:", this.getName());
    PECore.debugLog("Found {} Suspicious Stew Recipes", knownEffects.size());
    return true;
  }

  private NSSItem createStew(@Nullable SuspiciousStewEffects effects) {
    return effects == null
        ? NSSItem.createItem(SUSPICIOUS_STEW)
        : NSSItem.createItem(
            SUSPICIOUS_STEW,
            DataComponentPatch.builder()
                .set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects)
                .build());
  }

  @Override
  public String getName() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.title();
  }

  @Override
  public String getTranslationKey() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.getTranslationKey();
  }

  @Override
  public String getDescription() {
    return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.tooltip();
  }
}
