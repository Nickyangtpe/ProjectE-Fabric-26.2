package moze_intel.projecte.gameObjs.registration;

import net.minecraft.resources.Identifier;

public interface INamedEntry {
  default String getName() {
    return this.getId().getPath();
  }

  Identifier getId();
}
