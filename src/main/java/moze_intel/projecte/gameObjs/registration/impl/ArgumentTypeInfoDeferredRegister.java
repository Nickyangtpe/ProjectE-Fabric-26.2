package moze_intel.projecte.gameObjs.registration.impl;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.mixin.ArgumentTypeInfosAccessor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfo.Template;
import net.minecraft.core.registries.Registries;

public class ArgumentTypeInfoDeferredRegister extends PEDeferredRegister<ArgumentTypeInfo<?, ?>> {
  public ArgumentTypeInfoDeferredRegister(String modid) {
    super(Registries.COMMAND_ARGUMENT_TYPE, modid, ArgumentTypeInfoRegistryObject::new);
  }

  public <TYPE extends ArgumentType<?>> ArgumentTypeInfoRegistryObject<TYPE> registerContextFree(
      String name, Class<TYPE> argumentClass, Supplier<TYPE> constructor) {
    return this.register(name, argumentClass, () -> SingletonArgumentInfo.contextFree(constructor));
  }

  public <TYPE extends ArgumentType<?>> ArgumentTypeInfoRegistryObject<TYPE> registerContextAware(
      String name, Class<TYPE> argumentClass, Function<CommandBuildContext, TYPE> constructor) {
    return this.register(
        name, argumentClass, () -> SingletonArgumentInfo.contextAware(constructor));
  }

  public <TYPE extends ArgumentType<?>> ArgumentTypeInfoRegistryObject<TYPE> register(
      String name,
      Class<TYPE> argumentClass,
      Supplier<ArgumentTypeInfo<TYPE, ? extends Template<TYPE>>> sup) {
    return (ArgumentTypeInfoRegistryObject<TYPE>)
        super.<ArgumentTypeInfo<TYPE, ? extends Template<TYPE>>>register(
            name,
            () -> {
              ArgumentTypeInfo<TYPE, ? extends Template<TYPE>> info = sup.get();
              ArgumentTypeInfosAccessor.projecte$getByClass().put(argumentClass, info);
              return info;
            });
  }
}
