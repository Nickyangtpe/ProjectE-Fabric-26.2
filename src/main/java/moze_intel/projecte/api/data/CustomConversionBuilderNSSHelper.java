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
interface CustomConversionBuilderNSSHelper
    extends CustomConversionNSSHelper<ConversionBuilder<CustomConversionBuilder>> {
  CustomConversionBuilder before(NormalizedSimpleStack var1, long var2);

  CustomConversionBuilder before(NormalizedSimpleStack var1);

  CustomConversionBuilder after(NormalizedSimpleStack var1, long var2);

  CustomConversionBuilder after(NormalizedSimpleStack var1);

  default CustomConversionBuilder before(ItemStack stack, long emc) {
    return this.before(NSSItem.createItem(stack), emc);
  }

  default CustomConversionBuilder before(ItemLike itemProvider, long emc) {
    return this.before(NSSItem.createItem(itemProvider), emc);
  }

  default CustomConversionBuilder before(TagKey<Item> tag, long emc) {
    return this.before(NSSItem.createTag(tag), emc);
  }

  default CustomConversionBuilder before(ItemStack stack) {
    return this.before(NSSItem.createItem(stack));
  }

  default CustomConversionBuilder before(ItemLike itemProvider) {
    return this.before(NSSItem.createItem(itemProvider));
  }

  default CustomConversionBuilder before(TagKey<Item> tag) {
    return this.before(NSSItem.createTag(tag));
  }

  default CustomConversionBuilder after(ItemStack stack, long emc) {
    return this.after(NSSItem.createItem(stack), emc);
  }

  default CustomConversionBuilder after(ItemLike itemProvider, long emc) {
    return this.after(NSSItem.createItem(itemProvider), emc);
  }

  default CustomConversionBuilder after(TagKey<Item> tag, long emc) {
    return this.after(NSSItem.createTag(tag), emc);
  }

  default CustomConversionBuilder after(ItemStack stack) {
    return this.after(NSSItem.createItem(stack));
  }

  default CustomConversionBuilder after(ItemLike itemProvider) {
    return this.after(NSSItem.createItem(itemProvider));
  }

  default CustomConversionBuilder after(TagKey<Item> tag) {
    return this.after(NSSItem.createTag(tag));
  }

  default CustomConversionBuilder before(FluidStack stack, long emc) {
    return this.before(NSSFluid.createFluid(stack), emc);
  }

  default CustomConversionBuilder before(Fluid fluid, long emc) {
    return this.before(NSSFluid.createFluid(fluid), emc);
  }

  default CustomConversionBuilder beforeFluid(TagKey<Fluid> tag, long emc) {
    return this.before(NSSFluid.createTag(tag), emc);
  }

  default CustomConversionBuilder before(FluidStack stack) {
    return this.before(NSSFluid.createFluid(stack));
  }

  default CustomConversionBuilder before(Fluid fluid) {
    return this.before(NSSFluid.createFluid(fluid));
  }

  default CustomConversionBuilder beforeFluid(TagKey<Fluid> tag) {
    return this.before(NSSFluid.createTag(tag));
  }

  default CustomConversionBuilder after(FluidStack stack, long emc) {
    return this.after(NSSFluid.createFluid(stack), emc);
  }

  default CustomConversionBuilder after(Fluid fluid, long emc) {
    return this.after(NSSFluid.createFluid(fluid), emc);
  }

  default CustomConversionBuilder afterFluid(TagKey<Fluid> tag, long emc) {
    return this.after(NSSFluid.createTag(tag), emc);
  }

  default CustomConversionBuilder after(FluidStack stack) {
    return this.after(NSSFluid.createFluid(stack));
  }

  default CustomConversionBuilder after(Fluid fluid) {
    return this.after(NSSFluid.createFluid(fluid));
  }

  default CustomConversionBuilder afterFluid(TagKey<Fluid> tag) {
    return this.after(NSSFluid.createTag(tag));
  }

  default CustomConversionBuilder before(String fake, long emc) {
    return this.before(NSSFake.create(fake), emc);
  }

  default CustomConversionBuilder before(String fake) {
    return this.before(NSSFake.create(fake));
  }

  default CustomConversionBuilder after(String fake, long emc) {
    return this.after(NSSFake.create(fake), emc);
  }

  default CustomConversionBuilder after(String fake) {
    return this.after(NSSFake.create(fake));
  }
}
