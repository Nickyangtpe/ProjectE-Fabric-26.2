package moze_intel.projecte.api.mapper.recipe;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Set;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;

public interface INSSFakeGroupManager {
  default INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroup(
      Set<NormalizedSimpleStack> stacks) {
    Object2IntMap<NormalizedSimpleStack> map = new Object2IntOpenHashMap<NormalizedSimpleStack>(stacks.size());

    for (NormalizedSimpleStack stack : stacks) {
      map.put(stack, 1);
    }

    return this.getOrCreateFakeGroupDirect(map, true);
  }

  default INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroup(
      Object2IntMap<NormalizedSimpleStack> stacks, boolean representsIngredient) {
    return this.getOrCreateFakeGroup(stacks, representsIngredient, false);
  }

  INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroup(
      Object2IntMap<NormalizedSimpleStack> var1, boolean var2, boolean var3);

  default INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroupDirect(
      Object2IntMap<NormalizedSimpleStack> stacks, boolean representsIngredient) {
    return this.getOrCreateFakeGroupDirect(stacks, representsIngredient, false);
  }

  INSSFakeGroupManager.FakeGroupData getOrCreateFakeGroupDirect(
      Object2IntMap<NormalizedSimpleStack> var1, boolean var2, boolean var3);

  record FakeGroupData(NormalizedSimpleStack dummy, boolean created) {}
}
