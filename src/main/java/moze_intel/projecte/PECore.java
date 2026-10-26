package moze_intel.projecte;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import moze_intel.projecte.api.nss.AbstractNSSTag;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.event.FabricEventBootstrap;
import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.eventbus.IEventBus;
import moze_intel.projecte.compat.fml.ModContainer;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.compat.network.event.RegisterPayloadHandlersEvent;
import moze_intel.projecte.compat.registries.DeferredRegister;
import moze_intel.projecte.compat.server.ReloadableServerResourcesAccess;
import moze_intel.projecte.compat.server.ServerLifecycleHooks;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.IPEConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.gameObjs.blocks.DispenserBehaviors;
import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import moze_intel.projecte.gameObjs.registration.DoubleDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEAttributes;
import moze_intel.projecte.impl.TransmutationOffline;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.network.LargeDataSync;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.world_transmutation.WorldTransmutationManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.BeforeSave;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SyncDataPackContents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PECore {
  public static final String MODID = "projecte";
  public static final String MODNAME = "ProjectE";
  public static final GameProfile FAKEPLAYER_GAMEPROFILE =
      new GameProfile(UUID.fromString("590e39c7-9fb6-471b-a4c2-c0e539b2423d"), "[ProjectE]");
  public static final Logger LOGGER = LoggerFactory.getLogger("ProjectE");
  public static final ModContainer MOD_CONTAINER = new ModContainer("projecte");
  public static final List<String> uuids = new ArrayList<>();
  private static final String[] REGISTRY_CLASSES =
      new String[] {
        "moze_intel.projecte.gameObjs.registries.PEBlockTypes",
        "moze_intel.projecte.gameObjs.registries.PEBlocks",
        "moze_intel.projecte.gameObjs.registries.PEDataComponentTypes",
        "moze_intel.projecte.gameObjs.registries.PEItems",
        "moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes",
        "moze_intel.projecte.gameObjs.registries.PEEntityTypes",
        "moze_intel.projecte.gameObjs.registries.PEContainerTypes",
        "moze_intel.projecte.gameObjs.registries.PECreativeTabs",
        "moze_intel.projecte.gameObjs.registries.PESoundEvents",
        "moze_intel.projecte.gameObjs.registries.PERecipeSerializers",
        "moze_intel.projecte.gameObjs.registries.PERecipeConditions",
        "moze_intel.projecte.gameObjs.registries.PENormalizedSimpleStacks"
      };
  private static boolean initialized;
  private static boolean lifecycleRegistered;
  private static PacketHandler packetHandler;
  private static MinecraftServer lastMappedServer;

  private PECore() {}

  public static synchronized void init() {
    if (!initialized) {
      LOGGER.info("Initializing {}", "ProjectE");
      initialized = true;
      PEAttributes.initialize();
      FabricEventBootstrap.initialize();
      PEAttachmentTypes.initialize();
      ProjectEConfig.register(MOD_CONTAINER);
      registerDeferredRegistries(NeoForge.EVENT_BUS);
      DeferredRegister.registerAll();
      DispenserBehaviors.register();
      EMCMappingHandler.loadMappers();
      ProjectETNT.registerFlammability();
      String version =
          FabricLoader.getInstance()
              .getModContainer("projecte")
              .map(container -> container.getMetadata().getVersion().getFriendlyString())
              .orElse("0.0.0");
      packetHandler = new PacketHandler(NeoForge.EVENT_BUS, version);
      NeoForge.EVENT_BUS.post(new RegisterPayloadHandlersEvent());
      IntegrationHelper.initialize();
      NeoForge.EVENT_BUS.post(new RegisterCapabilitiesEvent());
      registerServerLifecycle();
    }
  }

  private static void registerDeferredRegistries(IEventBus modEventBus) {
    for (String className : REGISTRY_CLASSES) {
      try {
        Class<?> registryClass = Class.forName(className);
        boolean foundRegister = false;

        for (Field field : registryClass.getDeclaredFields()) {
          if (Modifier.isStatic(field.getModifiers())) {
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof DeferredRegister<?> register) {
              register.register(modEventBus);
              foundRegister = true;
            } else if (value instanceof DoubleDeferredRegister<?, ?> register) {
              register.register(modEventBus);
              foundRegister = true;
            }
          }
        }

        if (!foundRegister) {
          throw new IllegalStateException("No deferred register found in " + className);
        }
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(
            "Unable to initialize ProjectE registry class " + className, e);
      }
    }
  }

  private static synchronized void registerServerLifecycle() {
    if (!lifecycleRegistered) {
      lifecycleRegistered = true;
      ResourceManagerHelper.get(PackType.SERVER_DATA)
          .registerReloadListener(WorldTransmutationManager.INSTANCE);
      ServerLifecycleEvents.SERVER_STARTING.register(PECore::onServerStarting);
      ServerLifecycleEvents.SERVER_STARTED.register(PECore::onServerStarted);
      ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(PECore::onEndDataPackReload);
      ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
          (SyncDataPackContents) (player, joined) -> syncDataPackContents(player));
      ServerLifecycleEvents.BEFORE_SAVE.register(
          (BeforeSave)
              (server, flush, saving) -> {
                if (flush) {
                  flushPersistentFiles(server);
                }
              });
      ServerLifecycleEvents.SERVER_STOPPING.register(PECore::onServerStopping);
      ServerLifecycleEvents.SERVER_STOPPED.register(PECore::onServerStopped);
    }
  }

  private static void onServerStarting(MinecraftServer server) {
    ServerLifecycleHooks.setCurrentServer(server);
    LargeDataSync.resetServer();
    LOGGER.info("Starting ProjectE server systems");
    // Initialize persistent EMC together with the calculation below so a damaged file can
    // recover from this world's last complete table rather than aborting server startup.
    EMCMappingHandler.loadMappers();
  }

  private static void onServerStarted(MinecraftServer server) {
    if (lastMappedServer != server) {
      reloadProjectEData(server, server.getResourceManager());
      // The initial vanilla data-pack pass can happen before all ProjectE compatibility
      // condition handlers are ready. Run one normal reload after server startup so recipes
      // such as the Tome of Knowledge are evaluated immediately instead of only after /reload.
      server
          .reloadResources(server.getPackRepository().getSelectedIds())
          .exceptionally(
              throwable -> {
                LOGGER.error("Failed to finalize ProjectE data-pack initialization", throwable);
                return null;
              });
    }
  }

  private static void onEndDataPackReload(
      MinecraftServer server, CloseableResourceManager resourceManager, boolean succeeded) {
    if (succeeded && ServerLifecycleHooks.getCurrentServer() == server) {
      reloadProjectEData(server, resourceManager);

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        syncDataPackContents(player);
      }
    }
  }

  private static void reloadProjectEData(MinecraftServer server, ResourceManager resourceManager) {
    try {
      calculateEmcValues(server, resourceManager);
    } catch (Throwable throwable) {
      LOGGER.error("Error calculating ProjectE EMC values", throwable);
    }
  }

  /** Applies command changes on the server thread and synchronizes the resulting EMC data. */
  public static void refreshEMCValues(MinecraftServer server) {
    if (!CustomEMCParser.flush(server.registryAccess())) {
      throw new IllegalStateException("Could not save custom EMC settings");
    }
    calculateEmcValues(server, server.getResourceManager());
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      syncEmcContents(player);
    }
  }

  private static void calculateEmcValues(MinecraftServer server, ResourceManager resourceManager) {
    var previousValues =
        EMCMappingHandler.isAvailable() ? EMCMappingHandler.createPacketData().data() : null;
    try {
      AbstractNSSTag.clearCreatedTags();
      CustomEMCParser.init(server.registryAccess());
      EMCMappingHandler.loadMappers();
      ReloadableServerResources serverResources =
          ReloadableServerResourcesAccess.getManagers(server);
      EMCMappingHandler.map(serverResources, server.registryAccess(), resourceManager);
      lastMappedServer = server;
      LOGGER.info("Registered {} EMC values", EMCMappingHandler.getEmcMapSize());
      saveLastGoodEmc(server);
    } catch (RuntimeException exception) {
      if (previousValues != null) {
        if (EMCMappingHandler.createPacketData().data() != previousValues) {
          EMCMappingHandler.updateEmcValues(previousValues);
          FuelMapper.loadMap();
        }
        LOGGER.error(
            "EMC recalculation failed; all {} previous prices have been retained",
            previousValues.size(),
            exception);
      } else if (restoreLastGoodEmc(server)) {
        lastMappedServer = server;
        LOGGER.error(
            "EMC recalculation failed; recovered {} prices from this world's last complete table",
            EMCMappingHandler.getEmcMapSize(),
            exception);
        return;
      }
      throw exception;
    }
  }

  private static Path lastGoodEmcPath(MinecraftServer server) {
    return server.getWorldPath(LevelResource.ROOT).resolve("projecte").resolve("emc_last_good.json");
  }

  private static void saveLastGoodEmc(MinecraftServer server) {
    try {
      Path path = lastGoodEmcPath(server);
      var values = EMCMappingHandler.createPacketData().data();
      if (PregeneratedEMC.write(server.registryAccess(), path, values)) {
        PregeneratedEMC.write(
            server.registryAccess(), path.resolveSibling("emc_last_good.json.bak"), values);
      }
    } catch (RuntimeException exception) {
      LOGGER.error(
          "Could not persist the last complete EMC table; live prices remain available", exception);
    }
  }

  private static boolean restoreLastGoodEmc(MinecraftServer server) {
    Path path = lastGoodEmcPath(server);
    var restored =
        PregeneratedEMC.read(server.registryAccess(), path, true)
            .or(
                () ->
                    PregeneratedEMC.read(
                        server.registryAccess(), path.resolveSibling("emc_last_good.json.bak"), true));
    if (restored.isEmpty()) {
      return false;
    }
    EMCMappingHandler.restoreEmcValues(restored.get());
    return true;
  }

  private static void syncDataPackContents(ServerPlayer player) {
    syncEmcContents(player);
    PacketDistributor.sendToPlayer(player, WorldTransmutationManager.getSyncPacket());
  }

  private static void syncEmcContents(ServerPlayer player) {
    if (!EMCMappingHandler.isAvailable()) {
      LOGGER.warn(
          "EMC data is not ready for {}; retaining the player's previous price table",
          player.getName().getString());
      return;
    }
    try {
      PacketDistributor.sendToPlayer(player, EMCMappingHandler.createPacketData());
      PacketDistributor.sendToPlayer(player, FuelMapper.getSyncPacket());
    } catch (RuntimeException exception) {
      LOGGER.error(
          "Could not synchronize EMC to {}; server prices have been retained",
          player.getName().getString(),
          exception);
    }
  }

  private static void flushPersistentFiles(MinecraftServer server) {
    try {
      CustomEMCParser.flush(server.registryAccess());
      ProjectEConfig.saveAll();
    } catch (RuntimeException e) {
      LOGGER.error("Failed to flush ProjectE persistent data", e);
    }
  }

  private static void onServerStopping(MinecraftServer server) {
    flushPersistentFiles(server);

    for (IPEConfig config : ProjectEConfig.getConfigs()) {
      PECore.ModConfigContainer.unload(config);
    }
  }

  private static void onServerStopped(MinecraftServer server) {
    TransmutationOffline.cleanAll();
    LargeDataSync.resetServer();
    EMCMappingHandler.clearEmcMap();
    WorldHelper.clearCachedAgeProperties();
    ServerLifecycleHooks.clearCurrentServer(server);
    if (lastMappedServer == server) {
      lastMappedServer = null;
    }

    LOGGER.info("ProjectE server systems stopped");
  }

  public static <T extends Event> T postEvent(T event) {
    return NeoForge.EVENT_BUS.post(event);
  }

  public static PacketHandler packetHandler() {
    if (packetHandler == null) {
      throw new IllegalStateException(
          "ProjectE packet handler accessed before PECore initialization");
    } else {
      return packetHandler;
    }
  }

  public static void debugLog(String msg, Object... args) {
    LOGGER.debug(msg, args);
  }

  public static Identifier rl(String path) {
    return Identifier.fromNamespaceAndPath("projecte", path);
  }

  private static final class ModConfigContainer {
    private static void unload(IPEConfig config) {
      config.clearCache(true);
    }
  }
}
