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
interface CustomConversionNSSHelper<BUILDER extends ConversionBuilder<?>> {
  BUILDER conversion(NormalizedSimpleStack var1, int var2);

  default BUILDER conversion(NormalizedSimpleStack output) {
    return this.conversion(output, 1);
  }

  default BUILDER conversion(ItemStack output) {
    return this.conversion(NSSItem.createItem(output), output.getCount());
  }

  default BUILDER conversion(ItemLike output) {
    return this.conversion(output, 1);
  }

  default BUILDER conversion(ItemLike output, int amount) {
    return this.conversion(NSSItem.createItem(output), amount);
  }

  default BUILDER conversion(TagKey<Item> output) {
    return this.conversion(output, 1);
  }

  default BUILDER conversion(TagKey<Item> output, int amount) {
    return this.conversion(NSSItem.createTag(output), amount);
  }

  default BUILDER conversion(FluidStack output) {
    return this.conversion(NSSFluid.createFluid(output), output.getAmount());
  }

  default BUILDER conversion(Fluid output) {
    return this.conversion(output, 1);
  }

  default BUILDER conversion(Fluid output, int amount) {
    return this.conversion(NSSFluid.createFluid(output), amount);
  }

  default BUILDER conversionFluid(TagKey<Fluid> output) {
    return this.conversionFluid(output, 1);
  }

  default BUILDER conversionFluid(TagKey<Fluid> output, int amount) {
    return this.conversion(NSSFluid.createTag(output), amount);
  }

  default BUILDER conversion(String fake) {
    return this.conversion(fake, 1);
  }

  default BUILDER conversion(String fake, int amount) {
    return this.conversion(NSSFake.create(fake), amount);
  }
}
