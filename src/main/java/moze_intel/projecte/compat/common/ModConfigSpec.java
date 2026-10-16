package moze_intel.projecte.compat.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import moze_intel.projecte.compat.fml.config.IConfigSpec;
import org.jetbrains.annotations.NotNull;

public final class ModConfigSpec implements IConfigSpec {
  private final Map<String, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
  private Path file;
  private boolean loaded;
  private boolean dirty;

  public synchronized Map<String, ModConfigSpec.ConfigValue<?>> getValues() {
    return Collections.unmodifiableMap(this.values);
  }

  public synchronized Path getFile() {
    return this.file;
  }

  public synchronized boolean isLoaded() {
    return this.loaded;
  }

  public synchronized void setFile(Path file) {
    this.file = Objects.requireNonNull(file, "file");
  }

  public synchronized void load() {
    if (this.file == null) {
      throw new IllegalStateException("Configuration has not been assigned a file");
    }

    this.load(this.file);
  }

  public synchronized void load(Path file) {
    this.setFile(file);
    boolean needsSave = !Files.isRegularFile(file);
    if (!needsSave) {
      try {
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
          String trimmed = line.trim();
          if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
            int equals = trimmed.indexOf(61);
            if (equals > 0) {
              String key = trimmed.substring(0, equals).trim();
              ModConfigSpec.ConfigValue<?> value = this.values.get(key);
              if (value != null && !value.read(trimmed.substring(equals + 1).trim())) {
                needsSave = true;
              }
            }
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read configuration " + file, e);
      }
    }

    this.loaded = true;
    this.dirty = false;
    if (needsSave) {
      this.save();
    }
  }

