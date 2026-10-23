package moze_intel.projecte.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.ChannelFutureListener;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.server.ReloadableServerResourcesAccess;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.CustomEMCParser.CustomEMCFile;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.SyncFuelMapperPKT;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

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
        helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(items[i]), 10001L + i,
            "Command applies immediately for " + id);
      }
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
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DIAMOND), 4123L, "Command overrides datapack value");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.COAL), 0L, "Explicit removal overrides datapack value");
      execute(helper, source, "projecte resetemc minecraft:diamond");
      execute(helper, source, "projecte resetemc minecraft:coal");
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
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.IRON_ORE), 4319L,
          "Tag assignment overrides the automatic ore blacklist");
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.DEEPSLATE_IRON_ORE), 4319L,
          "Tag assignment reaches every ore variant");
      execute(helper, source, "projecte resetemc #c:ores/iron");
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
      helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.NETHER_STAR), 12397L,
          "Command refreshes immediately even with pregeneration enabled");
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

  @GameTest
  @SuppressWarnings("removal")
  public void commandsImmediatelySynchronizeEveryConnectedPlayer(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    ServerPlayer first = helper.makeMockServerPlayerInLevel();
    ServerPlayer second = helper.makeMockServerPlayerInLevel();
    ServerGamePacketListenerImpl firstConnection = first.connection;
    ServerGamePacketListenerImpl secondConnection = second.connection;
    var firstPackets = new ArrayList<IPEPacket>();
    var secondPackets = new ArrayList<IPEPacket>();
    first.connection = recordingConnection(helper, first, firstPackets);
    second.connection = recordingConnection(helper, second, secondPackets);
    var server = helper.getLevel().getServer();
    ResourceManager resources = server.getResourceManager();
    var managers = ReloadableServerResourcesAccess.getManagers(server);
    try {
      helper.assertTrue(server.getPlayerList().getPlayers().containsAll(List.of(first, second)),
          "Both clients are connected to the server");
      var knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(first, null);
      helper.assertTrue(knowledge != null, "Player EMC storage is available");
      knowledge.setEmc(BigInteger.valueOf(12345));
      String[] commands = {"projecte setemc 6777 minecraft:coal",
          "projecte removeemc minecraft:coal", "projecte resetemc minecraft:coal"};
      long[] values = {6777L, 0L, 128L};
      for (int step = 0; step < commands.length; step++) {
        firstPackets.clear();
        secondPackets.clear();
        execute(helper, source(helper).withEntity(first), commands[step]);
        helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(Items.COAL), values[step],
            "Server prices change before the command returns");
        for (List<IPEPacket> packets : List.of(firstPackets, secondPackets)) {
          List<SyncEmcPKT> emcPackets = packets.stream().filter(SyncEmcPKT.class::isInstance)
              .map(SyncEmcPKT.class::cast).toList();
          List<SyncFuelMapperPKT> fuelPackets = packets.stream().filter(SyncFuelMapperPKT.class::isInstance)
              .map(SyncFuelMapperPKT.class::cast).toList();
          helper.assertValueEqual(emcPackets.size(), 1, "Each client receives one refreshed EMC map");
          helper.assertValueEqual(fuelPackets.size(), 1, "Each client receives the refreshed fuel ordering");
          helper.assertValueEqual(emcPackets.getFirst().data().getLong(ItemInfo.fromItem(Items.COAL)),
              values[step], "Client synchronization contains the new price");
          helper.assertValueEqual(fuelPackets.getFirst().items().contains(Items.COAL.builtInRegistryHolder()),
              values[step] > 0L, "Client fuel availability matches the current EMC map");
        }
        helper.assertTrue(server.getResourceManager() == resources,
            "Command refresh uses the active data-pack resources");
        helper.assertTrue(ReloadableServerResourcesAccess.getManagers(server) == managers,
            "EMC refresh retains the active recipe manager");
        helper.assertValueEqual(knowledge.getEmc(), BigInteger.valueOf(12345),
            "Price refresh cannot add or remove the player's stored EMC");
      }
    } finally {
      first.connection = firstConnection;
      second.connection = secondConnection;
      first.discard();
      second.discard();
      server.getPlayerList().remove(first);
      server.getPlayerList().remove(second);
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void immediateCommandRefreshInvalidatesCondenserTargetPrices(GameTestHelper helper)
      throws CommandSyntaxException {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      BlockPos position = new BlockPos(3, 3, 3);
      helper.setBlock(position, PEBlocks.CONDENSER.getBlock());
      CondenserBlockEntity condenser = helper.getBlockEntity(position, CondenserBlockEntity.class);
      var player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(Vec3.atCenterOf(helper.absolutePos(position)).add(0, 0, 2));
      CondenserContainer menu = (CondenserContainer) condenser.createMenu(42, player.getInventory(), player);
      player.containerMenu = menu;
      menu.setCarried(new ItemStack(Items.DIAMOND));
      menu.clicked(0, 0, ContainerInput.PICKUP, player);
      condenser.insertEmc(8192, EmcAction.EXECUTE);
      execute(helper, source(helper), "projecte setemc 16384 minecraft:diamond");
      CondenserBlockEntity.tickServer(helper.getLevel(), condenser.getBlockPos(), condenser.getBlockState(), condenser);
      helper.assertValueEqual(condenser.requiredEmc, 16384L,
          "Existing condenser target uses the refreshed command price");
      helper.assertValueEqual(condenser.getStoredEmc(), 8192L,
          "Price refresh preserves the machine's stored EMC");
      helper.assertTrue(condenser.getOutput().getStackInSlot(0).isEmpty(),
          "The machine cannot purchase output at the old lower price");
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  @GameTest
  public void rejectedCommandsCannotRefreshOrChangeEmc(GameTestHelper helper) {
    CustomEMCFile original = copyEntries();
    var originalValues = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    try {
      CustomEMCParser.removeFromFile(NSSItem.createItem(Items.SPYGLASS));
      CustomEMCParser.flush(helper.getLevel().registryAccess());
      int index = EMCMappingHandler.getLoadIndex();
      for (String command : List.of("projecte setemc -1 minecraft:diamond",
          "projecte resetemc minecraft:spyglass")) {
        boolean rejected = false;
        try {
          helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source(helper));
        } catch (CommandSyntaxException expected) {
          rejected = true;
        }
        helper.assertTrue(rejected, "Invalid command is rejected: " + command);
      }
      helper.assertValueEqual(EMCMappingHandler.getLoadIndex(), index,
          "Rejected commands cannot trigger an EMC remap");
      helper.assertValueEqual(EMCMappingHandler.createPacketData().data(), originalValues,
          "Rejected commands retain the previous values");
    } finally {
      restore(helper, original, originalValues);
    }
    helper.succeed();
  }

  private static ServerGamePacketListenerImpl recordingConnection(
      GameTestHelper helper, ServerPlayer player, List<IPEPacket> packets) {
    return new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
        new Connection(PacketFlow.SERVERBOUND), player,
        CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
      @Override
      public void send(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof IPEPacket payload) {
          packets.add(payload);
        }
      }

      @Override
      public void send(Packet<?> packet, ChannelFutureListener listener) {
        send(packet);
      }
    };
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
    MappingConfig mapping = mappingConfig();
    boolean logExploits = mapping.logExploits.get();
    mapping.logExploits.set(false);
    int index = EMCMappingHandler.getLoadIndex();
    try {
      int result = helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
      helper.assertValueEqual(EMCMappingHandler.getLoadIndex(), index + 1,
          "Successful EMC command performs exactly one immediate remap");
      return result;
    } finally {
      mapping.logExploits.set(logExploits);
    }
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
    FuelMapper.loadMap();
  }
}
