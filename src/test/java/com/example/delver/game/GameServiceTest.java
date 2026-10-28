package com.example.delver.game;

import com.example.delver.game.content.ArmorType;
import com.example.delver.game.content.EnemyType;
import com.example.delver.game.content.ItemRarity;
import com.example.delver.game.content.LootType;
import com.example.delver.game.content.WeaponType;
import com.example.delver.game.map.TileType;
import com.example.delver.score.HighScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GameServiceTest {
    private HighScoreService highScoreService;
    private GameService gameService;
    private DungeonGenerator dungeonGenerator;
    private GameSnapshotFactory snapshotFactory;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        highScoreService = mock(HighScoreService.class);
        dungeonGenerator = new DungeonGenerator(new Random(81723));
        snapshotFactory = new GameSnapshotFactory();
        inventoryService = new InventoryService();
        gameService = new GameService(highScoreService, dungeonGenerator, snapshotFactory, inventoryService,
                new CombatService(new Random(81723)));
    }

    @Test
    void floorLayoutIsVisibleWhileContentsRemainFogged() {
        GameSnapshot run = gameService.start("Ada");

        assertThat(run.width()).isEqualTo(39);
        assertThat(run.height()).isEqualTo(23);
        assertThat(run.map()).hasSize(23);
        assertThat(run.map().getFirst()).hasSize(39);
        assertThat(run.map().stream().flatMap(List::stream))
                .contains(TileType.FOGGED_WALL, TileType.FOGGED_FLOOR);
        assertThat(run.map().stream().flatMap(List::stream)).doesNotContain(TileType.STAIRS);
        assertThat(run.enemies()).isNotEmpty();
        assertThat(run.loot()).isNotEmpty();
        assertThat(run.loot()).extracting(GameSnapshot.LootView::type)
                .contains("GOLD", "POTION");
        assertThat(run.enemies()).allSatisfy(enemy ->
                assertThat(run.map().get(enemy.y()).get(enemy.x()))
                        .isNotIn(TileType.FOGGED_WALL, TileType.FOGGED_FLOOR));
        assertThat(run.loot()).allSatisfy(item ->
                assertThat(run.map().get(item.y()).get(item.x()))
                        .isNotIn(TileType.FOGGED_WALL, TileType.FOGGED_FLOOR));
        assertThat(run.map().get(run.player().y()).get(run.player().x()))
                .isNotIn(TileType.FOGGED_WALL, TileType.FOGGED_FLOOR);
        assertThat(run.map().stream().flatMap(List::stream)).contains(TileType.FLOOR);
    }

    @Test
    void roomShowsCorridorEntrancesAndEnteringCorridorRevealsItsContents() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.width = 7;
        run.height = 3;
        run.tiles = new TileType[run.height][run.width];
        run.roomIds = new int[run.height][run.width];
        run.exploredTiles = new boolean[run.height][run.width];
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                run.tiles[y][x] = TileType.WALL;
                run.roomIds[y][x] = -1;
            }
        }
        run.roomIds[1][0] = 0;
        run.roomIds[1][1] = 0;
        run.player.x = 1;
        run.player.y = 1;
        for (int x = 0; x < run.width; x++) {
            run.tiles[1][x] = TileType.FLOOR;
        }
        run.roomIds[1][6] = 1;
        run.enemies.add(new GameRun.Enemy(EnemyType.GOBLIN, 4, 1));
        run.enemies.add(new GameRun.Enemy(EnemyType.SKELETON, 6, 1));

        GameSnapshot inRoom = snapshotFactory.create(run);

        assertThat(inRoom.map().get(1)).containsExactly(
                TileType.FLOOR, TileType.FLOOR, TileType.FLOOR,
                TileType.FOGGED_FLOOR, TileType.FOGGED_FLOOR, TileType.FOGGED_FLOOR, TileType.FOGGED_FLOOR);
        assertThat(inRoom.enemies()).isEmpty();

        run.player.x = 3;
        GameSnapshot inCorridor = snapshotFactory.create(run);

        assertThat(inCorridor.map().get(1)).containsExactly(
                TileType.FLOOR, TileType.FLOOR, TileType.FLOOR,
                TileType.FLOOR, TileType.FLOOR, TileType.FLOOR, TileType.FOGGED_FLOOR);
        assertThat(inCorridor.enemies()).extracting(GameSnapshot.EnemyView::x).containsExactly(4);

        run.player.x = 5;
        GameSnapshot atRoomEntrance = snapshotFactory.create(run);

        assertThat(atRoomEntrance.map().get(1)).containsExactly(
                TileType.FLOOR, TileType.FLOOR, TileType.FLOOR,
                TileType.FLOOR, TileType.FLOOR, TileType.FLOOR, TileType.FLOOR);
        assertThat(atRoomEntrance.enemies()).extracting(GameSnapshot.EnemyView::x).containsExactly(4, 6);
    }

    @Test
    void standingAtCorridorJunctionDoesNotRevealOtherBranches() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.width = 5;
        run.height = 5;
        run.tiles = new TileType[run.height][run.width];
        run.roomIds = new int[run.height][run.width];
        run.exploredTiles = new boolean[run.height][run.width];
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                run.tiles[y][x] = TileType.WALL;
                run.roomIds[y][x] = -1;
            }
        }
        for (int x = 1; x <= 3; x++) run.tiles[2][x] = TileType.FLOOR;
        for (int y = 1; y <= 3; y++) run.tiles[y][2] = TileType.FLOOR;
        run.player.x = 2;
        run.player.y = 2;
        run.enemies.add(new GameRun.Enemy(EnemyType.GOBLIN, 3, 2));
        run.enemies.add(new GameRun.Enemy(EnemyType.SKELETON, 2, 1));

        GameSnapshot snapshot = snapshotFactory.create(run);

        assertThat(snapshot.map().get(2).get(2)).isEqualTo(TileType.FLOOR);
        assertThat(snapshot.map().get(2).get(1)).isEqualTo(TileType.FOGGED_FLOOR);
        assertThat(snapshot.map().get(2).get(3)).isEqualTo(TileType.FOGGED_FLOOR);
        assertThat(snapshot.map().get(1).get(2)).isEqualTo(TileType.FOGGED_FLOOR);
        assertThat(snapshot.map().get(3).get(2)).isEqualTo(TileType.FOGGED_FLOOR);
        assertThat(snapshot.enemies()).isEmpty();
    }

    @Test
    void corridorVisionDoesNotSpreadAcrossWallsIntoSeparateCorridors() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.width = 6;
        run.height = 5;
        run.tiles = new TileType[run.height][run.width];
        run.roomIds = new int[run.height][run.width];
        run.exploredTiles = new boolean[run.height][run.width];
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                run.tiles[y][x] = TileType.WALL;
                run.roomIds[y][x] = -1;
            }
        }
        run.roomIds[1][0] = 0;
        for (int x = 0; x < 5; x++) run.tiles[1][x] = TileType.FLOOR;
        for (int x = 1; x < 5; x++) run.tiles[3][x] = TileType.FLOOR;
        run.player.x = 2;
        run.player.y = 1;
        run.enemies.add(new GameRun.Enemy(EnemyType.SKELETON, 3, 3));

        GameSnapshot snapshot = snapshotFactory.create(run);

        assertThat(snapshot.map().get(3).get(3)).isEqualTo(TileType.FOGGED_FLOOR);
        assertThat(snapshot.map().get(2).get(3)).isEqualTo(TileType.FOGGED_WALL);
        assertThat(snapshot.enemies()).isEmpty();
    }

    @Test
    void floorsVaryRoomCountSpawnAndStairPlacement() {
        Set<Long> roomCounts = new HashSet<>();
        Set<String> playerPositions = new HashSet<>();
        Set<String> stairPositions = new HashSet<>();

        for (int floor = 0; floor < 30; floor++) {
            GameRun run = new GameRun(UUID.randomUUID(), "Ada");
            dungeonGenerator.generate(run);
            roomCounts.add(java.util.Arrays.stream(run.roomIds)
                    .flatMapToInt(java.util.Arrays::stream)
                    .filter(roomId -> roomId >= 0)
                    .distinct()
                    .count());
            playerPositions.add(key(run.player.x, run.player.y));
            for (int y = 0; y < run.height; y++) {
                for (int x = 0; x < run.width; x++) {
                    if (run.tiles[y][x] == TileType.STAIRS) {
                        stairPositions.add(key(x, y));
                        assertThat(run.roomIds[y][x]).isNotEqualTo(run.roomIds[run.player.y][run.player.x]);
                    }
                }
            }
        }

        assertThat(roomCounts).contains(4L, 5L, 6L);
        assertThat(playerPositions).hasSizeGreaterThan(1);
        assertThat(stairPositions).hasSizeGreaterThan(1);
    }

    @Test
    void everyFloorHasGuaranteedGearAndLowerRaritiesAreMoreCommon() {
        Map<ItemRarity, Integer> gearByRarity = new HashMap<>();

        for (int floor = 0; floor < 500; floor++) {
            GameRun run = new GameRun(UUID.randomUUID(), "Ada");
            dungeonGenerator.generate(run);
            List<GameRun.Loot> gear = run.loot.stream()
                    .filter(item -> item.type == LootType.WEAPON || item.type == LootType.ARMOR)
                    .toList();
            assertThat(run.loot).filteredOn(item -> item.type == LootType.GOLD).hasSize(2);
            assertThat(run.loot).filteredOn(item -> item.type == LootType.POTION).hasSize(1);
            assertThat(gear).hasSizeBetween(1, 2);
            gear.forEach(item -> {
                ItemRarity rarity = item.weaponType != null
                        ? item.weaponType.rarity() : item.armorType.rarity();
                gearByRarity.merge(rarity, 1, Integer::sum);
            });
        }

        assertThat(gearByRarity.values().stream().mapToInt(Integer::intValue).sum()).isGreaterThan(500);
        assertThat(gearByRarity.getOrDefault(ItemRarity.COMMON, 0))
                .isGreaterThan(gearByRarity.getOrDefault(ItemRarity.UNCOMMON, 0));
        assertThat(gearByRarity.getOrDefault(ItemRarity.UNCOMMON, 0))
                .isGreaterThan(gearByRarity.getOrDefault(ItemRarity.RARE, 0));
    }

    @Test
    void gearCanBeEquippedFromTheGeneralInventory() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.player.inventory.clear();
        run.loot.add(new GameRun.Loot(WeaponType.IRON_SWORD, run.player.x, run.player.y));
        inventoryService.collectLoot(run);
        assertThat(run.player.inventory.getFirst().weaponType()).isEqualTo(WeaponType.IRON_SWORD);

        inventoryService.equipItem(run, 0);
        assertThat(run.player.equippedWeapon.name()).isEqualTo("iron sword");
        assertThat(run.player.inventory.getFirst().weaponType()).isEqualTo(WeaponType.WORN_BLADE);
        assertThat(run.player.attack + run.player.equippedWeapon.bonus()).isEqualTo(6);

        run.loot.add(new GameRun.Loot(ArmorType.LEATHER_ARMOR, run.player.x, run.player.y));
        inventoryService.collectLoot(run);
        inventoryService.equipItem(run, 1);
        assertThat(run.player.equippedArmor.name()).isEqualTo("leather armor");
        assertThat(run.player.inventory.get(1).armorType()).isEqualTo(ArmorType.CLOTH_ARMOR);
    }

    @Test
    void potionsOccupyInventorySlotsAndAreRemovedWhenUsed() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        assertThat(run.player.inventory).hasSize(1);
        assertThat(run.player.inventory.getFirst().type()).isEqualTo(LootType.POTION);
        run.player.hp = 16;

        inventoryService.usePotion(run, null);

        assertThat(run.player.hp).isEqualTo(20);
        assertThat(run.player.inventory).isEmpty();
    }

    @Test
    void discardingAnItemRemovesItAndMakesRoomForLoot() {
        GameSnapshot run = gameService.start("Ada");
        assertThat(run.player().inventory()).hasSize(1);

        run = gameService.act(run.runId(), ActionType.DISCARD_ITEM, 0);

        assertThat(run.player().inventory()).isEmpty();
        assertThat(run.messages()).contains("You discard healing potion.");
        assertThat(run.player().inventoryCapacity()).isEqualTo(5);
    }

    @Test
    void lootRemainsOnTheGroundWhenInventoryIsFull() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.player.inventory.clear();
        for (int slot = 0; slot < run.player.inventoryCapacity; slot++) {
            run.player.inventory.add(GameRun.Item.potion());
        }
        run.loot.add(new GameRun.Loot(WeaponType.IRON_SWORD, run.player.x, run.player.y));

        inventoryService.collectLoot(run);

        assertThat(run.player.inventory).hasSize(5);
        assertThat(run.loot).hasSize(1);
        assertThat(run.messages).contains("Your inventory is full. Discard an item before picking this up.");
    }

    @Test
    void enemyTypesOwnTheirCombatAndRewardStats() {
        assertThat(EnemyType.GOBLIN.maxHp()).isPositive();
        assertThat(EnemyType.GOBLIN.attackPower()).isPositive();
        assertThat(EnemyType.GOBLIN.goldReward()).isPositive();
        assertThat(EnemyType.SKELETON.maxHp()).isGreaterThan(EnemyType.GOBLIN.maxHp());
    }

    @Test
    void playerAndEnemyHitRollsUseASeventyPercentChance() {
        assertThat(CombatService.rollToHit(randomReturning(0))).isTrue();
        assertThat(CombatService.rollToHit(randomReturning(69))).isTrue();
        assertThat(CombatService.rollToHit(randomReturning(70))).isFalse();
        assertThat(CombatService.rollToHit(randomReturning(99))).isFalse();
    }

    @Test
    void strongerEnemyTypesJoinTheSpawnPoolEveryThreeFloors() {
        assertThat(DungeonGenerator.enemyPoolForFloor(1)).containsExactly(EnemyType.GOBLIN);
        assertThat(DungeonGenerator.enemyPoolForFloor(2)).containsExactly(EnemyType.GOBLIN);
        assertThat(DungeonGenerator.enemyPoolForFloor(3))
                .containsExactly(EnemyType.GOBLIN, EnemyType.SKELETON);
        assertThat(DungeonGenerator.enemyPoolForFloor(6))
                .containsExactly(EnemyType.GOBLIN, EnemyType.SKELETON, EnemyType.ORC);
        assertThat(DungeonGenerator.enemyPoolForFloor(9))
                .containsExactly(EnemyType.GOBLIN, EnemyType.SKELETON, EnemyType.ORC, EnemyType.TROLL);
        assertThat(DungeonGenerator.enemyPoolForFloor(12))
                .containsExactly(EnemyType.GOBLIN, EnemyType.SKELETON, EnemyType.ORC,
                        EnemyType.TROLL, EnemyType.WRAITH);
        assertThat(DungeonGenerator.enemyPoolForFloor(15)).containsExactly(EnemyType.values());

        EnemyType[] types = EnemyType.values();
        for (int index = 1; index < types.length; index++) {
            assertThat(types[index].maxHp()).isGreaterThan(types[index - 1].maxHp());
            assertThat(types[index].attackPower()).isGreaterThan(types[index - 1].attackPower());
            assertThat(types[index].goldReward()).isGreaterThan(types[index - 1].goldReward());
        }
    }

    @Test
    void weaponAndArmorTypesOwnTheirNamesAndBonuses() {
        assertThat(WeaponType.IRON_SWORD.displayName()).isEqualTo("iron sword");
        assertThat(WeaponType.IRON_SWORD.attackBonus()).isEqualTo(2);
        assertThat(ArmorType.LEATHER_ARMOR.displayName()).isEqualTo("leather armor");
        assertThat(ArmorType.LEATHER_ARMOR.defenseBonus()).isEqualTo(1);
        assertThat(WeaponType.DRAGONFANG.rarity().name()).isEqualTo("LEGENDARY");
        assertThat(WeaponType.DRAGONFANG.sellValue()).isPositive();
        assertThat(ArmorType.DRAGON_SCALE.rarity().name()).isEqualTo("LEGENDARY");
        assertThat(ArmorType.DRAGON_SCALE.sellValue()).isPositive();
        assertThat(java.util.Arrays.stream(WeaponType.values()).map(WeaponType::rarity).distinct())
                .containsExactlyInAnyOrder(ItemRarity.values());
        assertThat(java.util.Arrays.stream(ArmorType.values()).map(ArmorType::rarity).distinct())
                .containsExactlyInAnyOrder(ItemRarity.values());
    }

    @Test
    void lootSnapshotsExposeGearRarityAndSellValue() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.width = 1;
        run.height = 1;
        run.tiles = new TileType[][]{{TileType.FLOOR}};
        run.roomIds = new int[][]{{-1}};
        run.exploredTiles = new boolean[][]{{false}};
        run.loot.add(new GameRun.Loot(WeaponType.DRAGONFANG, 0, 0));

        GameSnapshot.LootView loot = snapshotFactory.create(run).loot().getFirst();

        assertThat(loot.rarity()).isEqualTo(ItemRarity.LEGENDARY);
        assertThat(loot.sellValue()).isEqualTo(WeaponType.DRAGONFANG.sellValue());
    }

    @Test
    void eachActionAdvancesOneTurnAndReturnsAnUpdatedSnapshot() {
        GameSnapshot started = gameService.start("Ada");

        GameSnapshot afterTurn = gameService.act(started.runId(), ActionType.WAIT);

        assertThat(afterTurn.turn()).isEqualTo(started.turn() + 1);
        assertThat(afterTurn.messages()).contains("You wait and listen to the dungeon.");
    }

    @Test
    void runScoreCombinesGoldAndFloors() {
        GameRun run = new GameRun(UUID.randomUUID(), "Ada");
        run.floor = 3;
        run.player.gold = 45;

        assertThat(run.score()).isEqualTo(245);
    }

    @Test
    void invalidNameAndUnknownRunAreReported() {
        assertThatThrownBy(() -> gameService.start("A name that is definitely too long"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gameService.get(UUID.randomUUID()))
                .isInstanceOf(RunNotFoundException.class);
    }

    @Test
    void aRunEndingInDeathIsRecordedAsAHighScore() {
        GameSnapshot run = gameService.start("Ada");
        for (int i = 0; i < 500 && !run.gameOver(); i++) {
            run = gameService.act(run.runId(), ActionType.WAIT);
        }

        assertThat(run.gameOver()).isTrue();
        verify(highScoreService).record("Ada", run.score(), run.floor(), run.player().gold());
    }

    private String key(int x, int y) {
        return x + ":" + y;
    }

    private Random randomReturning(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                assertThat(bound).isEqualTo(100);
                return value;
            }
        };
    }
}
