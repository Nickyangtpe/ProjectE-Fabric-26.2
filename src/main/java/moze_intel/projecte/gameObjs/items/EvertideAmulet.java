package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.capabilities.RegisterCapabilitiesEvent;
import moze_intel.projecte.compat.fluids.FluidStack;
import moze_intel.projecte.compat.fluids.capability.IFluidHandler;
import moze_intel.projecte.compat.fluids.capability.IFluidHandlerItem;
import moze_intel.projecte.compat.util.PESidedSuccess;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityWaterProjectile;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.WeatherData;
import org.jetbrains.annotations.NotNull;

public class EvertideAmulet extends ItemPE
    implements IProjectileShooter, IPedestalItem, ICapabilityAware, ISelfCraftingRemainder {
  public EvertideAmulet(Properties props) {
    super(props);
  }

  @NotNull
  public InteractionResult useOn(UseOnContext ctx) {
    Player player = ctx.getPlayer();
    if (player == null) {
      return InteractionResult.FAIL;
    }

    Level level = ctx.getLevel();
    BlockPos pos = ctx.getClickedPos();
    if (!level.isClientSide() && PlayerHelper.hasEditPermission(player, level, pos)) {
      Direction sideHit = ctx.getClickedFace();
      IFluidHandler fluidHandler =
          WorldHelper.getCapability(level, Capabilities.FluidHandler.BLOCK, pos, sideHit);
      if (fluidHandler != null) {
        fluidHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        return InteractionResult.CONSUME;
      }

      WorldHelper.placeFluid(
          player, level, pos, sideHit, Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
      level.playSound(
          null,
          player.getX(),
          player.getY(),
          player.getZ(),
          PESoundEvents.WATER_MAGIC.get(),
          SoundSource.PLAYERS,
          1.0F,
          1.0F);
    }

    return PESidedSuccess.sidedSuccess(level.isClientSide());
  }

  @Override
  public boolean shootProjectile(
      @NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
    Level level = player.level();
    if (!ProjectEConfig.server.items.opEvertide.get() && level.dimension() == Level.NETHER) {
      return false;
    }

    level.playSound(
        null,
        player.getX(),
        player.getY(),
        player.getZ(),
        PESoundEvents.WATER_MAGIC.get(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
    EntityWaterProjectile ent = new EntityWaterProjectile(player, level);
    ent.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
    level.addFreshEntity(ent);
    return true;
  }

  public void appendHoverText(
      @NotNull ItemStack stack,
      @NotNull TooltipContext context,
      @NotNull TooltipDisplay display,
      @NotNull Consumer<Component> tooltip,
      @NotNull TooltipFlag flags) {
    super.appendHoverText(stack, context, display, tooltip, flags);
    tooltip.accept(
        PELang.TOOLTIP_EVERTIDE_1.translate(ClientKeyHelper.getKeyName(PEKeybind.FIRE_PROJECTILE)));
    tooltip.accept(PELang.TOOLTIP_EVERTIDE_2.translate());
    tooltip.accept(PELang.TOOLTIP_EVERTIDE_3.translate());
    tooltip.accept(PELang.TOOLTIP_EVERTIDE_4.translate());
  }

  @Override
  public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
      @NotNull ItemStack stack,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull PEDESTAL pedestal) {
    if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.evertide.get() != -1) {
      if (pedestal.getActivityCooldown() == 0) {
        if (level instanceof ServerLevel serverLevel) {
          int i = (300 + level.getRandom().nextInt(600)) * 20;
          WeatherData weather = serverLevel.getWeatherData();
          weather.setRainTime(i);
          weather.setThunderTime(i);
          weather.setRaining(true);
        }

        pedestal.setActivityCooldown(
            level, pos, ProjectEConfig.server.cooldown.pedestal.evertide.get());
      } else {
        pedestal.decrementActivityCooldown(level, pos);
      }
    }

    return false;
  }

  @NotNull
  @Override
  public List<Component> getPedestalDescription(float tickRate) {
    List<Component> list = new ArrayList<>();
    if (ProjectEConfig.server.cooldown.pedestal.evertide.get() != -1) {
      list.add(PELang.PEDESTAL_EVERTIDE_1.translateColored(ChatFormatting.BLUE));
      list.add(
          PELang.PEDESTAL_EVERTIDE_2.translateColored(
              ChatFormatting.BLUE,
              MathUtils.tickToSecFormatted(
                  ProjectEConfig.server.cooldown.pedestal.evertide.get(), tickRate)));
    }

    return list;
  }

  @Override
  public void attachCapabilities(RegisterCapabilitiesEvent event) {
    event.registerItem(
        Capabilities.FluidHandler.ITEM,
        (stack, context) -> new EvertideAmulet.InfiniteFluidHandler(stack),
        this);
    IntegrationHelper.registerCuriosCapability(event, this);
  }

  private record InfiniteFluidHandler(ItemStack stack) implements IFluidHandlerItem {
    @NotNull
    @Override
    public ItemStack getContainer() {
      return this.stack;
    }

    @Override
    public int getTanks() {
      return 1;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
      return tank == 0 ? new FluidStack(Fluids.WATER, Integer.MAX_VALUE) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
      return tank == 0 ? Integer.MAX_VALUE : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
      return this.isWater(stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull IFluidHandler.FluidAction action) {
      return this.isWater(resource) ? resource.getAmount() : 0;
    }

    @NotNull
    @Override
    public FluidStack drain(
        @NotNull FluidStack resource, @NotNull IFluidHandler.FluidAction action) {
      return this.isWater(resource) ? resource : FluidStack.EMPTY;
    }

    private boolean isWater(FluidStack stack) {
      return stack.is(FluidTags.WATER);
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, @NotNull IFluidHandler.FluidAction action) {
      return new FluidStack(Fluids.WATER, maxDrain);
    }
  }
}
