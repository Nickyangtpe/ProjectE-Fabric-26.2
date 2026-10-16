package moze_intel.projecte.compat.fml.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;

public final class FMLPaths {
  public static final FMLPaths.PathValue CONFIGDIR =
      new FMLPaths.PathValue(FabricLoader.getInstance().getConfigDir());

  private FMLPaths() {}

  public static Path getOrCreateGameRelativePath(Path path) {
    Objects.requireNonNull(path, "path");

    try {
      Files.createDirectories(path);
      return path;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to create ProjectE configuration directory " + path, e);
    }
  }

  public static final class PathValue {
    private final Path path;

    private PathValue(Path path) {
      this.path = Objects.requireNonNull(path, "path");
    }

    public Path get() {
      return this.path;
    }
  }
}
