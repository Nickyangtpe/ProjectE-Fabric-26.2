package moze_intel.projecte.compat.fluids;

import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class FluidStack {
  public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);
  private final Fluid fluid;
  private int amount;
  private Component hoverName;
  private boolean amountChanged;

  public FluidStack(Fluid fluid, int amount) {
    this.fluid = fluid;
    this.amount = amount;
  }

  public static FluidStack of(Fluid fluid, int amount) {
    return new FluidStack(fluid, amount);
  }

  @NotNull
  public Fluid getFluid() {
    return this.fluid;
  }

  public int getAmount() {
    return this.amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
    this.amountChanged = true;
  }

  public boolean isEmpty() {
    return this.amount <= 0 || this.fluid == Fluids.EMPTY;
  }

  public boolean is(TagKey<Fluid> tag) {
    return this.fluid.builtInRegistryHolder().is(tag);
  }

  public void grow(int amount) {
    this.setAmount(this.amount + amount);
  }

  public void shrink(int amount) {
    this.setAmount(this.amount - amount);
  }

  @NotNull
  public Reference<Fluid> builtInRegistryHolder() {
    return this.fluid.builtInRegistryHolder();
  }

  @NotNull
  public DataComponentPatch getComponentsPatch() {
    return DataComponentPatch.EMPTY;
  }

  @NotNull
  public Component getHoverName() {
    if (this.hoverName == null) {
      this.hoverName =
          Component.literal(this.fluid.builtInRegistryHolder().key().identifier().toString());
    }

    return this.hoverName;
  }

  public boolean isAmountChanged() {
    return this.amountChanged;
  }

  public FluidStack copy() {
    return new FluidStack(this.fluid, this.amount);
  }

  public FluidStack copyWithAmount(int amount) {
    return new FluidStack(this.fluid, amount);
  }

  @Override
  public String toString() {
    return "FluidStack[" + this.fluid + " x" + this.amount + "]";
  }
}
