package moze_intel.projecte.compat.server;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;

public final class ReloadableServerResourcesAccess {
  private static final String HOLDER_CLASS =
      "net.minecraft.server.MinecraftServer$ReloadableResources";
  private static final Field RESOURCES_FIELD;
  private static final Method MANAGERS_METHOD;

  private ReloadableServerResourcesAccess() {}

  public static ReloadableServerResources getManagers(MinecraftServer server) {
    try {
      Object holder = RESOURCES_FIELD.get(server);
      if (holder == null) {
        throw new IllegalStateException("Minecraft server resources are not initialized yet");
      } else {
        return ReloadableServerResources.class.cast(MANAGERS_METHOD.invoke(holder));
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Unable to read active Minecraft server resource managers", e);
    }
  }

  static {
    try {
      Field resourcesField = MinecraftServer.class.getDeclaredField("resources");
      if (!Modifier.isStatic(resourcesField.getModifiers())
          && "net.minecraft.server.MinecraftServer$ReloadableResources"
              .equals(resourcesField.getType().getName())) {
        Method managersMethod = resourcesField.getType().getDeclaredMethod("managers");
        if (!ReloadableServerResources.class.equals(managersMethod.getReturnType())) {
          throw new NoSuchMethodException(
              "MinecraftServer.ReloadableResources.managers() has unexpected return type "
                  + managersMethod.getReturnType().getName());
        }

        resourcesField.setAccessible(true);
        managersMethod.setAccessible(true);
        RESOURCES_FIELD = resourcesField;
        MANAGERS_METHOD = managersMethod;
      } else {
        throw new NoSuchFieldException(
            "MinecraftServer.resources has unexpected type " + resourcesField.getType().getName());
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new ExceptionInInitializerError(
          new IllegalStateException(
              "Unable to access the verified Minecraft 26.2 active server resource holder", e));
    }
  }
}
