package moze_intel.projecte.compat.capabilities;

import moze_intel.projecte.compat.fluids.capability.IFluidHandler;
import moze_intel.projecte.compat.fluids.capability.IFluidHandlerItem;
import moze_intel.projecte.compat.items.IItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class Capabilities {
  private Capabilities() {}

  private static Identifier rl(String path) {
    return Identifier.fromNamespaceAndPath("projecte", path);
  }

  public static final class FluidHandler {
    public static final BlockCapability<IFluidHandler, @Nullable Direction> BLOCK =
        BlockCapability.createSided(Capabilities.rl("fluid_handler"), IFluidHandler.class);
    public static final ItemCapability<IFluidHandlerItem, Void> ITEM =
        ItemCapability.createVoid(Capabilities.rl("fluid_handler"), IFluidHandlerItem.class);

    private FluidHandler() {}
  }

  public static final class ItemHandler {
    public static final BlockCapability<IItemHandler, @Nullable Direction> BLOCK =
        BlockCapability.createSided(Capabilities.rl("item_handler"), IItemHandler.class);
    public static final EntityCapability<IItemHandler, Void> ENTITY =
        EntityCapability.createVoid(Capabilities.rl("item_handler"), IItemHandler.class);
    public static final ItemCapability<IItemHandler, Void> ITEM =
        ItemCapability.createVoid(Capabilities.rl("item_handler"), IItemHandler.class);

    private ItemHandler() {}
  }
}
