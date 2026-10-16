package moze_intel.projecte.gameObjs.registration.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import moze_intel.projecte.compat.event.BuildCreativeModeTabContentsEvent;
import moze_intel.projecte.compat.eventbus.IEventBus;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.utils.text.ILangEntry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.ModifyOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab.Builder;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

public class CreativeTabDeferredRegister extends PEDeferredRegister<CreativeModeTab> {
  private final Consumer<BuildCreativeModeTabContentsEvent> addToExistingTabs;
  private final String modid;

  public CreativeTabDeferredRegister(
      String modid, Consumer<BuildCreativeModeTabContentsEvent> addToExistingTabs) {
    super(Registries.CREATIVE_MODE_TAB, modid);
    this.modid = modid;
    this.addToExistingTabs = addToExistingTabs;
  }

  @Override
  public void register(@NotNull IEventBus bus) {
    super.register(bus);

    for (ResourceKey<CreativeModeTab> tabKey :
        List.of(
            CreativeModeTabs.BUILDING_BLOCKS,
            CreativeModeTabs.FUNCTIONAL_BLOCKS,
            CreativeModeTabs.REDSTONE_BLOCKS,
            CreativeModeTabs.TOOLS_AND_UTILITIES,
            CreativeModeTabs.COMBAT,
            CreativeModeTabs.INGREDIENTS)) {
      CreativeModeTabEvents.modifyOutputEvent(tabKey)
          .register(
              (ModifyOutput)
                  output ->
                      this.addToExistingTabs.accept(
                          new BuildCreativeModeTabContentsEvent(tabKey, output)));
    }
  }

  public PEDeferredHolder<CreativeModeTab, CreativeModeTab> registerMain(
      ILangEntry title, ItemLike icon, UnaryOperator<Builder> operator) {
    return this.register(this.modid, title, icon, operator);
  }

  public PEDeferredHolder<CreativeModeTab, CreativeModeTab> register(
      String name, ILangEntry title, ItemLike icon, UnaryOperator<Builder> operator) {
    return this.register(
        name,
        () -> {
          Builder builder =
              FabricCreativeModeTab.builder()
                  .title(title.translate())
                  .icon(() -> icon.asItem().getDefaultInstance());
          return operator.apply(builder).build();
        });
  }
}
