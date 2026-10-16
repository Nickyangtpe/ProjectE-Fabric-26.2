package moze_intel.projecte.compat.distmarker;

public enum Dist {
  CLIENT,
  DEDICATED_SERVER;

  public boolean isClient() {
    return this == CLIENT;
  }

  public boolean isDedicatedServer() {
    return this == DEDICATED_SERVER;
  }
}
