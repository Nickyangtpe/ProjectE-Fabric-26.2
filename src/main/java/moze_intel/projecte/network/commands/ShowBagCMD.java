package moze_intel.projecte.network.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import moze_intel.projecte.PEPermissions;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

public class ShowBagCMD {
  private static final SimpleCommandExceptionType NOT_FOUND =
      new SimpleCommandExceptionType(PELang.SHOWBAG_NOT_FOUND.translate());
  private static final DynamicCommandExceptionType INVALID_COLOR =
      new DynamicCommandExceptionType(value -> Component.literal("Unknown bag color: " + value));

  public static LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
    return (LiteralArgumentBuilder<CommandSourceStack>)
        ((LiteralArgumentBuilder)
                Commands.literal("showbag").requires(PEPermissions.COMMAND_SHOW_BAG))
            .then(
                ((RequiredArgumentBuilder)
                        Commands.argument("color", StringArgumentType.word())
                            .suggests(
                                (ctx, builder) -> {
                                  for (DyeColor color : DyeColor.values()) {
                                    builder.suggest(color.getName());
                                  }

                                  return builder.buildFuture();
                                })
                            .then(
                                Commands.argument("target", EntityArgument.player())
                                    .executes(
                                        ctx ->
                                            showBag(
                                                ctx,
                                                getColor(ctx),
                                                EntityArgument.getPlayer(ctx, "target")))))
                    .then(
                        Commands.argument("uuid", UuidArgument.uuid())
                            .executes(
                                ctx ->
                                    showBag(
                                        ctx, getColor(ctx), UuidArgument.getUuid(ctx, "uuid")))));
  }

  private static DyeColor getColor(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    String name = StringArgumentType.getString(ctx, "color").toLowerCase(Locale.ROOT);
    DyeColor color = DyeColor.byName(name, null);
    if (color == null) {
      throw INVALID_COLOR.create(name);
    } else {
      return color;
    }
  }

  private static int showBag(
      CommandContext<CommandSourceStack> ctx, DyeColor color, ServerPlayer player)
      throws CommandSyntaxException {
    ServerPlayer senderPlayer = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
    return showBag(senderPlayer, createContainer(senderPlayer, player, color));
  }

  private static int showBag(CommandContext<CommandSourceStack> ctx, DyeColor color, UUID uuid)
      throws CommandSyntaxException {
    ServerPlayer senderPlayer = ((CommandSourceStack) ctx.getSource()).getPlayerOrException();
    return showBag(
        senderPlayer,
        createContainer(
            ((CommandSourceStack) ctx.getSource()).getServer(), senderPlayer, uuid, color));
  }

  private static int showBag(ServerPlayer senderPlayer, MenuProvider container) {
    ContainerTypeDeferredRegister.openMenu(
        senderPlayer,
        container,
        b -> {
          b.writeBoolean(false);
          b.writeBoolean(false);
        });
    return 1;
  }

  private static MenuProvider createContainer(
      ServerPlayer sender, ServerPlayer target, DyeColor color) {
    IItemHandlerModifiable inv =
        (IItemHandlerModifiable)
            Objects.requireNonNull(PECapabilities.ALCH_BAG_CAPABILITY.find(target, null))
                .getBag(color);
    Component name = PELang.SHOWBAG_NAMED.translate(getBagName(color), target.getDisplayName());
    return getContainer(
        sender, name, inv, false, () -> target.isAlive() && !target.hasDisconnected());
  }

  private static MenuProvider createContainer(
      MinecraftServer server, ServerPlayer sender, UUID target, DyeColor color)
      throws CommandSyntaxException {
    IItemHandlerModifiable inv = loadOfflineBag(server, target, color);
    Component name = getBagName(color);
    Optional<NameAndId> profileByUUID = server.services().nameToIdCache().get(target);
    if (profileByUUID.isPresent()) {
      name = PELang.SHOWBAG_NAMED.translate(name, profileByUUID.get().name());
    }

    return getContainer(sender, name, inv, true, () -> true);
  }

  private static Component getBagName(DyeColor color) {
    AlchemicalBag bag = PEItems.getBag(color);
    return bag.getName(new ItemStack(bag));
  }

  private static MenuProvider getContainer(
      final ServerPlayer sender,
      final Component name,
      final IItemHandlerModifiable inv,
      final boolean immutable,
      final BooleanSupplier canInteractWith) {
    return new MenuProvider() {
      @NotNull
      public Component getDisplayName() {
        return name;
      }

      public AbstractContainerMenu createMenu(
          int windowId, @NotNull Inventory playerInv, @NotNull Player player) {
        return new AlchBagContainer(
            windowId, sender.getInventory(), InteractionHand.OFF_HAND, inv, 0, immutable) {
          @Override
          public boolean stillValid(@NotNull Player player) {
            return canInteractWith.getAsBoolean();
          }
        };
      }
    };
  }

  private static IItemHandlerModifiable loadOfflineBag(
      MinecraftServer server, UUID playerUUID, DyeColor color) throws CommandSyntaxException {
    Path player =
        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerUUID.toString() + ".dat");
    if (Files.exists(player) && Files.isRegularFile(player)) {
      try (InputStream in = Files.newInputStream(player)) {
        CompoundTag playerDat = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        CompoundTag attachmentData =
            (CompoundTag) playerDat.getCompound("fabric:attachments").orElse(null);
        if (attachmentData != null) {
          CompoundTag bagData =
              (CompoundTag)
                  attachmentData
                      .getCompound(PEAttachmentTypes.ALCHEMICAL_BAGS.identifier().toString())
                      .orElse(null);
          if (bagData != null) {
            RegistryOps<Tag> serializationContext =
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            DataResult<AlchBagImpl.AlchemicalBagAttachment> result =
                AlchBagImpl.AlchemicalBagAttachment.CODEC.parse(serializationContext, bagData);
            if (result.isSuccess()) {
              return ((AlchBagImpl.AlchemicalBagAttachment) result.getOrThrow()).getBag(color);
            }
          }
        }
      } catch (IOException var13) {
      }
    }

    throw NOT_FOUND.create();
  }
}
