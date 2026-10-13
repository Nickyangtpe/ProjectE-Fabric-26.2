package moze_intel.projecte.compat.conditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ConditionalResourceManager implements ResourceManager {
  private final ResourceManager delegate;

  public ConditionalResourceManager(ResourceManager delegate) {
    this.delegate = delegate;
  }

  public Optional<Resource> getResource(Identifier location) {
    Optional<Resource> resource = this.delegate.getResource(location);
    return resource.filter(value -> ConditionEvaluator.shouldLoad(location, value));
  }

  public Set<String> getNamespaces() {
    return this.delegate.getNamespaces();
  }

  public List<Resource> getResourceStack(Identifier location) {
    List<Resource> resources = this.delegate.getResourceStack(location);
    return !isConditionalPath(location)
        ? resources
        : resources.stream()
            .filter(resource -> ConditionEvaluator.shouldLoad(location, resource))
            .toList();
  }

  public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
    Map<Identifier, Resource> resources = this.delegate.listResources(directory, filter);
    if (!isConditionalDirectory(directory)) {
      return resources;
    }

    Map<Identifier, Resource> result = new LinkedHashMap<>();

    for (Entry<Identifier, Resource> entry : resources.entrySet()) {
      if (ConditionEvaluator.shouldLoad(entry.getKey(), entry.getValue())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }

    return result;
  }

  public Map<Identifier, List<Resource>> listResourceStacks(
      String directory, Predicate<Identifier> filter) {
    Map<Identifier, List<Resource>> resources = this.delegate.listResourceStacks(directory, filter);
    if (!isConditionalDirectory(directory)) {
      return resources;
    }

    Map<Identifier, List<Resource>> result = new LinkedHashMap<>();

    for (Entry<Identifier, List<Resource>> entry : resources.entrySet()) {
      List<Resource> accepted = new ArrayList<>();

      for (Resource resource : entry.getValue()) {
        if (ConditionEvaluator.shouldLoad(entry.getKey(), resource)) {
          accepted.add(resource);
        }
      }

      if (!accepted.isEmpty()) {
        result.put(entry.getKey(), List.copyOf(accepted));
      }
    }

    return result;
  }

  public Stream<PackResources> listPacks() {
    return this.delegate.listPacks();
  }

  private static boolean isConditionalDirectory(String directory) {
    return directory.equals("recipe") || directory.equals("advancement");
  }

  private static boolean isConditionalPath(Identifier location) {
    String path = location.getPath();
    return path.startsWith("recipe/") || path.startsWith("advancement/");
  }
}
