package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.impl.capability.KnowledgeImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run after Fabric's data-attachment mixin so the backup is merged into the
// already decoded attachment map instead of being overwritten by it.
@Mixin(value = Entity.class, priority = 900)
abstract class EntityAttachmentMigrationMixin {
  private static final String PROJECTE_KNOWLEDGE_BACKUP = "projecte:knowledge_backup";
  private static final String PROJECTE_BAGS_BACKUP = "projecte:alchemical_bags_backup";

  @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
  private void projecte$migrateNeoForgePlayerAttachments(
      ValueInput input, CallbackInfo callbackInfo) {
    if ((Object) this instanceof ServerPlayer player) {
      CompoundTag neoForgeAttachments =
          (CompoundTag) input.read("neoforge:attachments", CompoundTag.CODEC).orElse(null);
      PEAttachmentTypes.migrateNeoForgeAttachments(player, neoForgeAttachments);

      input
          .read(PROJECTE_KNOWLEDGE_BACKUP, KnowledgeImpl.KnowledgeAttachment.CODEC)
          .ifPresent(backup -> {
            KnowledgeImpl.KnowledgeAttachment merged =
                player.hasAttached(PEAttachmentTypes.KNOWLEDGE)
                    ? ((KnowledgeImpl.KnowledgeAttachment)
                            player.getAttachedOrThrow(PEAttachmentTypes.KNOWLEDGE))
                        .copy()
                    : new KnowledgeImpl.KnowledgeAttachment();
            KnowledgeImpl.KnowledgeAttachment.LegacyMergeResult result =
                merged.mergeMigratedData(backup);
            if (!player.hasAttached(PEAttachmentTypes.KNOWLEDGE) || result.changed()) {
              player.setAttached(PEAttachmentTypes.KNOWLEDGE, merged);
            }
          });

      input
          .read(PROJECTE_BAGS_BACKUP, AlchBagImpl.AlchemicalBagAttachment.CODEC)
          .ifPresent(backup -> {
            if (!player.hasAttached(PEAttachmentTypes.ALCHEMICAL_BAGS)) {
              player.setAttached(PEAttachmentTypes.ALCHEMICAL_BAGS, backup.copy());
            } else {
              AlchBagImpl.AlchemicalBagAttachment merged =
                  ((AlchBagImpl.AlchemicalBagAttachment)
                          player.getAttachedOrThrow(PEAttachmentTypes.ALCHEMICAL_BAGS))
                      .copy();
              merged.mergeMigratedData(backup);
              player.setAttached(PEAttachmentTypes.ALCHEMICAL_BAGS, merged);
            }
          });
    }
  }

  @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
  private void projecte$writePlayerDataBackup(ValueOutput output, CallbackInfo callbackInfo) {
    if ((Object) this instanceof ServerPlayer player) {
      if (player.hasAttached(PEAttachmentTypes.KNOWLEDGE)) {
        output.store(
            PROJECTE_KNOWLEDGE_BACKUP,
            KnowledgeImpl.KnowledgeAttachment.CODEC,
            (KnowledgeImpl.KnowledgeAttachment)
                player.getAttachedOrThrow(PEAttachmentTypes.KNOWLEDGE));
      }
      if (player.hasAttached(PEAttachmentTypes.ALCHEMICAL_BAGS)) {
        output.store(
            PROJECTE_BAGS_BACKUP,
            AlchBagImpl.AlchemicalBagAttachment.CODEC,
            (AlchBagImpl.AlchemicalBagAttachment)
                player.getAttachedOrThrow(PEAttachmentTypes.ALCHEMICAL_BAGS));
      }
    }
  }
}
