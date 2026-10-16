package moze_intel.projecte.gameObjs.items;

import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;

@FunctionalInterface
public interface ICapabilityAware {
  void attachCapabilities(RegisterCapabilitiesEvent var1);
}
