package moze_intel.projecte.impl.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.common.NeoForge;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.impl.codec.PECodecHelper;
import moze_intel.projecte.network.PEStreamCodecs;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KnowledgeImpl implements IKnowledgeProvider {
  private final Player player;

  public static IKnowledgeProvider wrapAttachment(
      final KnowledgeImpl.KnowledgeAttachment attachment) {
    return new KnowledgeImpl(null) {
      @Override
      protected KnowledgeImpl.KnowledgeAttachment attachment() {
        return attachment;
      }
    };
  }

  public KnowledgeImpl(Player player) {
    this.player = player;
  }

  protected KnowledgeImpl.KnowledgeAttachment attachment() {
    Objects.requireNonNull(this.player);
    return (KnowledgeImpl.KnowledgeAttachment)
        this.player.getAttachedOrCreate(PEAttachmentTypes.KNOWLEDGE);
  }

  protected void fireChangedEvent() {
    if (this.player != null && !this.player.level().isClientSide()) {
      NeoForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(this.player));
    }
  }

  @Override
  public boolean hasFullKnowledge() {
    return this.attachment().fullKnowledge;
  }

  @Override
  public void setFullKnowledge(boolean fullKnowledge) {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    if (attachment.fullKnowledge != fullKnowledge) {
      attachment.fullKnowledge = fullKnowledge;
      this.fireChangedEvent();
    }
  }

  @Override
  public void clearKnowledge() {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    boolean hasKnowledge = attachment.fullKnowledge || !attachment.knowledge.isEmpty();
    attachment.knowledge.clear();
    attachment.fullKnowledge = false;
    if (hasKnowledge) {
      this.fireChangedEvent();
    }
  }

  @Nullable
  private ItemInfo getIfPersistent(@NotNull ItemInfo info) {
    if (info.hasModifiedComponents() && !EMCMappingHandler.hasEmcValue(info)) {
      ItemInfo cleanedInfo = IEMCProxy.INSTANCE.getPersistentInfo(info);
      return cleanedInfo.hasModifiedComponents() && !EMCMappingHandler.hasEmcValue(cleanedInfo)
          ? cleanedInfo
          : null;
    } else {
      return null;
    }
  }

  @Override
  public boolean hasKnowledge(@NotNull ItemInfo info) {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    if (!attachment.fullKnowledge) {
      return attachment.knowledge.contains(IEMCProxy.INSTANCE.getPersistentInfo(info));
    }

    ItemInfo persistentInfo = this.getIfPersistent(info);
    return persistentInfo == null || attachment.knowledge.contains(persistentInfo);
  }

  @Override
  public boolean addKnowledge(@NotNull ItemInfo info) {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    if (attachment.fullKnowledge) {
      ItemInfo persistentInfo = this.getIfPersistent(info);
      return persistentInfo == null ? false : this.tryAdd(attachment, persistentInfo);
    } else if (info.getItem().is(PEItems.TOME_OF_KNOWLEDGE.getKey())) {
      info = info.itemOnly();
      attachment.knowledge.add(info);
      attachment.fullKnowledge = true;
      this.fireChangedEvent();
      return true;
    } else {
      return this.tryAdd(attachment, IEMCProxy.INSTANCE.getPersistentInfo(info));
    }
  }

  private boolean tryAdd(
      @NotNull KnowledgeImpl.KnowledgeAttachment attachment, @NotNull ItemInfo cleanedInfo) {
    if (attachment.knowledge.add(cleanedInfo)) {
      this.fireChangedEvent();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean removeKnowledge(@NotNull ItemInfo info) {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    if (attachment.fullKnowledge) {
      if (info.getItem().is(PEItems.TOME_OF_KNOWLEDGE.getKey())) {
        info = info.itemOnly();
        attachment.knowledge.remove(info);
        attachment.fullKnowledge = false;
        this.fireChangedEvent();
        return true;
      } else {
        ItemInfo persistentInfo = this.getIfPersistent(info);
        return persistentInfo != null && this.tryRemove(attachment, persistentInfo);
      }
    } else {
      return this.tryRemove(attachment, IEMCProxy.INSTANCE.getPersistentInfo(info));
    }
  }

  private boolean tryRemove(
      @NotNull KnowledgeImpl.KnowledgeAttachment attachment, @NotNull ItemInfo cleanedInfo) {
    if (attachment.knowledge.remove(cleanedInfo)) {
      this.fireChangedEvent();
      return true;
    } else {
      return false;
    }
  }

  @NotNull
  @Override
  public Set<ItemInfo> getKnowledge() {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    if (attachment.fullKnowledge) {
      Set<ItemInfo> allKnowledge = EMCMappingHandler.getMappedItems();
      allKnowledge.addAll(attachment.knowledge);
      return Collections.unmodifiableSet(allKnowledge);
    } else {
      return Collections.unmodifiableSet(attachment.knowledge);
    }
  }

  @NotNull
  public IItemHandlerModifiable getInputAndLocks() {
    return this.attachment().inputLocks;
  }

  @Override
  public BigInteger getEmc() {
    return this.attachment().emc;
  }

  @Override
  public void setEmc(BigInteger emc) {
    this.attachment().emc = emc;
  }

  @Override
  public void sync(@NotNull ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, new KnowledgeSyncPKT(this.attachment()));
  }

  @Override
  public void syncEmc(@NotNull ServerPlayer player) {
    PacketDistributor.sendToPlayer(player, new KnowledgeSyncEmcPKT(this.getEmc()));
  }

  @Override
  public void syncKnowledgeChange(@NotNull ServerPlayer player, ItemInfo change, boolean learned) {
    PacketDistributor.sendToPlayer(player, new KnowledgeSyncChangePKT(change, learned));
  }

  @Override
  public void syncInputAndLocks(
      @NotNull ServerPlayer player,
      IntList slotsChanged,
      IKnowledgeProvider.TargetUpdateType updateTargets) {
    if (!slotsChanged.isEmpty()) {
      KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
      int slots = attachment.inputLocks.getSlots();
      Int2ObjectMap<ItemStack> stacksToSync = new Int2ObjectOpenHashMap();
      IntListIterator var7 = slotsChanged.iterator();

      while (var7.hasNext()) {
        int slot = (Integer) var7.next();
        if (slot >= 0 && slot < slots) {
          stacksToSync.put(slot, attachment.inputLocks.getStackInSlot(slot));
        }
      }

      if (!stacksToSync.isEmpty()) {
        PacketDistributor.sendToPlayer(
            player, new KnowledgeSyncInputsAndLocksPKT(stacksToSync, updateTargets));
      }
    }
  }

  @Override
  public void receiveInputsAndLocks(Int2ObjectMap<ItemStack> changes) {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    int slots = attachment.inputLocks.getSlots();
    Iterator<Entry<ItemStack>> iterator = Int2ObjectMaps.fastIterator(changes);

    while (iterator.hasNext()) {
      Entry<ItemStack> entry = iterator.next();
      int slot = entry.getIntKey();
      if (slot >= 0 && slot < slots) {
        attachment.inputLocks.setStackInSlot(slot, (ItemStack) entry.getValue());
      }
    }
  }

  public final boolean pruneStaleKnowledge() {
    KnowledgeImpl.KnowledgeAttachment attachment = this.attachment();
    List<ItemInfo> toAdd = new ArrayList<>();
    boolean hasRemoved = false;
    Iterator<ItemInfo> iterator = attachment.knowledge.iterator();

    while (iterator.hasNext()) {
      ItemInfo info = iterator.next();
      ItemInfo persistentInfo = IEMCProxy.INSTANCE.getPersistentInfo(info);
      if (!info.equals(persistentInfo)) {
        if (IEMCProxy.INSTANCE.hasValue(persistentInfo)) {
          toAdd.add(persistentInfo);
        }

        iterator.remove();
        hasRemoved = true;
      } else if (!IEMCProxy.INSTANCE.hasValue(info)) {
        iterator.remove();
        hasRemoved = true;
      }
    }

    return attachment.knowledge.addAll(toAdd) || hasRemoved;
  }

  public static class KnowledgeAttachment {
    private static final int LOCK_SLOTS = 9;
    private static final Codec<Set<ItemInfo>> MUTABLE_KNOWLEDGE_CODEC =
        ItemInfo.CODEC
            .listOf()
            .promotePartial(
                error -> PECore.LOGGER.error("Failed to load stored knowledge: {}", error))
            .xmap(HashSet::new, List::copyOf);
    public static final Codec<KnowledgeImpl.KnowledgeAttachment> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        MUTABLE_KNOWLEDGE_CODEC
                            .fieldOf("knowledge")
                            .forGetter(attachment -> attachment.knowledge),
                        PECodecHelper.MUTABLE_HANDLER_CODEC
                            .optionalFieldOf("input_locks")
                            .xmap(
                                locks -> locks.orElseGet(() -> new ItemStackHandler(9)),
                                Optional::of)
                            .forGetter(attachment -> attachment.inputLocks),
                        IPECodecHelper.INSTANCE
                            .nonNegativeBigInt()
                            .optionalFieldOf("emc", BigInteger.ZERO)
                            .forGetter(attachment -> attachment.emc),
                        Codec.BOOL
                            .optionalFieldOf("full_knowledge", false)
                            .forGetter(attachment -> attachment.fullKnowledge))
                    .apply(instance, KnowledgeImpl.KnowledgeAttachment::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeImpl.KnowledgeAttachment>
        STREAM_CODEC =
            StreamCodec.composite(
                ItemInfo.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
                attachment -> attachment.knowledge,
                PEStreamCodecs.handlerStreamCodec(9),
                attachment -> attachment.inputLocks,
                PEStreamCodecs.EMC_VALUE,
                attachment -> attachment.emc,
                ByteBufCodecs.BOOL,
                attachment -> attachment.fullKnowledge,
                KnowledgeImpl.KnowledgeAttachment::new);
    private final ItemStackHandler inputLocks;
    private final Set<ItemInfo> knowledge;
    private boolean fullKnowledge;
    private BigInteger emc;

    public KnowledgeAttachment() {
      this(new HashSet<>(), new ItemStackHandler(9), BigInteger.ZERO, false);
    }

    private KnowledgeAttachment(
        Set<ItemInfo> knowledge,
        ItemStackHandler inputLocks,
        BigInteger emc,
        boolean fullKnowledge) {
      this.knowledge = knowledge;
      this.inputLocks = inputLocks;
      this.emc = emc;
      this.fullKnowledge = fullKnowledge;
    }

    public KnowledgeImpl.KnowledgeAttachment copy() {
      return new KnowledgeImpl.KnowledgeAttachment(
          new HashSet<>(this.knowledge),
          PEAttachmentTypes.copyHandler(this.inputLocks, ItemStackHandler::new),
          this.emc,
          this.fullKnowledge);
    }

    public KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult mergeMigratedData(
        KnowledgeImpl.KnowledgeAttachment legacy) {
      boolean changed = this.knowledge.addAll(legacy.knowledge);
      if (legacy.fullKnowledge && !this.fullKnowledge) {
        this.fullKnowledge = true;
        changed = true;
      }

      KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult legacyData =
          this.mergeLegacyData(legacy.emc, legacy.inputLocks);
      return new KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult(
          changed || legacyData.changed(), legacyData.emcMerged(), legacyData.inputLocksMerged());
    }

    public KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult mergeLegacyData(
        @Nullable BigInteger legacyEmc, @Nullable IItemHandler legacyInputLocks) {
      boolean changed = false;
      boolean emcMerged =
          legacyEmc == null || legacyEmc.signum() == 0 || this.emc.equals(legacyEmc);
      if (legacyEmc != null && legacyEmc.signum() > 0 && this.emc.signum() == 0) {
        this.emc = legacyEmc;
        changed = true;
        emcMerged = true;
      } else if (legacyEmc != null && legacyEmc.signum() > 0 && !this.emc.equals(legacyEmc)) {
        emcMerged = false;
      }

      boolean inputLocksMerged = true;
      if (legacyInputLocks != null) {
        for (int slot = 0; slot < legacyInputLocks.getSlots(); slot++) {
          ItemStack legacyStack = legacyInputLocks.getStackInSlot(slot);
          if (!legacyStack.isEmpty()) {
            if (slot >= this.inputLocks.getSlots()) {
              inputLocksMerged = false;
            } else {
              ItemStack currentStack = this.inputLocks.getStackInSlot(slot);
              if (currentStack.isEmpty()) {
                this.inputLocks.setStackInSlot(slot, legacyStack.copy());
                changed = true;
              } else if (!ItemStack.matches(currentStack, legacyStack)) {
                inputLocksMerged = false;
              }
            }
          }
        }
      }

      return new KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult(
          changed, emcMerged, inputLocksMerged);
    }

    public record LegacyMergeResult(boolean changed, boolean emcMerged, boolean inputLocksMerged) {}
  }
}
