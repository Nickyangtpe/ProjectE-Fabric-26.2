package moze_intel.projecte.gameObjs.customRecipes;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.registries.PERecipeSerializers;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipesCovalenceRepair extends CustomRecipe {
  @Nullable
  private RecipesCovalenceRepair.RepairTargetInfo findIngredients(CraftingInput inv) {
    List<ItemStack> dust = new ArrayList<>();
    ItemStack tool = ItemStack.EMPTY;

    for (ItemStack input : inv.items()) {
      if (!input.isEmpty()) {
        if (!input.is(PETags.Items.COVALENCE_DUST)) {
          if (!tool.isEmpty() || !ItemHelper.isRepairableDamagedItem(input)) {
            return null;
          }

          tool = input;
        } else {
          dust.add(input);
        }
      }
    }

    return !tool.isEmpty() && !dust.isEmpty()
        ? new RecipesCovalenceRepair.RepairTargetInfo(
            tool, dust.stream().mapToLong(IEMCProxy.INSTANCE::getValue).sum())
        : null;
  }

  public boolean matches(@NotNull CraftingInput inv, @NotNull Level level) {
    RecipesCovalenceRepair.RepairTargetInfo targetInfo = this.findIngredients(inv);
    return targetInfo != null && targetInfo.emcPerDurability <= targetInfo.dustEmc;
  }

  @NotNull
  public ItemStack assemble(@NotNull CraftingInput inv) {
    RecipesCovalenceRepair.RepairTargetInfo targetInfo = this.findIngredients(inv);
    if (targetInfo == null) {
      return ItemStack.EMPTY;
    }

    ItemStack output = targetInfo.tool.copy();
    output.setDamageValue(
        (int)
            Math.max(
                output.getDamageValue() - targetInfo.dustEmc / targetInfo.emcPerDurability, 0L));
    return output;
  }

  @NotNull
  public RecipeSerializer<? extends RecipesCovalenceRepair> getSerializer() {
    return PERecipeSerializers.COVALENCE_REPAIR.get();
  }

  private static class RepairTargetInfo {
    private final ItemStack tool;
    private final long emcPerDurability;
    private final long dustEmc;

    public RepairTargetInfo(ItemStack tool, long dustEmc) {
      this.tool = tool;
      this.dustEmc = dustEmc;
      this.emcPerDurability = EMCHelper.getEMCPerDurability(tool);
    }
  }
}
