package moze_intel.projecte.api.data;

import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import moze_intel.projecte.compat.annotation.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class BaseFileBuilder<BUILDER extends BaseFileBuilder<BUILDER>> {
  @Nullable protected String comment;
  private final String builderType;

  protected BaseFileBuilder(String builderType) {
    this.builderType = builderType;
  }

  private BUILDER self() {
    return (BUILDER) this;
  }

  public BUILDER comment(String comment) {
    Objects.requireNonNull(comment, "Comment defaults to null, remove unnecessary call.");
    if (this.comment != null) {
      throw new RuntimeException(this.builderType + " Builder already has a comment declared.");
    }

    this.comment = comment;
    return this.self();
  }

  public boolean hasComment() {
    return this.comment != null;
  }
}
