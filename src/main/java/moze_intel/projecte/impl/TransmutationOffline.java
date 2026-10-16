package moze_intel.projecte.impl;

import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import moze_intel.projecte.utils.ItemHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

public class TransmutationOffline {
  private static final IKnowledgeProvider NOT_FOUND_PROVIDER =
      immutableView(new KnowledgeImpl.KnowledgeAttachment());
  private static final Map<UUID, IKnowledgeProvider> cachedKnowledgeProviders = new HashMap<>();

  public static void cleanAll() {
    cachedKnowledgeProviders.clear();
  }

  public static void clear(UUID playerUUID) {
    cachedKnowledgeProviders.remove(playerUUID);
  }

  static IKnowledgeProvider forPlayer(MinecraftServer server, UUID playerUUID) {
    if (!cachedKnowledgeProviders.containsKey(playerUUID)
        && !cacheOfflineData(server, playerUUID)) {
      cachedKnowledgeProviders.put(playerUUID, NOT_FOUND_PROVIDER);
    }

    return cachedKnowledgeProviders.get(playerUUID);
  }

  private static boolean cacheOfflineData(MinecraftServer server, UUID playerUUID) {
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      throw new IllegalStateException("CRITICAL: Trying to read filesystem on client!!");
    }

    Path player =
        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerUUID.toString() + ".dat");
    if (Files.exists(player) && Files.isRegularFile(player)) {
      try (InputStream in = Files.newInputStream(player)) {
        CompoundTag playerDat = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        CompoundTag attachmentData =
            (CompoundTag) playerDat.getCompound("fabric:attachments").orElse(null);
        CompoundTag neoForgeAttachmentData =
            (CompoundTag) playerDat.getCompound("neoforge:attachments").orElse(null);
        if (attachmentData != null || neoForgeAttachmentData != null) {
          RegistryOps<Tag> serializationContext =
              server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
          KnowledgeImpl.KnowledgeAttachment attachment = new KnowledgeImpl.KnowledgeAttachment();
          boolean foundData = false;

          for (CompoundTag root : new CompoundTag[] {attachmentData, neoForgeAttachmentData}) {
            if (root != null) {
              Tag knowledgeData = root.get(PEAttachmentTypes.KNOWLEDGE.identifier().toString());
              if (knowledgeData != null) {
                DataResult<KnowledgeImpl.KnowledgeAttachment> result =
                    KnowledgeImpl.KnowledgeAttachment.CODEC.parse(
                        serializationContext, knowledgeData);
                if (!result.isSuccess()) {
                  result.ifError(
                      error ->
                          PECore.LOGGER.warn(
                              "Failed to cache offline data for API calls for UUID: {}. {}",
                              playerUUID,
                              error.message()));
                } else {
                  KnowledgeImpl.KnowledgeAttachment loaded =
                      (KnowledgeImpl.KnowledgeAttachment) result.getOrThrow();
                  if (foundData) {
                    KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult merge =
                        attachment.mergeMigratedData(loaded);
                    if (!merge.emcMerged() || !merge.inputLocksMerged()) {
                      PECore.LOGGER.warn(
                          "Conflicting knowledge payloads while reading offline player {}",
                          playerUUID);
                    }
                  } else {
                    attachment = loaded;
                  }

                  foundData = true;
                }
              }

              BigInteger legacyEmc = null;
              Tag legacyEmcTag = root.get(PEAttachmentTypes.LEGACY_EMC.identifier().toString());
              if (legacyEmcTag != null) {
                DataResult<BigInteger> result =
                    PEAttachmentTypes.LEGACY_EMC_CODEC.parse(serializationContext, legacyEmcTag);
                if (result.isSuccess()) {
                  legacyEmc = (BigInteger) result.getOrThrow();
                  foundData = true;
                } else {
                  result.ifError(
                      error ->
                          PECore.LOGGER.warn(
                              "Failed to read legacy EMC for offline player {}: {}",
                              playerUUID,
                              error.message()));
                }
              }

              ItemStackHandler legacyInputLocks = null;
              Tag legacyInputLocksTag =
                  root.get(PEAttachmentTypes.LEGACY_INPUT_LOCKS.identifier().toString());
              if (legacyInputLocksTag != null) {
                DataResult<ItemStackHandler> result =
                    PEAttachmentTypes.LEGACY_INPUT_LOCKS_CODEC.parse(
                        serializationContext, legacyInputLocksTag);
                if (result.isSuccess()) {
                  legacyInputLocks = (ItemStackHandler) result.getOrThrow();
                  foundData = true;
                } else {
                  result.ifError(
                      error ->
                          PECore.LOGGER.warn(
                              "Failed to read legacy input locks for offline player {}: {}",
                              playerUUID,
                              error.message()));
                }
              }

              if (legacyEmc != null || legacyInputLocks != null) {
                KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult merge =
                    attachment.mergeLegacyData(legacyEmc, legacyInputLocks);
                if (!merge.emcMerged() || !merge.inputLocksMerged()) {
                  PECore.LOGGER.warn(
                      "Conflicting legacy ProjectE player data while reading offline player {}",
                      playerUUID);
                }
              }
            }
          }

          if (foundData) {
            cachedKnowledgeProviders.put(playerUUID, immutableView(attachment));
            PECore.debugLog("Caching offline data for UUID: {}", playerUUID);
            return true;
          }
        }
      } catch (IOException e) {
        PECore.LOGGER.warn("Failed to cache offline data for API calls for UUID: {}", playerUUID);
      }
    }

    return false;
  }

  private static IKnowledgeProvider immutableView(KnowledgeImpl.KnowledgeAttachment attachment) {
    final IKnowledgeProvider toCopy = KnowledgeImpl.wrapAttachment(attachment);
    return new IKnowledgeProvider() {
      final Set<ItemInfo> immutableKnowledge = Collections.unmodifiableSet(toCopy.getKnowledge());
      final IItemHandlerModifiable immutableInputLocks =
          ItemHelper.immutableCopy(toCopy.getInputAndLocks());

      @Override
      public boolean hasFullKnowledge() {
        return toCopy.hasFullKnowledge();
      }

      @Override
      public void setFullKnowledge(boolean fullKnowledge) {}

      @Override
      public void clearKnowledge() {}

      @Override
      public boolean hasKnowledge(@NotNull ItemInfo info) {
        return toCopy.hasKnowledge(info);
      }

      @Override
      public boolean addKnowledge(@NotNull ItemInfo info) {
        return false;
      }

      @Override
      public boolean removeKnowledge(@NotNull ItemInfo info) {
        return false;
      }

      @NotNull
      @Override
      public Set<ItemInfo> getKnowledge() {
        return this.immutableKnowledge;
      }

      @NotNull
      @Override
      public IItemHandler getInputAndLocks() {
        return this.immutableInputLocks;
      }

      @Override
      public BigInteger getEmc() {
        return toCopy.getEmc();
      }

      @Override
      public void setEmc(BigInteger emc) {}

      @Override
      public void sync(@NotNull ServerPlayer player) {
        toCopy.sync(player);
      }

      @Override
      public void syncEmc(@NotNull ServerPlayer player) {
        toCopy.syncEmc(player);
      }

      @Override
      public void syncKnowledgeChange(
          @NotNull ServerPlayer player, ItemInfo change, boolean learned) {
        toCopy.syncKnowledgeChange(player, change, learned);
      }

      @Override
      public void syncInputAndLocks(
          @NotNull ServerPlayer player,
          IntList slotsChanged,
          IKnowledgeProvider.TargetUpdateType updateTargets) {
        toCopy.syncInputAndLocks(player, slotsChanged, updateTargets);
      }

      @Override
      public void receiveInputsAndLocks(Int2ObjectMap<ItemStack> changes) {}
    };
  }
}
