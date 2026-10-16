package moze_intel.projecte.compat.client.gui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class FabricMenuScreenRegistry {
  private static final Method MENU_SCREEN_REGISTER = findMenuScreenRegisterMethod();

  private FabricMenuScreenRegistry() {}

  public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
      MenuType<? extends M> menuType, FabricMenuScreenRegistry.Factory<M, S> factory) {
    Class<?> factoryInterface = MENU_SCREEN_REGISTER.getParameterTypes()[1];
    Object adapter =
        Proxy.newProxyInstance(
            MenuScreens.class.getClassLoader(),
            new Class[] {factoryInterface},
            (proxy, method, arguments) -> {
              if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
              }

              if (arguments != null
                  && arguments.length == 3
                  && Screen.class.isAssignableFrom(method.getReturnType())) {
                M menu = (M) arguments[0];
                return factory.create(menu, (Inventory) arguments[1], (Component) arguments[2]);
              }

              if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                  case "toString" -> "ProjectE menu screen factory for " + menuType;
                  case "hashCode" -> System.identityHashCode(proxy);
                  case "equals" -> proxy == arguments[0];
                  default -> throw new UnsupportedOperationException(method.toString());
                };
              } else {
                throw new IllegalStateException(
                    "Unexpected vanilla menu screen factory method: " + method);
              }
            });

    try {
      MENU_SCREEN_REGISTER.invoke(null, menuType, adapter);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to register ProjectE menu screen for " + menuType, e);
    }
  }

  private static Method findMenuScreenRegisterMethod() {
    for (Method method : MenuScreens.class.getDeclaredMethods()) {
      Class<?>[] parameters = method.getParameterTypes();
      if (Modifier.isStatic(method.getModifiers())
          && parameters.length == 2
          && parameters[0] == MenuType.class
          && parameters[1].isInterface()) {
        method.setAccessible(true);
        return method;
      }
    }

    throw new IllegalStateException(
        "Could not locate the vanilla MenuScreens handled-screen registration method for projecte");
  }

  @FunctionalInterface
  public interface Factory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M var1, Inventory var2, Component var3);
  }
}
