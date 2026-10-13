package moze_intel.projecte.events;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.compat.items.ItemHandlerHelper;
import moze_intel.projecte.compat.util.FakePlayer;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import moze_intel.projecte.gameObjs.items.armor.PEArmor;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEAttributes;
import moze_intel.projecte.impl.TransmutationOffline;
import moze_intel.projecte.mixin.ItemEntityAccessor;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerEvents {
  private static final Set<LivingEntity> REPLACED_DAMAGE =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private static final EquipmentSlot[] ARMOR_SLOTS =
      new EquipmentSlot[] {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
      };

  private PlayerEvents() {}

  public static void respawnEvent(ServerPlayer player) {
    IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (knowledge != null) {
      knowledge.sync(player);
    }

    IAlchBagProvider bagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player, null);
    if (bagProvider != null) {
      bagProvider.syncAllBags(player);
    }
  }

  public static void playerChangeDimension(
      ServerPlayer player, ServerLevel origin, ServerLevel destination) {
    IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (knowledge != null) {
      knowledge.sync(player);
    }

    IAlchBagProvider bagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player, null);
    if (bagProvider != null) {
      bagProvider.syncAllBags(player);
    }
  }

  public static void playerConnect(ServerPlayer player) {
    PEAttachmentTypes.migrateLegacyPlayerData(player);
    onConstruct(player);
    IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player, null);
    if (knowledge != null) {
      knowledge.sync(player);
      PlayerHelper.updateScore(player, PlayerHelper.SCOREBOARD_EMC, knowledge.getEmc());
    }

    IAlchBagProvider alchBagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player, null);
    if (alchBagProvider != null) {
      alchBagProvider.syncAllBags(player);
    }

    PECore.debugLog("Sent knowledge and bag data to {}", player.getName());
    onHighAlchemistJoin(player);
  }

  public static void onConstruct(Player player) {
    if (!(player instanceof FakePlayer)) {
      TransmutationOffline.clear(player.getUUID());
      PECore.debugLog("Clearing offline data cache in preparation to load online data");
    }
  }

  public static void onHighAlchemistJoin(ServerPlayer player) {
    if (PECore.uuids.contains(player.getUUID().toString())) {
      MinecraftServer server = player.level().getServer();
      if (server != null) {
        Component joinMessage =
            PELang.HIGH_ALCHEMIST.translateColored(
                ChatFormatting.BLUE, ChatFormatting.GOLD, player.getDisplayName());
        server.getPlayerList().broadcastSystemMessage(joinMessage, false);
      }
    }
  }

  public static boolean pickupItem(ItemEntity itemEntity, Player player) {
    UUID target = ((ItemEntityAccessor) itemEntity).projecte$getTarget();
    if (!itemEntity.level().isClientSide()
        && !itemEntity.hasPickUpDelay()
        && (target == null || player.getUUID().equals(target))) {
      ItemStack bag =
          AlchemicalBag.getFirstBagWithSuctionItem(
              player, player.getInventory().getNonEquipmentItems());
      if (bag.isEmpty()) {
        return false;
      }

      IAlchBagProvider bagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player, null);
      if (bagProvider == null) {
        return false;
      }

      ItemStack stack = itemEntity.getItem();
      IItemHandler handler = bagProvider.getBag(((AlchemicalBag) bag.getItem()).color);
      ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
      int pickedUpCount = stack.getCount() - remainder.getCount();
      if (pickedUpCount <= 0) {
        return false;
      }

      player.take(itemEntity, pickedUpCount);
      if (remainder.isEmpty()) {
        itemEntity.discard();
      } else {
        itemEntity.setItem(remainder);
        itemEntity.setPickUpDelay(1);
      }

      player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), pickedUpCount);
      player.onItemPickup(itemEntity);
      return true;
    } else {
      return false;
    }
  }

  public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
    if (entity instanceof ServerPlayer player
        && source.is(DamageTypeTags.IS_FIRE)
        && TickEvents.shouldPlayerResistFire(player)) {
      return false;
    } else if (!(amount <= 0.0F) && !REPLACED_DAMAGE.contains(entity)) {
      PEArmor.ReductionInfo reductionInfo = getReductionInfo(entity, source);
      if (reductionInfo.percentReduced() >= 1.0F) {
        return false;
      }

      float absorbed =
          Math.min(amount * reductionInfo.percentReduced(), reductionInfo.maxDamagedAbsorbed());
      float reducedAmount = amount - absorbed;
      if (absorbed > 0.0F && entity.level() instanceof ServerLevel serverLevel) {
        REPLACED_DAMAGE.add(entity);

        try {
          entity.hurtServer(serverLevel, source, reducedAmount);
        } finally {
          REPLACED_DAMAGE.remove(entity);
        }

        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  private static PEArmor.ReductionInfo getReductionInfo(LivingEntity entity, DamageSource source) {
    PEArmor.ReductionInfo reductionInfo = PEArmor.ReductionInfo.ZERO;

    for (EquipmentSlot slot : ARMOR_SLOTS) {
      ItemStack armorStack = entity.getItemBySlot(slot);
      if (armorStack.getItem() instanceof PEArmor armorItem) {
        reductionInfo = reductionInfo.add(armorItem.getReductionInfo(source));
      }
    }

    return reductionInfo;
  }

  public static void onLevelTick(ServerLevel level) {
    for (ServerPlayer player : level.players()) {
      PEAttributes.tickFlight(player);
    }
  }
}
