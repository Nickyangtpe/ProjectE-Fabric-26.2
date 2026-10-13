package moze_intel.projecte.compat.fml.config;

import java.nio.file.Path;
import java.util.Objects;
import moze_intel.projecte.compat.common.ModConfigSpec;

public final class ModConfig {
  private final String modId;
  private final ModConfig.Type type;
  private final ModConfigSpec spec;
  private final Path file;

  public ModConfig(String modId, ModConfig.Type type, ModConfigSpec spec, Path file) {
    this.modId = Objects.requireNonNull(modId, "modId");
    this.type = Objects.requireNonNull(type, "type");
    this.spec = Objects.requireNonNull(spec, "spec");
    this.file = Objects.requireNonNull(file, "file");
  }

  public String getModId() {
    return this.modId;
  }

  public ModConfig.Type getType() {
    return this.type;
  }

  public ModConfigSpec getSpec() {
    return this.spec;
  }

  public Path getFile() {
    return this.file;
  }

  public void load() {
    this.spec.load(this.file);
  }

  public void save() {
    this.spec.save();
  }

  public enum Type {
    CLIENT,
    COMMON,
    SERVER;
  }
}
