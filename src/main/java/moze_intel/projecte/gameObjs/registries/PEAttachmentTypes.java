package moze_intel.projecte.gameObjs.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.compat.items.ItemStackHandler;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import moze_intel.projecte.impl.codec.PECodecHelper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.CopyFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PEAttachmentTypes {
  private static final String NEOFORGE_ATTACHMENT_KEY = "neoforge:attachments";
  private static final String PROJECTE_PREFIX = "projecte:";
  private static boolean copyEventsRegistered;
  public static final AttachmentType<AlchBagImpl.AlchemicalBagAttachment> ALCHEMICAL_BAGS =
      AttachmentRegistry.create(
          PECore.rl("alchemical_bags"),
          builder ->
              builder
                  .initializer(AlchBagImpl.AlchemicalBagAttachment::new)
                  .persistent(AlchBagImpl.AlchemicalBagAttachment.CODEC)
                  .copyOnDeath());
  public static final AttachmentType<KnowledgeImpl.KnowledgeAttachment> KNOWLEDGE =
      AttachmentRegistry.create(
          PECore.rl("knowledge"),
          builder ->
              builder
                  .initializer(KnowledgeImpl.KnowledgeAttachment::new)
                  .persistent(KnowledgeImpl.KnowledgeAttachment.CODEC)
                  .copyOnDeath());
  public static final Codec<ItemStackHandler> LEGACY_INPUT_LOCKS_CODEC =
      PECodecHelper.MUTABLE_HANDLER_CODEC;
  public static final Codec<BigInteger> LEGACY_EMC_CODEC =
      IPECodecHelper.INSTANCE.nonNegativeBigInt();
  public static final AttachmentType<ItemStackHandler> LEGACY_INPUT_LOCKS =
      AttachmentRegistry.create(
          PECore.rl("input_locks"),
          builder -> builder.persistent(LEGACY_INPUT_LOCKS_CODEC).copyOnDeath());
  public static final AttachmentType<BigInteger> LEGACY_EMC =
      AttachmentRegistry.create(
          PECore.rl("emc"), builder -> builder.persistent(LEGACY_EMC_CODEC).copyOnDeath());
  public static final AttachmentType<Boolean> SWIFTWOLF_FLIGHT =
      AttachmentRegistry.create(
          PECore.rl("swiftwolf_flight"), builder -> builder.persistent(Codec.BOOL).copyOnDeath());
  public static final AttachmentType<Boolean> GEM_ARMOR_STATE =
      AttachmentRegistry.create(
          PECore.rl("gem_armor_state"),
          builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
  public static final AttachmentType<List<CompoundTag>> NEOFORGE_ATTACHMENT_BACKUP =
      AttachmentRegistry.create(
          PECore.rl("neoforge_attachment_backup"),
          builder -> builder.persistent(CompoundTag.CODEC.listOf()).copyOnDeath());

  private PEAttachmentTypes() {}

  public static synchronized void initialize() {
    if (!copyEventsRegistered) {
      copyEventsRegistered = true;
      ServerPlayerEvents.COPY_FROM.register(
          (CopyFrom)
              (oldPlayer, newPlayer, alive) -> {
                if (oldPlayer.hasAttached(ALCHEMICAL_BAGS)) {
                  newPlayer.setAttached(
                      ALCHEMICAL_BAGS,
                      ((AlchBagImpl.AlchemicalBagAttachment)
                              oldPlayer.getAttachedOrThrow(ALCHEMICAL_BAGS))
                          .copy());
                }

                if (oldPlayer.hasAttached(KNOWLEDGE)) {
                  newPlayer.setAttached(
                      KNOWLEDGE,
                      ((KnowledgeImpl.KnowledgeAttachment) oldPlayer.getAttachedOrThrow(KNOWLEDGE))
                          .copy());
                }

                if (oldPlayer.hasAttached(LEGACY_INPUT_LOCKS)) {
                  newPlayer.setAttached(
                      LEGACY_INPUT_LOCKS,
                      (ItemStackHandler)
                          copyHandler(
                              (IItemHandler) oldPlayer.getAttachedOrThrow(LEGACY_INPUT_LOCKS),
                              ItemStackHandler::new));
                }

                if (oldPlayer.hasAttached(NEOFORGE_ATTACHMENT_BACKUP)) {
                  newPlayer.setAttached(
                      NEOFORGE_ATTACHMENT_BACKUP,
                      copyTags(
                          (List<CompoundTag>)
                              oldPlayer.getAttachedOrThrow(NEOFORGE_ATTACHMENT_BACKUP)));
                }
              });
    }
  }

  public static void migrateNeoForgeAttachments(
      ServerPlayer player, @Nullable CompoundTag neoForgeRoot) {
    List<CompoundTag> sources = new ArrayList<>();
    if (player.hasAttached(NEOFORGE_ATTACHMENT_BACKUP)) {
      for (CompoundTag backup :
          (List<CompoundTag>) (List<?>) player.getAttachedOrThrow(NEOFORGE_ATTACHMENT_BACKUP)) {
        CompoundTag copy = copyProjectEEntries(backup);
        if (!copy.isEmpty() && !sources.contains(copy)) {
          sources.add(copy);
        }
      }
    }

    CompoundTag incoming = copyProjectEEntries(neoForgeRoot);
    if (!incoming.isEmpty() && !sources.contains(incoming)) {
      sources.add(incoming);
    }

    if (!sources.isEmpty()) {
      RegistryOps<Tag> serializationContext =
          player.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
      List<CompoundTag> unresolvedRoots = new ArrayList<>();

      for (CompoundTag source : sources) {
        CompoundTag unresolved = new CompoundTag();

        for (String key : source.keySet()) {
          Tag value = source.get(key);
          if ((value == null || !migrateNeoForgeEntry(player, key, value, serializationContext))
              && value != null) {
            unresolved.put(key, value.copy());
          }
        }

        if (!unresolved.isEmpty() && !unresolvedRoots.contains(unresolved)) {
          unresolvedRoots.add(unresolved);
        }
      }

      if (unresolvedRoots.isEmpty()) {
        player.removeAttached(NEOFORGE_ATTACHMENT_BACKUP);
      } else {
        player.setAttached(NEOFORGE_ATTACHMENT_BACKUP, copyTags(unresolvedRoots));
      }
    }

    migrateLegacyPlayerData(player);
  }

  private static boolean migrateNeoForgeEntry(
      ServerPlayer player, String key, Tag value, RegistryOps<Tag> serializationContext) {
    return switch (key) {
      case "projecte:knowledge" -> {
        KnowledgeImpl.KnowledgeAttachment legacy =
            parseLegacyKnowledge(serializationContext, value, key);
        yield legacy != null && mergeKnowledge(player, legacy);
      }
      case "projecte:emc" -> {
        BigInteger legacy = parseLegacyEmc(serializationContext, value, key);
        yield legacy != null && mergeKnowledgeData(player, legacy, null);
      }
      case "projecte:input_locks" -> {
        ItemStackHandler legacy = parseLegacyInputLocks(serializationContext, value, key);
        yield legacy != null && mergeKnowledgeData(player, null, legacy);
      }
      case "projecte:alchemical_bags" -> {
        AlchBagImpl.AlchemicalBagAttachment legacy =
            parseLegacyValue(
                AlchBagImpl.AlchemicalBagAttachment.CODEC, serializationContext, value, key);
        if (legacy == null) {
          yield false;
        } else if (!player.hasAttached(ALCHEMICAL_BAGS)) {
          player.setAttached(ALCHEMICAL_BAGS, legacy.copy());
          yield true;
        } else {
          AlchBagImpl.AlchemicalBagAttachment merged =
              ((AlchBagImpl.AlchemicalBagAttachment) player.getAttachedOrThrow(ALCHEMICAL_BAGS))
                  .copy();
          boolean complete = merged.mergeMigratedData(legacy);
          player.setAttached(ALCHEMICAL_BAGS, merged);
          yield complete;
        }
      }
      case "projecte:swiftwolf_flight" ->
          migrateBoolean(player, SWIFTWOLF_FLIGHT, value, serializationContext, key);
      case "projecte:gem_armor_state" ->
          migrateBoolean(player, GEM_ARMOR_STATE, value, serializationContext, key);
      default -> {
        PECore.LOGGER.warn(
            "Preserving unrecognized ProjectE NeoForge attachment {} for player {}",
            key,
            player.getUUID());
        yield false;
      }
    };
  }

  private static boolean migrateBoolean(
      ServerPlayer player,
      AttachmentType<Boolean> type,
      Tag value,
      RegistryOps<Tag> serializationContext,
      String key) {
    Boolean legacy = parseLegacyValue(Codec.BOOL, serializationContext, value, key);
    if (legacy == null) {
      return false;
    } else if (!player.hasAttached(type)) {
      player.setAttached(type, legacy);
      return true;
    } else {
      boolean current = (Boolean) player.getAttachedOrThrow(type);
      if (current != legacy) {
        PECore.LOGGER.warn(
            "Conflicting {} attachment for player {}; preserving both values until resolved",
            key,
            player.getUUID());
        return false;
      } else {
        return true;
      }
    }
  }

  private static boolean mergeKnowledge(
      ServerPlayer player, KnowledgeImpl.KnowledgeAttachment legacy) {
    boolean hasCurrent = player.hasAttached(KNOWLEDGE);
    KnowledgeImpl.KnowledgeAttachment merged =
        hasCurrent
            ? ((KnowledgeImpl.KnowledgeAttachment) player.getAttachedOrThrow(KNOWLEDGE)).copy()
            : new KnowledgeImpl.KnowledgeAttachment();
    KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult result = merged.mergeMigratedData(legacy);
    if (!hasCurrent || result.changed()) {
      player.setAttached(KNOWLEDGE, merged);
    }

    if (!result.emcMerged() || !result.inputLocksMerged()) {
      PECore.LOGGER.warn(
          "Conflicting legacy knowledge payload for player {}; retaining unresolved NeoForge data",
          player.getUUID());
    }

    return result.emcMerged() && result.inputLocksMerged();
  }

  private static boolean mergeKnowledgeData(
      ServerPlayer player, @Nullable BigInteger emc, @Nullable IItemHandler locks) {
    boolean hasCurrent = player.hasAttached(KNOWLEDGE);
    KnowledgeImpl.KnowledgeAttachment merged =
        hasCurrent
            ? ((KnowledgeImpl.KnowledgeAttachment) player.getAttachedOrThrow(KNOWLEDGE)).copy()
            : new KnowledgeImpl.KnowledgeAttachment();
    KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult result = merged.mergeLegacyData(emc, locks);
    if (!hasCurrent || result.changed()) {
      player.setAttached(KNOWLEDGE, merged);
    }

    if (!result.emcMerged() || !result.inputLocksMerged()) {
      PECore.LOGGER.warn(
          "Conflicting legacy EMC or input-lock data for player {}; retaining unresolved NeoForge"
              + " data",
          player.getUUID());
    }

    return result.emcMerged() && result.inputLocksMerged();
  }

  @Nullable
  private static KnowledgeImpl.KnowledgeAttachment parseLegacyKnowledge(
      RegistryOps<Tag> serializationContext, Tag value, String key) {
    if (value instanceof CompoundTag compound) {
      CompoundTag normalized = compound.copy();
      Tag locks = normalized.get("input_locks");
      if (locks != null) {
        Tag normalizedLocks = normalizeLegacyInputLocks(locks);
        if (normalizedLocks == null) {
          PECore.LOGGER.warn(
              "Failed to migrate NeoForge ProjectE attachment {}: unsupported input_locks format",
              key);
          return null;
        }

        normalized.put("input_locks", normalizedLocks);
      }

      if (normalized.get("emc") instanceof NumericTag numeric
          && numeric.getId() >= 1
          && numeric.getId() <= 4) {
        normalized.putString("emc", Long.toString(numeric.longValue()));
      }

      return parseLegacyValue(
          KnowledgeImpl.KnowledgeAttachment.CODEC, serializationContext, normalized, key);
    } else {
      return parseLegacyValue(
          KnowledgeImpl.KnowledgeAttachment.CODEC, serializationContext, value, key);
    }
  }

  @Nullable
  private static BigInteger parseLegacyEmc(
      RegistryOps<Tag> serializationContext, Tag value, String key) {
    if (value instanceof NumericTag numeric && numeric.getId() >= 1 && numeric.getId() <= 4) {
      long amount = numeric.longValue();
      if (amount >= 0L) {
        return BigInteger.valueOf(amount);
      }

      PECore.LOGGER.warn(
          "Failed to migrate NeoForge ProjectE attachment {}: EMC must be non-negative", key);
      return null;
    } else {
      return parseLegacyValue(LEGACY_EMC_CODEC, serializationContext, value, key);
    }
  }

  @Nullable
  private static ItemStackHandler parseLegacyInputLocks(
      RegistryOps<Tag> serializationContext, Tag value, String key) {
    Tag normalized = normalizeLegacyInputLocks(value);
    if (normalized == null) {
      PECore.LOGGER.warn(
          "Failed to migrate NeoForge ProjectE attachment {}: unsupported input-lock slot format",
          key);
      return null;
    } else {
      return parseLegacyValue(LEGACY_INPUT_LOCKS_CODEC, serializationContext, normalized, key);
    }
  }

  @Nullable
  private static Tag normalizeLegacyInputLocks(Tag value) {
    Tag slots = value;
    if (value instanceof CompoundTag compound) {
      Tag wrappedSlots = compound.get("slots");
      if (wrappedSlots instanceof ListTag) {
        slots = wrappedSlots;
      }
    }

    if (slots instanceof ListTag list) {
      ListTag normalized = new ListTag();

      for (Tag slot : list) {
        if (slot instanceof StringTag string) {
          if (!string.value().isEmpty()) {
            return null;
          }

          normalized.add(new CompoundTag());
        } else {
          normalized.add(slot.copy());
        }
      }

      return normalized;
    } else {
      return value.copy();
    }
  }

  @Nullable
  private static <T> T parseLegacyValue(
      Codec<T> codec, RegistryOps<Tag> serializationContext, Tag value, String key) {
    DataResult<T> result = codec.parse(serializationContext, value);
    if (result.isSuccess()) {
      return (T) result.getOrThrow();
    }

    result.ifError(
        error ->
            PECore.LOGGER.warn(
                "Failed to migrate NeoForge ProjectE attachment {}: {}", key, error.message()));
    return null;
  }

  private static CompoundTag copyProjectEEntries(@Nullable CompoundTag source) {
    CompoundTag copy = new CompoundTag();
    if (source != null) {
      for (String key : source.keySet()) {
        if (key.startsWith("projecte:")) {
          Tag value = source.get(key);
          if (value != null) {
            copy.put(key, value.copy());
          }
        }
      }
    }

    return copy;
  }

  private static List<CompoundTag> copyTags(List<CompoundTag> source) {
    List<CompoundTag> copy = new ArrayList<>(source.size());

    for (CompoundTag tag : source) {
      copy.add(tag.copy());
    }

    return List.copyOf(copy);
  }

  public static void migrateLegacyPlayerData(ServerPlayer player) {
    boolean hasLegacyEmc = player.hasAttached(LEGACY_EMC);
    boolean hasLegacyInputLocks = player.hasAttached(LEGACY_INPUT_LOCKS);
    if (hasLegacyEmc || hasLegacyInputLocks) {
      KnowledgeImpl.KnowledgeAttachment knowledge =
          player.hasAttached(KNOWLEDGE)
              ? ((KnowledgeImpl.KnowledgeAttachment) player.getAttachedOrThrow(KNOWLEDGE)).copy()
              : new KnowledgeImpl.KnowledgeAttachment();
      BigInteger legacyEmc =
          hasLegacyEmc ? (BigInteger) player.getAttachedOrThrow(LEGACY_EMC) : null;
      IItemHandler legacyInputLocks =
          hasLegacyInputLocks ? (IItemHandler) player.getAttachedOrThrow(LEGACY_INPUT_LOCKS) : null;
      KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult result =
          knowledge.mergeLegacyData(legacyEmc, legacyInputLocks);
      if (result.changed()) {
        player.setAttached(KNOWLEDGE, knowledge);
      }

      if (hasLegacyEmc) {
        if (result.emcMerged()) {
          player.removeAttached(LEGACY_EMC);
        } else {
          PECore.LOGGER.warn(
              "Preserving conflicting legacy EMC attachment for player {}", player.getUUID());
        }
      }

      if (hasLegacyInputLocks) {
        if (result.inputLocksMerged()) {
          player.removeAttached(LEGACY_INPUT_LOCKS);
        } else {
          PECore.LOGGER.warn(
              "Preserving conflicting legacy input-lock attachment for player {}",
              player.getUUID());
        }
      }
    }
  }

  public static <HANDLER extends IItemHandlerModifiable> HANDLER copyHandler(
      IItemHandler handler, Int2ObjectFunction<HANDLER> handlerCreator) {
    int slots = handler.getSlots();
    HANDLER handlerCopy = (HANDLER) handlerCreator.get(slots);

    for (int i = 0; i < slots; i++) {
      ItemStack stack = handler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        handlerCopy.setStackInSlot(i, stack.copy());
      }
    }

    return handlerCopy;
  }
}
