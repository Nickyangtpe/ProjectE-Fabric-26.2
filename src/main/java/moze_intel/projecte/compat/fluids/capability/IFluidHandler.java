package moze_intel.projecte.compat.fluids.capability;

import moze_intel.projecte.compat.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public interface IFluidHandler {
  int getTanks();

  @NotNull
  FluidStack getFluidInTank(int var1);

  int getTankCapacity(int var1);

  boolean isFluidValid(int var1, @NotNull FluidStack var2);

  int fill(@NotNull FluidStack var1, @NotNull IFluidHandler.FluidAction var2);

  @NotNull
  FluidStack drain(@NotNull FluidStack var1, @NotNull IFluidHandler.FluidAction var2);

  @NotNull
  FluidStack drain(int var1, @NotNull IFluidHandler.FluidAction var2);

  enum FluidAction {
    EXECUTE,
    SIMULATE;

    public boolean execute() {
      return this == EXECUTE;
    }

    public boolean simulate() {
      return this == SIMULATE;
    }
  }
}
