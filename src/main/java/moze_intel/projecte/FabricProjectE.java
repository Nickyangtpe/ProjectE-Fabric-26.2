package moze_intel.projecte;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricProjectE implements ModInitializer {
  public static final String MOD_ID = "projecte";
  public static final Logger LOGGER = LoggerFactory.getLogger("projecte");

  public void onInitialize() {
    PECore.init();
  }
}
