package moze_intel.projecte.compat.event;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.level.ItemLike;

public final class BuildCreativeModeTabContentsEvent {
  private final ResourceKey<CreativeModeTab> tabKey;
  private final FabricCreativeModeTabOutput output;

  public BuildCreativeModeTabContentsEvent(
      ResourceKey<CreativeModeTab> tabKey, FabricCreativeModeTabOutput output) {
    this.tabKey = tabKey;
    this.output = output;
  }

  public ResourceKey<CreativeModeTab> getTabKey() {
    return this.tabKey;
  }

  public void accept(ItemLike item) {
    this.output.accept(item.asItem().getDefaultInstance(), TabVisibility.PARENT_AND_SEARCH_TABS);
  }

  public void accept(ItemStack stack) {
    this.output.accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS);
  }
}
