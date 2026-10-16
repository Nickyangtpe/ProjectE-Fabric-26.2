package moze_intel.projecte.api.mapper.recipe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecipeTypeMapper {
  int priority() default 0;

  String[] requiredMods() default {""};

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Instance {}
}