  public synchronized void save() {
    if (this.file == null) {
      throw new IllegalStateException("Configuration has not been assigned a file");
    }

    try {
      Path parent = this.file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      StringBuilder output =
          new StringBuilder(
              "# ProjectE configuration; edits take effect after the next reload.\n\n");

      for (Entry<String, ModConfigSpec.ConfigValue<?>> entry : this.values.entrySet()) {
        ModConfigSpec.ConfigValue<?> value = entry.getValue();

        for (String comment : value.comments()) {
          for (String line : comment.split("\\R", -1)) {
            output.append("# ").append(line).append('\n');
          }
        }

        output.append(entry.getKey()).append(" = ").append(value.format()).append("\n\n");
      }

      Path temporary = this.file.resolveSibling(this.file.getFileName() + ".tmp");
      Files.writeString(temporary, output.toString(), StandardCharsets.UTF_8);

      try {
        Files.move(
            temporary,
            this.file,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException atomicMoveUnsupported) {
        Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
      }

      this.dirty = false;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write configuration " + this.file, e);
    }
  }

  public synchronized boolean isDirty() {
    return this.dirty;
  }

  private <T> ModConfigSpec.ConfigValue<T> add(
      String key,
      T defaultValue,
      Function<String, T> decoder,
      Predicate<Object> validator,
      List<String> comments,
      String translation) {
    ModConfigSpec.ConfigValue<T> value =
        new ModConfigSpec.ConfigValue<>(
            defaultValue, decoder, validator, comments, translation, this);
    this.values.put(key, value);
    return value;
  }

  private void changed() {
    this.dirty = true;
  }

  private static String formatValue(Object value) {
    if (value instanceof String string) {
      return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    } else {
      return value instanceof List<?> list
          ? list.stream()
              .map(ModConfigSpec::formatValue)
              .reduce((left, right) -> left + ", " + right)
              .map(result -> "[" + result + "]")
              .orElse("[]")
          : String.valueOf(value);
    }
  }

  public static class BooleanValue extends ModConfigSpec.ConfigValue<Boolean> {
    private BooleanValue(
        boolean defaultValue,
        Function<String, Boolean> decoder,
        Predicate<Object> validator,
        List<String> comments,
        String translation,
        ModConfigSpec owner) {
      super(defaultValue, decoder, validator, comments, translation, owner);
    }

    protected BooleanValue(boolean defaultValue) {
      super(defaultValue);
    }
  }

  public static final class Builder {
    private final ModConfigSpec spec = new ModConfigSpec();
    private final List<String> path = new ArrayList<>();
    private final List<String> comments = new ArrayList<>();
    private String translation;

    private String key(String name) {
      if (name != null && !name.isBlank()) {
        return this.path.isEmpty() ? name : String.join(".", this.path) + "." + name;
      } else {
        throw new IllegalArgumentException("Configuration key must not be blank");
      }
    }

    public ModConfigSpec.Builder comment(String comment) {
      if (comment != null && !comment.isBlank()) {
        this.comments.add(comment);
      }

      return this;
    }

    public ModConfigSpec.Builder comment(String... comments) {
      if (comments != null) {
        for (String comment : comments) {
          this.comment(comment);
        }
      }

      return this;
    }

    public ModConfigSpec.Builder translation(String translation) {
      this.translation = translation;
      return this;
    }

    public ModConfigSpec.Builder push(String path) {
      if (path != null && !path.isBlank()) {
        this.path.add(path);
        return this;
      } else {
        throw new IllegalArgumentException("Configuration path must not be blank");
      }
    }

    public ModConfigSpec.Builder pop() {
      return this.pop(1);
    }

    public ModConfigSpec.Builder pop(int count) {
      for (int i = 0; i < count && !this.path.isEmpty(); i++) {
        this.path.remove(this.path.size() - 1);
      }

      return this;
    }

    public ModConfigSpec.Builder worldRestart() {
      return this;
    }

    private List<String> takeComments() {
      List<String> result = List.copyOf(this.comments);
      this.comments.clear();
      return result;
    }

    private String takeTranslation() {
      String result = this.translation;
      this.translation = null;
      return result;
    }

    public ModConfigSpec.BooleanValue define(String name, boolean defaultValue) {
      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      return (ModConfigSpec.BooleanValue)
          this.spec.values.computeIfAbsent(
              key,
              ignored ->
                  new ModConfigSpec.BooleanValue(
                      defaultValue,
                      ModConfigSpec.Builder::parseBoolean,
                      value -> value instanceof Boolean,
                      comments,
                      translation,
                      this.spec));
    }

    public ModConfigSpec.IntValue define(String name, int defaultValue) {
      return this.defineInRange(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public ModConfigSpec.IntValue defineInRange(String name, int defaultValue, int min, int max) {
      if (min > max) {
        throw new IllegalArgumentException("Invalid integer range");
      }

      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      Predicate<Object> validator =
          value -> value instanceof Integer integer && integer >= min && integer <= max;
      return (ModConfigSpec.IntValue)
          this.spec.values.computeIfAbsent(
              key,
              ignored ->
                  new ModConfigSpec.IntValue(
                      defaultValue,
                      ModConfigSpec.Builder::parseInteger,
                      validator,
                      comments,
                      translation,
                      this.spec));
    }

    public ModConfigSpec.LongValue defineInRange(
        String name, long defaultValue, long min, long max) {
      if (min > max) {
        throw new IllegalArgumentException("Invalid long range");
      }

      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      Predicate<Object> validator =
          value -> value instanceof Long number && number >= min && number <= max;
      return (ModConfigSpec.LongValue)
          this.spec.values.computeIfAbsent(
              key,
              ignored ->
                  new ModConfigSpec.LongValue(
                      defaultValue,
                      ModConfigSpec.Builder::parseLong,
                      validator,
                      comments,
                      translation,
                      this.spec));
    }

    public ModConfigSpec.DoubleValue defineInRange(
        String name, double defaultValue, double min, double max) {
      if (!(min <= max)) {
        throw new IllegalArgumentException("Invalid double range");
      }

      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      Predicate<Object> validator =
          value -> value instanceof Double number && number >= min && number <= max;
      return (ModConfigSpec.DoubleValue)
          this.spec.values.computeIfAbsent(
              key,
              ignored ->
                  new ModConfigSpec.DoubleValue(
                      defaultValue,
                      ModConfigSpec.Builder::parseDouble,
                      validator,
                      comments,
                      translation,
                      this.spec));
    }

    public ModConfigSpec.ConfigValue<String> define(String name, String defaultValue) {
      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      return this.spec.add(
          key,
          Objects.requireNonNull(defaultValue, "defaultValue"),
          ModConfigSpec.Builder::parseString,
          value -> value instanceof String,
          comments,
          translation);
    }

    public ModConfigSpec.ConfigValue<List<? extends String>> defineList(
        String name, List<? extends String> defaultValue, Predicate<Object> elementValidator) {
      Objects.requireNonNull(defaultValue, "defaultValue");
      Objects.requireNonNull(elementValidator, "elementValidator");
      String key = this.key(name);
      List<String> comments = this.takeComments();
      String translation = this.takeTranslation();
      List<String> copy = List.copyOf(defaultValue);
      Predicate<Object> validator =
          value -> value instanceof List<?> list && list.stream().allMatch(elementValidator);
      return this.spec.add(
          key,
          copy,
          serialized -> parseList(serialized, elementValidator),
          validator,
          comments,
          translation);
    }

    public ModConfigSpec.ConfigValue<List<? extends String>> defineListAllowEmpty(
        String name, List<? extends String> defaultValue, Predicate<Object> elementValidator) {
      return this.defineList(name, defaultValue, elementValidator);
    }

    public ModConfigSpec build() {
      if (!this.path.isEmpty()) {
        throw new IllegalStateException(
            "Unclosed configuration path: " + String.join(".", this.path));
      } else {
        return this.spec;
      }
    }

    private static Boolean parseBoolean(String value) {
      return switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "true" -> Boolean.TRUE;
        case "false" -> Boolean.FALSE;
        default -> throw new IllegalArgumentException("Not a boolean: " + value);
      };
    }

    private static Integer parseInteger(String value) {
      return Integer.valueOf(value.trim());
    }

    private static Long parseLong(String value) {
      return Long.valueOf(value.trim());
    }

    private static Double parseDouble(String value) {
      return Double.valueOf(value.trim());
    }

    private static String parseString(String value) {
      String trimmed = value.trim();
      return trimmed.length() >= 2
              && trimmed.charAt(0) == '"'
              && trimmed.charAt(trimmed.length() - 1) == '"'
          ? trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\")
          : trimmed;
    }

    private static List<String> parseList(String value, Predicate<Object> elementValidator) {
      String trimmed = value.trim();
      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        String contents = trimmed.substring(1, trimmed.length() - 1).trim();
        if (contents.isEmpty()) {
          return List.of();
        }

        List<String> result = new ArrayList<>();

        for (String item : contents.split(",")) {
          String parsed = parseString(item.trim());
          if (!elementValidator.test(parsed)) {
            throw new IllegalArgumentException("Invalid list element: " + parsed);
          }

          result.add(parsed);
        }

        return List.copyOf(result);
      } else {
        throw new IllegalArgumentException("Not a list: " + value);
      }
    }
  }

  public static class ConfigValue<T> {
    private final T defaultValue;
    private final Function<String, T> decoder;
    private final Predicate<Object> validator;
    private final List<String> comments;
    private final String translation;
    private final ModConfigSpec owner;
    private T value;

    protected ConfigValue(T defaultValue) {
      this(defaultValue, ignored -> defaultValue, ignored -> true, List.of(), null, null);
    }

    private ConfigValue(
        T defaultValue,
        Function<String, T> decoder,
        Predicate<Object> validator,
        List<String> comments,
        String translation,
        ModConfigSpec owner) {
      this.defaultValue = defaultValue;
      this.decoder = decoder;
      this.validator = validator;
      this.comments = List.copyOf(comments);
      this.translation = translation;
      this.owner = owner;
      this.value = defaultValue;
    }

    @NotNull
    public synchronized T get() {
      return this.value;
    }

    public synchronized void set(T value) {
      if (!this.validator.test(value)) {
        throw new IllegalArgumentException("Invalid configuration value: " + value);
      }

      this.value = Objects.requireNonNull(value, "value");
      if (this.owner != null) {
        this.owner.changed();
      }
    }

    @NotNull
    public T getDefault() {
      return this.defaultValue;
    }

    public List<String> comments() {
      return this.comments;
    }

    public String translation() {
      return this.translation;
    }

    private synchronized boolean read(String serialized) {
      try {
        T decoded = this.decoder.apply(serialized);
        if (decoded != null && this.validator.test(decoded)) {
          this.value = decoded;
          return true;
        } else {
          return false;
        }
      } catch (RuntimeException ignored) {
        this.value = this.defaultValue;
        return false;
      }
    }

    private synchronized String format() {
      return ModConfigSpec.formatValue(this.value);
    }
  }

  public static class DoubleValue extends ModConfigSpec.ConfigValue<Double> {
    private DoubleValue(
        double defaultValue,
        Function<String, Double> decoder,
        Predicate<Object> validator,
        List<String> comments,
        String translation,
        ModConfigSpec owner) {
      super(defaultValue, decoder, validator, comments, translation, owner);
    }

    protected DoubleValue(double defaultValue) {
      super(defaultValue);
    }
  }

  public static class IntValue extends ModConfigSpec.ConfigValue<Integer> {
    private IntValue(
        int defaultValue,
        Function<String, Integer> decoder,
        Predicate<Object> validator,
        List<String> comments,
        String translation,
        ModConfigSpec owner) {
      super(defaultValue, decoder, validator, comments, translation, owner);
    }

    protected IntValue(int defaultValue) {
      super(defaultValue);
    }
  }

  public static class LongValue extends ModConfigSpec.ConfigValue<Long> {
    private LongValue(
        long defaultValue,
        Function<String, Long> decoder,
        Predicate<Object> validator,
        List<String> comments,
        String translation,
        ModConfigSpec owner) {
      super(defaultValue, decoder, validator, comments, translation, owner);
    }

    protected LongValue(long defaultValue) {
      super(defaultValue);
    }
  }
}
