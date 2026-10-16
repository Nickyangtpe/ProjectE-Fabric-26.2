package moze_intel.projecte.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import moze_intel.projecte.api.config.IConfigurableElement;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

public class AnnotationHelper {
  private static final Type MAPPER_TYPE = Type.getType(EMCMapper.class);
  private static final Type RECIPE_TYPE_MAPPER_TYPE = Type.getType(RecipeTypeMapper.class);
  private static final Type DATA_COMPONENT_PROCESSOR_TYPE =
      Type.getType(DataComponentProcessor.class);

  public static List<IDataComponentProcessor> getDataComponentProcessors() {
    List<IDataComponentProcessor> dataComponentProcessors = new ArrayList<>();
    Object2IntMap<IDataComponentProcessor> priorities = new Object2IntOpenHashMap<>();

    for (AnnotationHelper.DiscoveredAnnotation data :
        scanAnnotations(DATA_COMPONENT_PROCESSOR_TYPE)) {
      if (checkRequiredMods(data.annotationData())) {
        IDataComponentProcessor processor = getDataComponentProcessor(data.memberName());
        if (processor != null) {
          int priority = getPriority(data.annotationData());
          dataComponentProcessors.add(processor);
          priorities.put(processor, priority);
          PECore.debugLog(
              "Found and loaded Data Component Processor: {}, with priority {}",
              processor.getName(),
              priority);
        }
      }
    }

    dataComponentProcessors.sort(Comparator.comparingInt(priorities::getInt).reversed());
    return dataComponentProcessors;
  }

  public static List<IRecipeTypeMapper> getRecipeTypeMappers() {
    List<IRecipeTypeMapper> recipeTypeMappers = new ArrayList<>();
    Object2IntMap<IRecipeTypeMapper> priorities = new Object2IntOpenHashMap<>();

    for (AnnotationHelper.DiscoveredAnnotation data : scanAnnotations(RECIPE_TYPE_MAPPER_TYPE)) {
      if (checkRequiredMods(data.annotationData())) {
        IRecipeTypeMapper mapper = getRecipeTypeMapper(data.memberName());
        if (mapper != null) {
          int priority = getPriority(data.annotationData());
          recipeTypeMappers.add(mapper);
          priorities.put(mapper, priority);
          PECore.debugLog(
              "Found and loaded RecipeType Mapper: {}, with priority {}",
              mapper.getName(),
              priority);
        }
      }
    }

    recipeTypeMappers.sort(Comparator.comparingInt(priorities::getInt).reversed());
    return recipeTypeMappers;
  }

  public static List<IEMCMapper<NormalizedSimpleStack, Long>> getEMCMappers() {
    List<IEMCMapper<NormalizedSimpleStack, Long>> emcMappers = new ArrayList<>();
    Object2IntMap<IEMCMapper<NormalizedSimpleStack, Long>> priorities = new Object2IntOpenHashMap<>();

    for (AnnotationHelper.DiscoveredAnnotation data : scanAnnotations(MAPPER_TYPE)) {
      if (checkRequiredMods(data.annotationData())) {
        IEMCMapper<?, ?> mapper = getEMCMapper(data.memberName());
        if (mapper != null) {
          try {
            IEMCMapper<NormalizedSimpleStack, Long> emcMapper =
                (IEMCMapper<NormalizedSimpleStack, Long>) mapper;
            int priority = getPriority(data.annotationData());
            emcMappers.add(emcMapper);
            priorities.put(emcMapper, priority);
            PECore.debugLog(
                "Found and loaded EMC mapper: {}, with priority {}", mapper.getName(), priority);
          } catch (ClassCastException e) {
            PECore.LOGGER.error(
                "{}: Is not a mapper for {}, to {}",
                new Object[] {mapper.getClass(), NormalizedSimpleStack.class, Long.class, e});
          }
        }
      }
    }

    emcMappers.sort(Comparator.comparingInt(priorities::getInt).reversed());
    return emcMappers;
  }

  private static List<AnnotationHelper.DiscoveredAnnotation> scanAnnotations(Type annotationType) {
    String descriptor = annotationType.getDescriptor();
    List<AnnotationHelper.DiscoveredAnnotation> discovered = new ArrayList<>();
    Set<String> seenClasses = new HashSet<>();

    for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
      List<Path> roots = new ArrayList<>(mod.getRootPaths());
      if ("projecte".equals(mod.getMetadata().getId())) {
        try {
          CodeSource codeSource = PECore.class.getProtectionDomain().getCodeSource();
          if (codeSource != null) {
            Path codeRoot = Path.of(codeSource.getLocation().toURI());
            if (Files.isDirectory(codeRoot)
                && Files.isRegularFile(codeRoot.resolve("moze_intel/projecte/PECore.class"))
                && !roots.contains(codeRoot)) {
              roots.add(codeRoot);
              PECore.debugLog(
                  "Added ProjectE development code root {} to annotation scan", codeRoot);
            }
          }
        } catch (Exception e) {
          PECore.LOGGER.warn(
              "Unable to resolve ProjectE's development class root for annotation scanning", e);
        }
      }

      for (Path root : roots) {
        try (Stream<Path> paths = Files.walk(root)) {
          paths
              .filter(x$0 -> Files.isRegularFile(x$0))
              .filter(path -> path.getFileName().toString().endsWith(".class"))
              .forEach(path -> scanClass(path, descriptor, seenClasses, discovered));
        } catch (IOException | RuntimeException e) {
          PECore.LOGGER.warn("Failed to scan classes under mod root {}", root, e);
        }
      }
    }

