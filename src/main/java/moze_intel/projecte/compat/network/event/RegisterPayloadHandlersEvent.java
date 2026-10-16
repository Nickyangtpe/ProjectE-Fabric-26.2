package moze_intel.projecte.compat.network.event;

import moze_intel.projecte.compat.eventbus.Event;
import moze_intel.projecte.compat.network.registration.PayloadRegistrar;

public class RegisterPayloadHandlersEvent extends Event {
  private static final String DEFAULT_VERSION = "1";

  public PayloadRegistrar registrar(String version) {
    return new PayloadRegistrar(version);
  }

  public PayloadRegistrar registrar() {
    return this.registrar("1");
  }
}
