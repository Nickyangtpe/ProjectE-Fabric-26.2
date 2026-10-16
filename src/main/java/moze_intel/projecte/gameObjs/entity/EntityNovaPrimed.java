package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import moze_intel.projecte.gameObjs.registration.impl.BlockRegistryObject;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class EntityNovaPrimed extends PrimedTnt {
  public EntityNovaPrimed(EntityType<? extends EntityNovaPrimed> type, Level level) {
    super(type, level);
    this.setupEntity();
  }

  public EntityNovaPrimed(Level level, double x, double y, double z, LivingEntity placer) {
    super(level, x, y, z, placer);
    this.setupEntity();
    this.blocksBuilding = true;
  }

  private void setupEntity() {
    this.setBlockState(this.getBlock().defaultState());
    this.setFuse(this.getFuse() / 4);
  }

  protected abstract BlockRegistryObject<ProjectETNT, ?> getBlock();

  @NotNull
  public abstract EntityType<?> getType();

  protected float getExplosionPower() {
    return 16.0F;
  }

  public void explodeProjectE() {
    WorldHelper.createNovaExplosion(
        this.level(), this, this.getX(), this.getY(), this.getZ(), this.getExplosionPower());
  }

  public ItemStack getPickResult() {
    return new ItemStack(this.getBlock());
  }
}
