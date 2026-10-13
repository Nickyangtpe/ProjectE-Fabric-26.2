package moze_intel.projecte.api.data;

import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.api.nss.NSSFake;
import moze_intel.projecte.api.nss.NSSFluid;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import moze_intel.projecte.compat.fluids.FluidStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
interface ConversionBuilderNSSHelper<PARENT> {
  ConversionBuilder<PARENT> ingredient(NormalizedSimpleStack var1, int var2);

  default ConversionBuilder<PARENT> ingredient(NormalizedSimpleStack input) {
    return this.ingredient(input, 1);
  }

  default ConversionBuilder<PARENT> ingredient(ItemStack input) {
    return this.ingredient(NSSItem.createItem(input), input.getCount());
  }

  default ConversionBuilder<PARENT> ingredient(ItemLike input) {
    return this.ingredient(input, 1);
  }

  default ConversionBuilder<PARENT> ingredient(ItemLike input, int amount) {
    return this.ingredient(NSSItem.createItem(input), amount);
  }

  default ConversionBuilder<PARENT> ingredient(TagKey<Item> input) {
    return this.ingredient(input, 1);
  }

  default ConversionBuilder<PARENT> ingredient(TagKey<Item> input, int amount) {
    return this.ingredient(NSSItem.createTag(input), amount);
  }

  default ConversionBuilder<PARENT> ingredient(FluidStack input) {
    return this.ingredient(NSSFluid.createFluid(input), input.getAmount());
  }

  default ConversionBuilder<PARENT> ingredient(Fluid input) {
    return this.ingredient(input, 1);
  }

  default ConversionBuilder<PARENT> ingredient(Fluid input, int amount) {
    return this.ingredient(NSSFluid.createFluid(input), amount);
  }

  default ConversionBuilder<PARENT> ingredientFluid(TagKey<Fluid> input) {
    return this.ingredientFluid(input, 1);
  }

  default ConversionBuilder<PARENT> ingredientFluid(TagKey<Fluid> input, int amount) {
    return this.ingredient(NSSFluid.createTag(input), amount);
  }

  default ConversionBuilder<PARENT> ingredient(String fake) {
    return this.ingredient(fake, 1);
  }

  default ConversionBuilder<PARENT> ingredient(String fake, int amount) {
    return this.ingredient(NSSFake.create(fake), amount);
  }
}
