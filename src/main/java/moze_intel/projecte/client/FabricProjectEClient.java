package moze_intel.projecte.client;

import net.fabricmc.api.ClientModInitializer;

public class FabricProjectEClient implements ClientModInitializer {
  public void onInitializeClient() {
    PEClient.initialize();
  }
}
