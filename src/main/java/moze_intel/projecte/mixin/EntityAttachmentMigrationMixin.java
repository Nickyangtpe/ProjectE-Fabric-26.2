package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityAttachmentMigrationMixin {
  @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
  private void projecte$migrateNeoForgePlayerAttachments(
      ValueInput input, CallbackInfo callbackInfo) {
    if ((Object) this instanceof ServerPlayer player) {
      CompoundTag neoForgeAttachments =
          (CompoundTag) input.read("neoforge:attachments", CompoundTag.CODEC).orElse(null);
      PEAttachmentTypes.migrateNeoForgeAttachments(player, neoForgeAttachments);
    }
  }
}
