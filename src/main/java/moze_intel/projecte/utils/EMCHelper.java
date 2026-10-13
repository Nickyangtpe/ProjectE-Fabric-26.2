package moze_intel.projecte.utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Iterator;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.items.IItemHandler;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public final class EMCHelper {
  private static final NumberFormat EMC_FORMATTER =
      (NumberFormat)
          Util.make(NumberFormat.getInstance(), formatter -> formatter.setMaximumFractionDigits(1));

  public static <K> Object2IntMap<K> intMapOf(K key, int value) {
    return Object2IntMaps.singleton(key, value);
  }

  public static <K> Object2IntMap<K> intMapOf(K key, int value, K key2, int value2) {
    Object2IntMap<K> intMap = new Object2IntArrayMap(2);
    intMap.put(key, value);
    intMap.put(key2, value2);
    return intMap;
  }

  public static <K> Object2IntMap<K> intMapOf(
      K key, int value, K key2, int value2, K key3, int value3) {
    Object2IntMap<K> intMap = new Object2IntArrayMap(3);
    intMap.put(key, value);
    intMap.put(key2, value2);
    intMap.put(key3, value3);
    return intMap;
  }

  public static <K> Object2IntMap<K> intMapOf(
      K key, int value, K key2, int value2, K key3, int value3, K key4, int value4) {
    Object2IntMap<K> intMap = new Object2IntArrayMap(3);
    intMap.put(key, value);
    intMap.put(key2, value2);
    intMap.put(key3, value3);
    intMap.put(key4, value4);
    return intMap;
  }

  public static long consumePlayerFuel(
      Player player, @Range(from = 0L, to = Long.MAX_VALUE) long minFuel) {
    if (!player.isCreative() && minFuel != 0L) {
      IItemHandler curios = IntegrationHelper.getAccessoryInventory(player);
      if (curios != null) {
        int i = 0;

        for (int slots = curios.getSlots(); i < slots; i++) {
          long actualExtracted = tryExtract(curios.getStackInSlot(i), minFuel);
          if (actualExtracted > 0L) {
            player.containerMenu.broadcastChanges();
            return actualExtracted;
          }
        }
      }

      IItemHandler inv = Capabilities.ItemHandler.ENTITY.find(player, null);
      if (inv != null) {
        Int2IntMap map = new Int2IntOpenHashMap();
        boolean metRequirement = false;
        long emcConsumed = 0L;
        int i = 0;

        for (int slots = inv.getSlots(); i < slots; i++) {
          ItemStack stack = inv.getStackInSlot(i);
          if (!stack.isEmpty()) {
            long actualExtracted = tryExtract(stack, minFuel);
            if (actualExtracted > 0L) {
              player.containerMenu.broadcastChanges();
              return actualExtracted;
            }

            if (!metRequirement && FuelMapper.isStackFuel(stack)) {
              long emc = IEMCProxy.INSTANCE.getValue(stack);
              int toRemove = Mth.ceil((double) (minFuel - emcConsumed) / emc);
              int actualRemoved = Math.min(stack.getCount(), toRemove);
              if (actualRemoved > 0) {
                map.put(i, actualRemoved);
                emcConsumed += emc * actualRemoved;
                metRequirement = emcConsumed >= minFuel;
              }
            }
          }
        }

        if (metRequirement) {
          Iterator<Entry> iterator = Int2IntMaps.fastIterator(map);

          while (iterator.hasNext()) {
            Entry entry = iterator.next();
            inv.extractItem(entry.getIntKey(), entry.getIntValue(), false);
          }

          player.containerMenu.broadcastChanges();
          return emcConsumed;
        }
      }

      return -1L;
    } else {
      return minFuel;
    }
  }

  private static long tryExtract(@NotNull ItemStack stack, long minFuel) {
    IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(stack, null);
    if (emcHolder != null) {
      long simulatedExtraction =
          emcHolder.extractEmc(stack, minFuel, IEmcStorage.EmcAction.SIMULATE);
      if (simulatedExtraction >= minFuel) {
        return emcHolder.extractEmc(stack, simulatedExtraction, IEmcStorage.EmcAction.EXECUTE);
      }
    }

    return 0L;
  }

  public static String formatEmc(Number emc) {
    return EMC_FORMATTER.format(emc);
  }

  public static String formatEmc(double emc) {
    return EMC_FORMATTER.format(emc);
  }

  public static String formatEmc(long emc) {
    return EMC_FORMATTER.format(emc);
  }

  public static @Range(from = 0L, to = Long.MAX_VALUE) long getEmcSellValue(
      @Range(from = 0L, to = Long.MAX_VALUE) long originalValue) {
    if (originalValue == 0L) {
      return 0L;
    }

    long emc = Mth.lfloor(originalValue * ProjectEConfig.server.difficulty.covalenceLoss.get());
    if (emc < 1L) {
      if (ProjectEConfig.server.difficulty.covalenceLossRounding.get()) {
        emc = 1L;
      } else {
        emc = 0L;
      }
    }

    return emc;
  }

  public static Component getEmcTextComponent(long emc, int stackSize) {
    if (ProjectEConfig.server.difficulty.covalenceLoss.get() == 1.0) {
      String value;
      ILangEntry prefix;
      if (stackSize > 1) {
        prefix = PELang.EMC_STACK_TOOLTIP;
        value = formatEmc(BigInteger.valueOf(emc).multiply(BigInteger.valueOf(stackSize)));
      } else {
        prefix = PELang.EMC_TOOLTIP;
        value = formatEmc(emc);
      }

      return prefix.translateColored(ChatFormatting.YELLOW, ChatFormatting.WHITE, value);
    } else {
      long emcSellValue = getEmcSellValue(emc);
      ILangEntry prefix;
      String value;
      String sell;
      if (stackSize > 1) {
        prefix = PELang.EMC_STACK_TOOLTIP_WITH_SELL;
        BigInteger bigIntStack = BigInteger.valueOf(stackSize);
        value = formatEmc(BigInteger.valueOf(emc).multiply(bigIntStack));
        sell = formatEmc(BigInteger.valueOf(emcSellValue).multiply(bigIntStack));
      } else {
        prefix = PELang.EMC_TOOLTIP_WITH_SELL;
        value = formatEmc(emc);
        sell = formatEmc(emcSellValue);
      }

      return prefix.translateColored(
          ChatFormatting.YELLOW, ChatFormatting.WHITE, value, ChatFormatting.BLUE, sell);
    }
  }

  public static @Range(from = 0L, to = Long.MAX_VALUE) long getEMCPerDurability(ItemStack stack) {
    if (stack.isEmpty()) {
      return 0L;
    } else if (stack.isDamageableItem()) {
      ItemStack stackCopy = stack.copy();
      stackCopy.setDamageValue(0);
      long emc =
          (long) Math.ceil((double) IEMCProxy.INSTANCE.getValue(stackCopy) / stack.getMaxDamage());
      return Math.max(emc, 1L);
    } else {
      return 1L;
    }
  }

  public static long removeFractionalEMC(ItemStack stack, double amount) {
    double unprocessedEMC = (Double) stack.getOrDefault(PEDataComponentTypes.UNPROCESSED_EMC, 0.0);
    unprocessedEMC += amount;
    long toRemove = (long) unprocessedEMC;
    unprocessedEMC -= toRemove;
    stack.set(PEDataComponentTypes.UNPROCESSED_EMC, unprocessedEMC);
    return toRemove;
  }
}
