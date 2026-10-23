package moze_intel.projecte.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.server.ReloadableServerResourcesAccess;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.CustomEMCParser.CustomEMCFile;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;

public class EmcCommandGameTests {
  @GameTest
  public void commandsOverrideDatapackOreAndRawMaterialDefaults(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      CommandSourceStack source = source(helper);
      Item[] items = {Items.DIAMOND, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE,
          Items.IRON_ORE, Items.RAW_IRON, Items.RAW_GOLD, Items.COAL, Items.BEDROCK,
          Items.IRON_SWORD, Items.WRITTEN_BOOK, Items.FIREWORK_ROCKET, PEItems.DARK_MATTER_PICKAXE.get()};
      for (int i = 0; i < items.length; i++) {
        String id = items[i].builtInRegistryHolder().getRegisteredName();
        String command = i % 2 == 0 ? "projecte setemc " + (10001 + i) + " " + id
            : "projecte setemc " + id + " " + (10001 + i);
        helper.assertValueEqual(execute(helper, source, command), 1, "Set command succeeds for " + id);
      }
      remap(helper, helper.getLevel().getServer().getResourceManager());
      for (int i = 0; i < items.length; i++) {
        helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(items[i]), 10001L + i,
            "Explicit command value takes precedence for " + items[i].builtInRegistryHolder().getRegisteredName());
      }
      CustomEMCParser.init(helper.getLevel().registryAccess());
      for (int i = 0; i < items.length; i++) {
        helper.assertValueEqual(CustomEMCParser.currentEntries.entries().getLong(NSSItem.createItem(items[i])),
            10001L + i, "Command values persist through config serialization");
      }
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void commandRemovalAndResetRestoreServerDatapackDefaults(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      CommandSourceStack source = source(helper);
      execute(helper, source, "projecte setemc 4123 minecraft:diamond");
      execute(helper, source, "projecte removeemc minecraft:coal");
      remap(helper, helper.getLevel().getServer().getResourceManager());
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DIAMOND), 4123L, "Command overrides datapack value");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.COAL), 0L, "Explicit removal overrides datapack value");
      execute(helper, source, "projecte resetemc minecraft:diamond");
      execute(helper, source, "projecte resetemc minecraft:coal");
      remap(helper, helper.getLevel().getServer().getResourceManager());
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DIAMOND), 8192L, "Reset restores original datapack value");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.COAL), 128L, "Reset removes a custom zero override");
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void heldOffhandAndExactComponentCommandsKeepItemCompatibility(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
      CommandSourceStack source = source(helper).withEntity(player);
      ItemStack named = new ItemStack(Items.DIAMOND, 37);
      named.set(DataComponents.CUSTOM_NAME, Component.literal("Exact custom EMC"));
      player.setItemInHand(InteractionHand.MAIN_HAND, named);
      execute(helper, source, "projecte setemc 2147");
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.RAW_IRON, 17));
      execute(helper, source, "projecte setemc 3319");
      execute(helper, source, "projecte setemc 773 minecraft:diamond[custom_model_data={floats:[7.0]}]");
      ItemStack modelVariant = new ItemStack(Items.DIAMOND);
      modelVariant.set(DataComponents.CUSTOM_MODEL_DATA,
          new CustomModelData(List.of(7.0F), List.of(), List.of(), List.of()));
      remap(helper, helper.getLevel().getServer().getResourceManager());
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(named), 2147L, "Held custom components keep their exact EMC");
      helper.assertValueEqual(named.getCount(), 37, "EMC set does not consume the held stack");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.RAW_IRON), 3319L, "Offhand raw material can be assigned EMC");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(named)), ItemInfo.fromStack(named), "Explicit component mapping retains the original compatible stack");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(modelVariant), 773L,
          "Component-bearing command argument assigns the exact variant");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(modelVariant)),
          ItemInfo.fromStack(modelVariant), "Explicit model variant remains compatible");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DIAMOND), 8192L, "Exact variant setting does not change other stacks");
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void tagCommandsCanOverrideAndRestoreOreBlacklist(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      CommandSourceStack source = source(helper);
      execute(helper, source, "projecte setemc 4319 #c:ores/iron");
      remap(helper, helper.getLevel().getServer().getResourceManager());
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.IRON_ORE), 4319L,
          "Tag assignment overrides the automatic ore blacklist");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DEEPSLATE_IRON_ORE), 4319L,
          "Tag assignment reaches every ore variant");
      execute(helper, source, "projecte resetemc #c:ores/iron");
      remap(helper, helper.getLevel().getServer().getResourceManager());
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.IRON_ORE), 0L,
          "Tag reset restores the original ore blacklist");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DEEPSLATE_IRON_ORE), 0L,
          "Tag reset does not leave variant overrides behind");
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void commandValueCannotBeIgnoredByPregeneratedCache(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    MappingConfig mapping = mappingConfig();
    boolean usePregenerated = mapping.usePregenerated.get();
    try {
      mapping.usePregenerated.set(true);
      PregeneratedEMC.write(helper.getLevel().registryAccess(),
          ProjectEConfig.CONFIG_DIR.resolve("pregenerated_emc.json"), originalValues);
      execute(helper, source(helper), "projecte setemc 12397 minecraft:nether_star");
      ResourceManager delegate = helper.getLevel().getServer().getResourceManager();
      // Exercise the cache path used on servers that have no lightweight EMC datapacks.
      ResourceManager withoutEmcPack = (ResourceManager) Proxy.newProxyInstance(
          ResourceManager.class.getClassLoader(), new Class<?>[] {ResourceManager.class},
          (proxy, method, args) -> {
            if ((method.getName().equals("listResources") || method.getName().equals("listResourceStacks"))
                && args != null && args.length > 0 && "emc".equals(args[0])) {
              return Map.of();
            }
            return method.invoke(delegate, args);
          });
      remap(helper, withoutEmcPack);
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.NETHER_STAR), 12397L,
          "Custom command is not lost behind a stale pregenerated map");
    } finally {
      mapping.usePregenerated.set(usePregenerated);
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  private static CustomEMCFile copyEntries() {
    return new CustomEMCFile(new Object2LongLinkedOpenHashMap<>(CustomEMCParser.currentEntries.entries()),
        CustomEMCParser.currentEntries.comment());
  }

  private static CommandSourceStack source(GameTestHelper helper) {
    return helper.getLevel().getServer().createCommandSourceStack()
        .withLevel(helper.getLevel()).withSource(CommandSource.NULL).withSuppressedOutput();
  }

  private static int execute(GameTestHelper helper, CommandSourceStack source, String command)
      throws CommandSyntaxException {
    return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
  }

  private static void remap(GameTestHelper helper, ResourceManager resources) {
    var server = helper.getLevel().getServer();
    CustomEMCParser.init(server.registryAccess());
    MappingConfig mapping = mappingConfig();
    boolean logExploits = mapping.logExploits.get();
    // These intentionally overriding test values do not need exploit-diagnostic spam.
    mapping.logExploits.set(false);
    try {
      EMCMappingHandler.map(ReloadableServerResourcesAccess.getManagers(server), server.registryAccess(), resources);
    } finally {
      mapping.logExploits.set(logExploits);
    }
  }

  private static MappingConfig mappingConfig() {
    return (MappingConfig) ProjectEConfig.getConfigs().stream()
        .filter(MappingConfig.class::isInstance).findFirst().orElseThrow();
  }

  private static void restore(GameTestHelper helper, CustomEMCFile entries,
      Object2LongOpenHashMap<ItemInfo> values) {
    // Mark the file dirty before restoring the original entries, so the test leaves no overrides.
    CustomEMCParser.addToFile(NSSItem.createItem(Items.NETHER_STAR), 1);
    CustomEMCParser.currentEntries = entries;
    CustomEMCParser.flush(helper.getLevel().registryAccess());
    EMCMappingHandler.updateEmcValues(values);
  }
}
