package moze_intel.projecte.compat.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import moze_intel.projecte.config.ProjectEConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConditionEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger("ProjectE/DataPackConditions");
  private static final Set<String> WARNED_CONDITIONS = ConcurrentHashMap.newKeySet();

  private ConditionEvaluator() {}

  public static boolean testAll(JsonElement conditions, String source) {
    if (conditions != null && !conditions.isJsonNull()) {
      if (conditions.isJsonArray()) {
        for (JsonElement condition : conditions.getAsJsonArray()) {
          if (!test(condition, source)) {
            return false;
          }
        }

        return true;
      } else {
        return test(conditions, source);
      }
    } else {
      return true;
    }
  }

  public static boolean test(JsonElement condition, String source) {
    if (condition != null && condition.isJsonObject()) {
      JsonObject object = condition.getAsJsonObject();
      JsonElement typeElement = object.get("type");
      if (typeElement != null && typeElement.isJsonPrimitive()) {
        String type;
        try {
          type = typeElement.getAsString();
        } catch (RuntimeException exception) {
          warn("<invalid-type>", source, "Condition type is not a string");
          return false;
        }
        return switch (type) {
          case "projecte:full_klein_stars" -> ProjectEConfig.common.fullKleinStars.get();
          case "projecte:tome_enabled" -> ProjectEConfig.common.craftableTome.get();
          case "neoforge:not", "forge:not" -> !test(object.get("value"), source);
          case "neoforge:and", "forge:and" -> testAll(groupValues(object), source);
          case "neoforge:or", "forge:or" -> testAny(groupValues(object), source);
          case "neoforge:mod_loaded", "forge:mod_loaded", "fabric:mod_loaded" ->
              isModLoaded(object, type, source);
          case "neoforge:item_exists", "forge:item_exists" -> itemExists(object, type, source);
          case "neoforge:true", "forge:true" -> true;
          case "neoforge:false", "forge:false" -> false;
          default -> {
            warn(
                type,
                source,
                "Condition type is not supported by the Fabric compatibility evaluator");
            yield false;
          }
        };
      } else {
        warn("<missing-type>", source, "Condition has no string type");
        return false;
      }
    } else {
      warn("<invalid>", source, "Condition is not a JSON object");
      return false;
    }
  }

  public static boolean shouldLoad(Identifier resourceId, Resource resource) {
    String path = resourceId.getPath();
    if (!path.startsWith("recipe/") && !path.startsWith("advancement/")) {
      return true;
    }

    try (BufferedReader reader = resource.openAsReader()) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) {
        return true;
      }

      JsonObject json = root.getAsJsonObject();
      JsonElement conditions = json.get("neoforge:conditions");
      if (conditions == null) {
        conditions = json.get("forge:conditions");
      }

      return testAll(conditions, resourceId.toString());
    } catch (IOException | RuntimeException exception) {
      LOGGER.warn(
          "Unable to evaluate data-pack conditions for {}; allowing vanilla to parse it",
          resourceId,
          exception);
      return true;
    }
  }

  private static JsonElement groupValues(JsonObject object) {
    JsonElement values = object.get("values");
    return values != null ? values : object.get("value");
  }

  private static boolean testAny(JsonElement values, String source) {
    if (values != null && values.isJsonArray()) {
      for (JsonElement value : values.getAsJsonArray()) {
        if (test(value, source)) {
          return true;
        }
      }

      return false;
    } else {
      warn("<invalid-group>", source, "OR condition requires a values array");
      return false;
    }
  }

  private static boolean isModLoaded(JsonObject object, String type, String source) {
    String modId = stringField(object, "modid");
    if (modId == null) {
      warn(type, source, "Mod-loaded condition has no modid");
      return false;
    } else {
      return FabricLoader.getInstance().isModLoaded(modId);
    }
  }

  private static boolean itemExists(JsonObject object, String type, String source) {
    String item = stringField(object, "item");
    Identifier id = item == null ? null : Identifier.tryParse(item);
    if (id == null) {
      warn(type, source, "Item-exists condition has no valid item id");
      return false;
    } else {
      return BuiltInRegistries.ITEM.getOptional(id).isPresent();
    }
  }

  private static String stringField(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element != null && element.isJsonPrimitive()) {
      try {
        return element.getAsString();
      } catch (RuntimeException exception) {
        return null;
      }
    } else {
      return null;
    }
  }

  private static void warn(String type, String source, String message) {
    String key = type + ":" + message;
    if (WARNED_CONDITIONS.add(key)) {
      LOGGER.warn(
          "{} while evaluating data-pack resource {}. The affected condition will fail closed.",
          message,
          source);
    }
  }
}
