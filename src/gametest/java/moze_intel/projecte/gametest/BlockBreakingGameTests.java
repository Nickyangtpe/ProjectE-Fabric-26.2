package moze_intel.projecte.gametest;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.compat.capabilities.Capabilities;
import moze_intel.projecte.compat.event.ToolModeBreakEvents;
import moze_intel.projecte.compat.items.IItemHandlerModifiable;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.gameObjs.items.MercurialEye.MercurialEyeMode;
import moze_intel.projecte.gameObjs.items.tools.PEPickaxe.PickaxeMode;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.ExperienceDropTarget;
import moze_intel.projecte.utils.NovaExplosion;
import moze_intel.projecte.utils.ToolHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockBreakingGameTests {
  private static final BlockPos TARGET = new BlockPos(3, 2, 3);

  @GameTest
  public void catalystsRetainOreSpawnerAndSculkExperience(GameTestHelper helper) {
    for (Item item : catalysts()) {
      for (Block block : new Block[] {Blocks.DIAMOND_ORE, Blocks.REDSTONE_ORE, Blocks.SPAWNER, Blocks.SCULK}) {
        clearDrops(helper);
        helper.setBlock(TARGET, block);
        ServerPlayer player = player(helper, item, GameType.SURVIVAL);
        useOn(helper, player, TARGET);

        helper.assertBlockPresent(Blocks.AIR, TARGET);
        int xp = experience(helper);
        assertExperienceAt(helper, Vec3.atLowerCornerOf(helper.absolutePos(TARGET)), 0.25);
        if (block == Blocks.DIAMOND_ORE) {
          helper.assertValueInBetween(3, xp, 7, "Diamond ore's normal experience");
          helper.assertValueEqual(itemCount(helper, Items.DIAMOND), 1, "Original diamond loot is not duplicated");
        } else if (block == Blocks.REDSTONE_ORE) {
          helper.assertValueInBetween(1, xp, 5, "Redstone ore's normal experience");
          helper.assertValueInBetween(4, itemCount(helper, Items.REDSTONE), 5, "Original redstone loot");
        } else if (block == Blocks.SPAWNER) {
          helper.assertValueInBetween(15, xp, 43, "Spawner's normal experience");
          helper.assertValueEqual(itemCount(helper, Items.SPAWNER), 0, "Spawner is experience-only");
        } else {
          helper.assertValueEqual(xp, 1, "Sculk gives experience even without item loot");
          helper.assertValueEqual(itemCount(helper, Items.SCULK), 0, "Sculk's normal no-silk loot");
        }
        useOn(helper, player, TARGET);
        helper.assertValueEqual(experience(helper), xp, "Reusing the ability cannot drop experience again");
      }
    }
    helper.succeed();
  }

  @GameTest
  public void chargedCatalystDropsExperienceForEveryRemovedBlockOnce(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.DESTRUCTION_CATALYST.get(), GameType.SURVIVAL);
    player.getMainHandItem().set(PEDataComponentTypes.CHARGE, 1);
    for (int x = 2; x <= 4; x++) {
      helper.setBlock(new BlockPos(x, 2, 2), Blocks.SCULK);
      helper.setBlock(new BlockPos(x, 2, 3), Blocks.SCULK);
    }
    useOn(helper, player, TARGET);
    helper.assertValueEqual(experience(helper), 6, "All six blocks' experience is retained, including merged orbs");
    assertExperienceAt(helper, Vec3.atLowerCornerOf(helper.absolutePos(TARGET)), 0.25);
    helper.assertValueEqual(player.getMainHandItem().get(PEDataComponentTypes.STORED_EMC), 952L, "Original per-block fuel cost");
    useOn(helper, player, TARGET);
    helper.assertValueEqual(experience(helper), 6, "No extra experience from an empty area");
    helper.succeed();
  }

  @GameTest
  public void silkTouchCatalystsKeepOreBlocksWithoutExperience(GameTestHelper helper) {
    for (Item item : catalysts()) {
      clearDrops(helper);
      helper.setBlock(TARGET, Blocks.DIAMOND_ORE);
      ServerPlayer player = player(helper, item, GameType.SURVIVAL);
      silkTouch(helper, player.getMainHandItem());
      useOn(helper, player, TARGET);
      helper.assertBlockPresent(Blocks.AIR, TARGET);
      helper.assertValueEqual(experience(helper), 0, "Silk Touch still suppresses ore experience");
      helper.assertValueEqual(itemCount(helper, Items.DIAMOND_ORE), 1, "Silk Touch keeps the original ore drop");
    }
    helper.succeed();
  }

  @GameTest
  public void deniedOrUnfundedDestructionCannotDropExperience(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.DESTRUCTION_CATALYST.get(), GameType.SURVIVAL);
    helper.setBlock(TARGET, Blocks.DIAMOND_ORE);
    AtomicBoolean deny = new AtomicBoolean(true);
    PlayerBlockBreakEvents.BEFORE.register((level, breaker, pos, state, blockEntity) -> breaker != player || !deny.get());
    try {
      useOn(helper, player, TARGET);
      helper.assertBlockPresent(Blocks.DIAMOND_ORE, TARGET);
      helper.assertValueEqual(experience(helper), 0, "Canceled break cannot give experience");
      helper.assertValueEqual(itemCount(helper, Items.DIAMOND), 0, "Canceled break cannot give item loot");
      deny.set(false);
      player.getMainHandItem().set(PEDataComponentTypes.STORED_EMC, 0L);
      useOn(helper, player, TARGET);
      helper.assertBlockPresent(Blocks.DIAMOND_ORE, TARGET);
      helper.assertValueEqual(experience(helper), 0, "Missing fuel cannot give experience");
    } finally {
      deny.set(false);
    }
    helper.succeed();
  }

  @GameTest
  public void ordinaryBlocksAndCreativeDestructionDoNotGiveExperience(GameTestHelper helper) {
    helper.setBlock(TARGET, Blocks.STONE);
    ServerPlayer survival = player(helper, PEItems.DESTRUCTION_CATALYST.get(), GameType.SURVIVAL);
    useOn(helper, survival, TARGET);
    helper.assertBlockPresent(Blocks.AIR, TARGET);
    helper.assertValueEqual(experience(helper), 0, "Ordinary stone has no experience");

    helper.setBlock(TARGET, Blocks.SCULK);
    ServerPlayer creative = player(helper, PEItems.DESTRUCTION_CATALYST.get(), GameType.CREATIVE);
    useOn(helper, creative, TARGET);
    helper.assertBlockPresent(Blocks.AIR, TARGET);
    helper.assertValueEqual(experience(helper), 0, "Creative destruction does not award experience");
    helper.succeed();
  }

  @GameTest
  public void areaToolsRetainExperienceAndOriginalLoot(GameTestHelper helper) {
    for (Item item : new Item[] {PEItems.DARK_MATTER_HAMMER.get(), PEItems.RED_MATTER_HAMMER.get(), PEItems.RED_MATTER_MORNING_STAR.get()}) {
      clearDrops(helper);
      ServerPlayer player = player(helper, item, GameType.SURVIVAL);
      player.getMainHandItem().set(PEDataComponentTypes.CHARGE, 1);
      for (int x = 2; x <= 4; x++) {
        helper.setBlock(new BlockPos(x, 2, 3), Blocks.DIAMOND_ORE);
      }
      ToolHelper.digAOE(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), helper.absolutePos(TARGET), Direction.SOUTH, true, 0L);
      for (int x = 2; x <= 4; x++) {
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, 3));
      }
      helper.assertValueEqual(itemCount(helper, Items.DIAMOND), 3, "All three original ore drops");
      helper.assertValueInBetween(9, experience(helper), 21, "Experience for all three mined ores");
      assertExperienceAt(helper, Vec3.atLowerCornerOf(helper.absolutePos(TARGET)), 0.25);
    }
    helper.succeed();
  }

  @GameTest
  public void veinMiningRetainsEveryBlocksExperienceOnce(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.RED_MATTER_PICKAXE.get(), GameType.SURVIVAL);
    for (int x = 3; x <= 5; x++) {
      helper.setBlock(new BlockPos(x, 2, 3), Blocks.SCULK);
    }
    List<ItemStack> drops = new ArrayList<>();
    int mined = WorldHelper.harvestVein(helper.getLevel(), player, player.getMainHandItem(), new AABB(helper.absolutePos(TARGET)), drops, Blocks.SCULK, (state, block) -> state.is(block));
    helper.assertValueEqual(mined, 3, "All connected blocks were mined");
    helper.assertValueEqual(experience(helper), 3, "Every vein block's experience is awarded once");
    helper.assertTrue(drops.isEmpty(), "Experience is retained even when loot is empty");
    mined = WorldHelper.harvestVein(helper.getLevel(), player, player.getMainHandItem(), new AABB(helper.absolutePos(TARGET)), drops, Blocks.SCULK, (state, block) -> state.is(block));
    helper.assertValueEqual(mined, 0, "Already removed blocks cannot be re-mined");
    helper.assertValueEqual(experience(helper), 3, "Repeated vein mining cannot duplicate experience");
    helper.succeed();
  }

  @GameTest
  public void multiBlockModeKeepsPrimaryAndSecondaryExperience(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.DARK_MATTER_PICKAXE.get(), GameType.SURVIVAL);
    player.getMainHandItem().set(PEDataComponentTypes.PICKAXE_MODE, PickaxeMode.TALLSHOT);
    for (int y = 1; y <= 3; y++) {
      helper.setBlock(new BlockPos(3, y, 3), Blocks.DIAMOND_ORE);
    }
    BlockPos target = helper.absolutePos(TARGET);
    ToolModeBreakEvents.captureDigAction(player, target, Action.START_DESTROY_BLOCK, Direction.NORTH);
    helper.assertTrue(player.gameMode.destroyBlock(target), "Native primary block break succeeds");
    for (int y = 1; y <= 3; y++) {
      helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, y, 3));
    }
    helper.assertValueEqual(itemCount(helper, Items.DIAMOND), 3, "Primary and both secondary drops");
    helper.assertValueInBetween(9, experience(helper), 21, "Primary and secondary experience are both retained");
    // The native primary drop is centered/randomized in this block; the extra blocks' drops
    // are collected at its lower corner, exactly like the secondary item loot.
    assertExperienceAt(helper, Vec3.atLowerCornerOf(target), 1.0);
    helper.succeed();
  }

  @GameTest
  public void tagClearingRetainsExperienceForCustomBlockTags(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.RED_MATTER_KATAR.get(), GameType.SURVIVAL);
    player.getMainHandItem().set(PEDataComponentTypes.CHARGE, 1);
    helper.setBlock(TARGET, Blocks.SCULK);
    ToolHelper.clearTagAOE(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), 0L,
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("projecte_condenser_tests", "experience_blocks")));
    helper.assertBlockPresent(Blocks.AIR, TARGET);
    helper.assertValueEqual(experience(helper), 1, "Tag-based destruction also keeps native experience");
    assertExperienceAt(helper, player.position(), 0.25);
    helper.succeed();
  }

  @GameTest
  public void mercurialEyeKeepsExperienceWhenReplacingAnUnvaluedBlock(GameTestHelper helper) {
    var original = new Object2LongOpenHashMap<ItemInfo>(EMCMappingHandler.createPacketData().data());
    var values = new Object2LongOpenHashMap<ItemInfo>(original);
    values.put(ItemInfo.fromItem(Items.SCULK), 0L);
    EMCMappingHandler.updateEmcValues(values);
    try {
      helper.setBlock(TARGET, Blocks.SCULK);
      ServerPlayer player = player(helper, PEItems.MERCURIAL_EYE.get(), GameType.SURVIVAL);
      ItemStack eye = player.getMainHandItem();
      eye.set(PEDataComponentTypes.MERCURIAL_EYE_MODE, MercurialEyeMode.TRANSMUTATION_CLASSIC);
      var inventory = (IItemHandlerModifiable) Capabilities.ItemHandler.ITEM.find(eye, null);
      helper.assertTrue(inventory != null, "Mercurial Eye inventory is available");
      ItemStack star = new ItemStack(PEItems.KLEIN_STAR_EIN.get());
      PEItems.KLEIN_STAR_EIN.get().insertEmc(star, 1_000, EmcAction.EXECUTE);
      inventory.setStackInSlot(0, star);
      inventory.setStackInSlot(1, new ItemStack(Items.COBBLESTONE));

      useOn(helper, player, TARGET);
      helper.assertBlockPresent(Blocks.COBBLESTONE, TARGET);
      helper.assertValueEqual(experience(helper), 1, "Replacement preserves sculk's normal experience");
      assertExperienceAt(helper, Vec3.atLowerCornerOf(helper.absolutePos(TARGET)), 1.0);
      helper.assertValueEqual(itemCount(helper, Items.SCULK), 0, "No extra sculk loot without Silk Touch");
      helper.assertValueEqual(PEItems.KLEIN_STAR_EIN.get().getStoredEmc(inventory.getStackInSlot(0)), 999L, "Replacement retains its original EMC cost");
      useOn(helper, player, TARGET);
      helper.assertValueEqual(experience(helper), 1, "Replacing the same block again does not duplicate experience");
    } finally {
      EMCMappingHandler.updateEmcValues(original);
    }
    helper.succeed();
  }

  @GameTest
  public void catalystTeleportsOnlyItsNewExperienceAlongWithLoot(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.CATALYTIC_LENS.get(), GameType.SURVIVAL);
    player.getMainHandItem().set(PEDataComponentTypes.CHARGE, 1);
    BlockPos remote = new BlockPos(3, 2, 1);
    helper.setBlock(remote, Blocks.SCULK);
    Vec3 destination = Vec3.atLowerCornerOf(helper.absolutePos(TARGET));
    Vec3 oldPosition = Vec3.atCenterOf(helper.absolutePos(remote));
    ExperienceOrb old = new ExperienceOrb(helper.getLevel(), oldPosition, Vec3.ZERO, 37);
    helper.getLevel().addFreshEntity(old);

    useOn(helper, player, TARGET);
    helper.assertBlockPresent(Blocks.AIR, remote);
    helper.assertValueEqual(old.position(), oldPosition, "Existing remote experience must not be teleported");
    helper.assertValueEqual(old.getValue(), 37, "Existing experience retains its value");
    helper.assertValueEqual(experience(helper), 38, "Teleportation keeps exactly one new sculk experience");
    var generated = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(1)).stream()
        .filter(orb -> orb != old).toList();
    helper.assertTrue(!generated.isEmpty(), "The new experience exists at the loot destination");
    for (ExperienceOrb orb : generated) {
      helper.assertTrue(orb.position().distanceTo(destination) < 0.25, "New tunnel experience is teleported to the clicked loot position");
    }
    helper.succeed();
  }

  @GameTest
  public void veinMiningTeleportsExperienceToItsItemDropPoint(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.RED_MATTER_PICKAXE.get(), GameType.SURVIVAL);
    for (int x = 3; x <= 5; x++) {
      helper.setBlock(new BlockPos(x, 2, 3), Blocks.DIAMOND_ORE);
    }
    BlockPos target = helper.absolutePos(TARGET);
    ToolHelper.tryVeinMine(player, player.getMainHandItem(), target, Direction.SOUTH);
    helper.assertValueEqual(itemCount(helper, Items.DIAMOND), 3, "Vein items are still collected once");
    helper.assertValueInBetween(9, experience(helper), 21, "All vein experience is conserved");
    assertExperienceAt(helper, Vec3.atLowerCornerOf(target), 0.25);
    helper.succeed();
  }

  @GameTest
  public void novaExplosionTeleportsBlockExperienceToItsLootDestination(GameTestHelper helper) {
    ServerPlayer player = player(helper, PEItems.HYPERKINETIC_LENS.get(), GameType.SURVIVAL);
    helper.setBlock(TARGET, Blocks.SCULK);
    Vec3 center = Vec3.atCenterOf(helper.absolutePos(TARGET));
    NovaExplosion explosion = new NovaExplosion(helper.getLevel(), player, center.x, center.y, center.z, 2, BlockInteraction.DESTROY);
    explosion.explode();
    helper.assertBlockPresent(Blocks.AIR, TARGET);
    helper.assertValueEqual(experience(helper), 1, "Explosion's native block experience is preserved");
    assertExperienceAt(helper, player.position(), 0.25);
    helper.succeed();
  }

  @GameTest
  public void experienceDestinationRestoresAfterNestedOrFailedOperations(GameTestHelper helper) {
    Vec3 first = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(1, 2, 1)));
    Vec3 second = Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(5, 2, 5)));
    Vec3 original = Vec3.atLowerCornerOf(helper.absolutePos(TARGET));
    try (ExperienceDropTarget.Scope outer = ExperienceDropTarget.at(helper.getLevel(), first)) {
      try (ExperienceDropTarget.Scope inner = ExperienceDropTarget.at(helper.getLevel(), second)) {
        ExperienceOrb.award(helper.getLevel(), original, 37);
      }
      ExperienceOrb.award(helper.getLevel(), original, 73);
    }
    try {
      try (ExperienceDropTarget.Scope ignored = ExperienceDropTarget.at(helper.getLevel(), first)) {
        throw new IllegalStateException("Simulated failed collection");
      }
    } catch (IllegalStateException expected) {
      helper.assertValueEqual(expected.getMessage(), "Simulated failed collection", "Scope closes on failure");
    }
    ExperienceOrb.award(helper.getLevel(), original, 17);
    helper.assertValueEqual(experience(helper), 127, "No lost or duplicated experience under nested scopes");
    for (ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(1))) {
      Vec3 expected = orb.getValue() == 37 ? second : orb.getValue() == 73 ? first : original;
      helper.assertTrue(orb.position().distanceTo(expected) < 0.25, "Destination is restored and normal awards are not redirected");
    }
    helper.succeed();
  }

  @GameTest
  public void silkTouchAreaAndVeinMiningKeepLootWithoutExperience(GameTestHelper helper) {
    for (Item item : new Item[] {PEItems.RED_MATTER_HAMMER.get(), PEItems.RED_MATTER_PICKAXE.get()}) {
      clearDrops(helper);
      ServerPlayer player = player(helper, item, GameType.SURVIVAL);
      player.getMainHandItem().set(PEDataComponentTypes.CHARGE, 1);
      silkTouch(helper, player.getMainHandItem());
      helper.setBlock(TARGET, Blocks.DIAMOND_ORE);
      if (item == PEItems.RED_MATTER_HAMMER.get()) {
        useOn(helper, player, TARGET);
      } else {
        ToolHelper.tryVeinMine(player, player.getMainHandItem(), helper.absolutePos(TARGET), Direction.SOUTH);
      }
      helper.assertBlockPresent(Blocks.AIR, TARGET);
      helper.assertValueEqual(itemCount(helper, Items.DIAMOND_ORE), 1, "Area and vein mining preserve Silk Touch loot");
      helper.assertValueEqual(experience(helper), 0, "Area and vein mining obey Silk Touch experience suppression");
    }
    helper.succeed();
  }

  @GameTest
  public void blockDropGameRuleStillSuppressesExperience(GameTestHelper helper) {
    var rules = helper.getLevel().getGameRules();
    boolean original = rules.get(GameRules.BLOCK_DROPS);
    rules.set(GameRules.BLOCK_DROPS, false, helper.getLevel().getServer());
    try {
      helper.setBlock(TARGET, Blocks.SCULK);
      ServerPlayer player = player(helper, PEItems.DESTRUCTION_CATALYST.get(), GameType.SURVIVAL);
      useOn(helper, player, TARGET);
      helper.assertBlockPresent(Blocks.AIR, TARGET);
      helper.assertValueEqual(experience(helper), 0, "Disabled block drops still prevent experience");
    } finally {
      rules.set(GameRules.BLOCK_DROPS, original, helper.getLevel().getServer());
    }
    helper.succeed();
  }

  private static Item[] catalysts() {
    return new Item[] {PEItems.DESTRUCTION_CATALYST.get(), PEItems.CATALYTIC_LENS.get()};
  }

  private static ServerPlayer player(GameTestHelper helper, Item item, GameType mode) {
    ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
    Vec3 position = Vec3.atCenterOf(helper.absolutePos(TARGET));
    player.setPos(position.x, position.y, position.z - 2);
    ItemStack stack = new ItemStack(item);
    stack.set(PEDataComponentTypes.STORED_EMC, 1_000L);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    return player;
  }

  private static void useOn(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
    BlockPos pos = helper.absolutePos(relativePos);
    ItemStack stack = player.getMainHandItem();
    stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
        new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false)));
  }

  private static void silkTouch(GameTestHelper helper, ItemStack stack) {
    stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
  }

  private static int experience(GameTestHelper helper) {
    int experience = 0;
    for (ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(1))) {
      // An orb may represent several merged awards. Its value alone would undercount.
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
      orb.saveWithoutId(output);
      experience += orb.getValue() * output.buildResult().getIntOr("Count", 1);
    }
    return experience;
  }

  private static void assertExperienceAt(GameTestHelper helper, Vec3 destination, double tolerance) {
    var orbs = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(1));
    helper.assertTrue(!orbs.isEmpty(), "Expected experience at the collected item drop position");
    for (ExperienceOrb orb : orbs) {
      helper.assertTrue(orb.position().distanceTo(destination) <= tolerance,
          "Experience must be collected with items, rather than left at " + orb.position());
    }
  }

  private static int itemCount(GameTestHelper helper, Item item) {
    return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1)).stream()
        .map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
  }

  private static void clearDrops(GameTestHelper helper) {
    helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, helper.getBounds().inflate(1)).forEach(ExperienceOrb::discard);
    helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1)).forEach(ItemEntity::discard);
  }
}