    discovered.sort(Comparator.comparing(AnnotationHelper.DiscoveredAnnotation::memberName));
    return discovered;
  }

  private static void scanClass(
      Path classFile,
      String descriptor,
      Set<String> seenClasses,
      List<AnnotationHelper.DiscoveredAnnotation> discovered) {
    try (InputStream input = Files.newInputStream(classFile)) {
      AnnotationHelper.AnnotationScanner scanner =
          new AnnotationHelper.AnnotationScanner(descriptor);
      new ClassReader(input).accept(scanner, 7);
      if (scanner.className != null
          && scanner.annotationData != null
          && seenClasses.add(scanner.className)) {
        discovered.add(
            new AnnotationHelper.DiscoveredAnnotation(
                scanner.className, Map.copyOf(scanner.annotationData)));
      }
    } catch (IOException | RuntimeException e) {
      PECore.debugLog("Could not inspect class metadata in {}", classFile);
    }
  }

  @Nullable
  private static IEMCMapper<?, ?> getEMCMapper(String className) {
    return createOrGetInstance(
        className, IEMCMapper.class, EMCMapper.Instance.class, IConfigurableElement::getName);
  }

  @Nullable
  private static IRecipeTypeMapper getRecipeTypeMapper(String className) {
    return createOrGetInstance(
        className,
        IRecipeTypeMapper.class,
        RecipeTypeMapper.Instance.class,
        IConfigurableElement::getName);
  }

  @Nullable
  private static IDataComponentProcessor getDataComponentProcessor(String className) {
    return createOrGetInstance(
        className,
        IDataComponentProcessor.class,
        DataComponentProcessor.Instance.class,
        IConfigurableElement::getName);
  }

  @Nullable
  private static <T> T createOrGetInstance(
      String className,
      Class<T> baseClass,
      Class<? extends Annotation> instanceAnnotation,
      Function<T, String> nameFunction) {
    try {
      Class<? extends T> subClass = Class.forName(className).asSubclass(baseClass);
      Field[] fields = subClass.getDeclaredFields();

      for (Field field : fields) {
        if (field.isAnnotationPresent(instanceAnnotation)) {
          if (Modifier.isStatic(field.getModifiers())) {
            try {
              Object fieldValue = field.get(null);
              if (baseClass.isInstance(fieldValue)) {
                T instance = baseClass.cast(fieldValue);
                PECore.debugLog(
                    "Found specified {} instance for: {}. Using it rather than creating a new"
                        + " instance.",
                    baseClass.getSimpleName(),
                    nameFunction.apply(instance));
                return instance;
              }

              PECore.LOGGER.error(
                  "{} annotation found on non {} field: {}",
                  new Object[] {
                    instanceAnnotation.getSimpleName(), baseClass.getSimpleName(), field
                  });
              return null;
            } catch (IllegalAccessException e) {
              PECore.LOGGER.error(
                  "{} annotation found on inaccessible field: {}",
                  new Object[] {instanceAnnotation.getSimpleName(), field, e});
              return null;
            }
          }

          PECore.LOGGER.error(
              "{} annotation found on non static field: {}",
              instanceAnnotation.getSimpleName(),
              field);
          return null;
        }
      }

      return (T) subClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | LinkageError
        | InvocationTargetException
        | NoSuchMethodException e) {
      PECore.LOGGER.error("Failed to load: {}", className, e);
      return null;
    }
  }

  private static boolean checkRequiredMods(Map<String, Object> annotationData) {
    Object rawRequiredMods = annotationData.get("requiredMods");
    if (rawRequiredMods == null) {
      return true;
    }

    List<String> requiredMods = new ArrayList<>();
    if (rawRequiredMods instanceof List) {
      for (Object value : (List) rawRequiredMods) {
        if (value instanceof String modId && !modId.isBlank()) {
          requiredMods.add(modId);
        }
      }
    } else if (rawRequiredMods instanceof String modId && !modId.isBlank()) {
      requiredMods.add(modId);
    } else if (rawRequiredMods instanceof String[] modIds) {
      requiredMods.addAll(Arrays.asList(modIds));
    }

    if (requiredMods.stream().anyMatch(modIdx -> !FabricLoader.getInstance().isModLoaded(modIdx))) {
      PECore.debugLog(
          "Skipped checking class with missing required mods {}",
          Arrays.toString(requiredMods.toArray()));
      return false;
    } else {
      return true;
    }
  }

  private static int getPriority(Map<String, Object> annotationData) {
    return annotationData.get("priority") instanceof Number number ? number.intValue() : 0;
  }

  private static final class AnnotationScanner extends ClassVisitor {
    private final String descriptor;
    @Nullable private String className;
    @Nullable private Map<String, Object> annotationData;

    private AnnotationScanner(String descriptor) {
      super(589824);
      this.descriptor = descriptor;
    }

    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.className = name.replace('/', '.');
    }

    public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
      if (!this.descriptor.equals(annotationDescriptor)) {
        return null;
      }

      final Map<String, Object> values = new HashMap<>();
      this.annotationData = values;
      return new AnnotationVisitor(589824) {
        public void visit(String name, Object value) {
          values.put(name, value);
        }

        public AnnotationVisitor visitArray(String name) {
          final List<Object> arrayValues = new ArrayList<>();
          values.put(name, arrayValues);
          return new AnnotationVisitor(589824) {
            public void visit(String ignored, Object value) {
              arrayValues.add(value);
            }
          };
        }
      };
    }
  }

  private record DiscoveredAnnotation(String memberName, Map<String, Object> annotationData) {}
}
