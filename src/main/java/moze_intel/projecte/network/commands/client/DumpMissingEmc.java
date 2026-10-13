package moze_intel.projecte.network.commands.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashSet;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.common.Tags;
import moze_intel.projecte.compat.fml.loading.FMLEnvironment;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.mappers.OreBlacklistMapper;
import moze_intel.projecte.emc.mappers.RawMaterialsBlacklistMapper;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.alchemy.PotionContents;

public class DumpMissingEmc {
  private static final boolean SKIP_TOP =
      Boolean.parseBoolean(System.getProperties().getProperty("projecte.skip_top"));

  public static ArgumentBuilder<FabricClientCommandSource, ?> register(
      CommandBuildContext context) {
    return ((LiteralArgumentBuilder)
            LiteralArgumentBuilder.<FabricClientCommandSource>literal("dumpmissingemc")
                .then(
                    RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument(
                            "skip_expected", BoolArgumentType.bool())
                        .executes(
                            ctx -> execute(ctx, BoolArgumentType.getBool(ctx, "skip_expected")))))
        .executes(ctx -> execute(ctx, false));
  }

  private static boolean expectedMissing(FeatureFlagSet enabledFeatures, ItemInfo info) {
    Holder<Item> holder = info.getItem();
    if (holder.is(PETags.Items.IGNORE_MISSING_EMC)) {
      return true;
    }

    Item item = holder.value();
    if (item instanceof BlockItem blockItem && blockItem.getBlock().defaultDestroyTime() == -1.0F) {
      return true;
    }
    if (item instanceof Tome && !ProjectEConfig.common.craftableTome.get()) {
      return true;
    }
    if (item instanceof BundleItem bundleItem && !bundleItem.isEnabled(enabledFeatures)) {
      return true;
    }
    if (!FMLEnvironment.production
        && SKIP_TOP
        && holder
            .unwrapKey()
            .map(key -> key.identifier().getNamespace().equals("theoneprobe"))
            .orElse(false)) {
      return true;
    }
    if (MappingConfig.isEnabled(OreBlacklistMapper.INSTANCE)
        && (holder.is(Tags.Items.ORES) || holder.value() == Items.GILDED_BLACKSTONE)) {
      return true;
    }
    if (MappingConfig.isEnabled(RawMaterialsBlacklistMapper.INSTANCE)
        && (holder.is(Tags.Items.RAW_MATERIALS)
            || holder.is(Tags.Items.STORAGE_BLOCKS_RAW_COPPER)
            || holder.is(Tags.Items.STORAGE_BLOCKS_RAW_IRON)
            || holder.is(Tags.Items.STORAGE_BLOCKS_RAW_GOLD))) {
      return true;
    }
    PotionContents potionContents = info.getOrNull(DataComponents.POTION_CONTENTS);
    return potionContents != null
        && potionContents.potion().isPresent()
        && ((Holder<net.minecraft.world.item.alchemy.Potion>) potionContents.potion().get())
            .is(PETags.Potions.IGNORE_MISSING_EMC);
  }

  private static int execute(
      CommandContext<FabricClientCommandSource> ctx, boolean skipExpectedMissing) {
    FabricClientCommandSource source = (FabricClientCommandSource) ctx.getSource();
    Minecraft minecraft = Minecraft.getInstance();
    RegistryAccess registryAccess = source.getLevel().registryAccess();
    FeatureFlagSet features =
        minecraft.getConnection() == null
            ? FeatureFlags.DEFAULT_FLAGS
            : minecraft.getConnection().enabledFeatures();
    CreativeModeTab tab =
        (CreativeModeTab)
            BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.SEARCH);
    if (tab.getSearchTabDisplayItems().isEmpty()) {
      boolean hasPermissions = (Boolean) minecraft.options.operatorItemsTab().get();
      if (!hasPermissions && minecraft.player != null) {
        hasPermissions = minecraft.player.canUseGameMasterBlocks();
      }

      try {
        tab.buildContents(new ItemDisplayParameters(features, hasPermissions, registryAccess));
      } catch (Exception var11) {
      }
    }

    Set<ItemInfo> missing = new HashSet<>();

    for (Item item : BuiltInRegistries.ITEM) {
      if (item != Items.AIR && item.isEnabled(features)) {
        ItemInfo itemInfo = ItemInfo.fromItem(item);
        if ((!skipExpectedMissing || !expectedMissing(features, itemInfo))
            && !IEMCProxy.INSTANCE.hasValue(itemInfo)) {
          missing.add(itemInfo);
        }
      }
    }

    for (ItemStack stack : tab.getSearchTabDisplayItems()) {
      if (!stack.isEmpty() && !stack.getComponentsPatch().isEmpty()) {
        ItemInfo itemInfo = ItemInfo.fromStack(stack);
        if (IEMCProxy.INSTANCE.hasValue(itemInfo)) {
          missing.remove(itemInfo.itemOnly());
        } else if (!skipExpectedMissing || !expectedMissing(features, itemInfo)) {
          missing.add(itemInfo);
        }
      }
    }

    int missingCount = missing.size();
    if (missingCount == 0) {
      source.sendFeedback(PELang.DUMP_MISSING_EMC_NONE_MISSING.translate());
    } else {
      if (missingCount == 1) {
        source.sendFeedback(PELang.DUMP_MISSING_EMC_ONE_MISSING.translate());
      } else {
        source.sendFeedback(PELang.DUMP_MISSING_EMC_MULTIPLE_MISSING.translate(missingCount));
      }

      missing.stream().map(ItemInfo::toString).sorted().forEach(PECore.LOGGER::info);
    }

    return missingCount;
  }
}
