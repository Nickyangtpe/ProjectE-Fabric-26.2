package moze_intel.projecte.utils;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import moze_intel.projecte.PECore;
import moze_intel.projecte.compat.network.PacketDistributor;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ClientKeyHelper {
  private static final Category PROJECTE_CATEGORY = Category.register(PECore.rl("main"));
  private static ImmutableBiMap<PEKeybind, KeyMapping> peToMc = ImmutableBiMap.of();

  public static void registerKeyBindings() {
    Builder<PEKeybind, KeyMapping> builder = ImmutableBiMap.builder();
    addKeyBinding(builder, PEKeybind.HELMET_TOGGLE, true, 88);
    addKeyBinding(builder, PEKeybind.BOOTS_TOGGLE, false, 88);
    addKeyBinding(builder, PEKeybind.CHARGE, false, 86);
    addKeyBinding(builder, PEKeybind.EXTRA_FUNCTION, false, 67);
    addKeyBinding(builder, PEKeybind.FIRE_PROJECTILE, false, 82);
    addKeyBinding(builder, PEKeybind.MODE, false, 71);
    addKeyBinding(builder, PEKeybind.TRANSMUTATION_TABLET, false, 75);
    peToMc = builder.build();
  }

  private static void addKeyBinding(
      Builder<PEKeybind, KeyMapping> builder,
      PEKeybind keyBind,
      boolean requireShift,
      int keyCode) {
    KeyMapping keyMapping =
        KeyMappingHelper.registerKeyMapping(
            new ClientKeyHelper.PEKeyMapping(keyBind, requireShift, keyCode));
    builder.put(keyBind, keyMapping);
  }

  public static Component getKeyName(PEKeybind keybind) {
    KeyMapping keyMapping = (KeyMapping) peToMc.get(keybind);
    if (keyMapping == null) {
      return TextComponentUtil.build(keybind);
    }

    Component translatedKey = keyMapping.getTranslatedKeyMessage();
    return (Component)
        (keyMapping instanceof ClientKeyHelper.PEKeyMapping peKeyMapping
                && peKeyMapping.requireShift
            ? Component.translatable("key.projecte.shift_modifier", new Object[] {translatedKey})
            : translatedKey);
  }

  private static class PEKeyMapping extends KeyMapping {
    private final PEKeybind keybind;
    private final boolean requireShift;
    private boolean lastState;

    PEKeyMapping(PEKeybind keybind, boolean requireShift, int keyCode) {
      super(keybind.getTranslationKey(), keyCode, ClientKeyHelper.PROJECTE_CATEGORY);
      this.keybind = keybind;
      this.requireShift = requireShift;
    }

    public boolean isDown() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() != null) {
        return false;
      }

      long window = minecraft.getWindow().handle();
      boolean shiftDown = GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
      return shiftDown == this.requireShift && super.isDown();
    }

    public void setDown(boolean value) {
      super.setDown(value);
      boolean state = this.isDown();
      if (state != this.lastState) {
        if (state) {
          PacketDistributor.sendToServer(new KeyPressPKT(this.keybind));
        }

        this.lastState = state;
      }
    }
  }
}
