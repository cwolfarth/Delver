package com.example.delver.game;

import com.example.delver.game.content.ArmorType;
import com.example.delver.game.content.EnemyType;
import com.example.delver.game.content.ItemRarity;
import com.example.delver.game.content.LootType;
import com.example.delver.game.content.WeaponType;
import com.example.delver.game.map.TileType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
final class DungeonGenerator {
    private static final int MAP_WIDTH = 39;
    private static final int MAP_HEIGHT = 23;
    private final Random random;

    DungeonGenerator() {
        this(new Random());
    }

    DungeonGenerator(Random random) {
        this.random = random;
    }

    void generate(GameRun run) {
        run.width = MAP_WIDTH;
        run.height = MAP_HEIGHT;
        run.tiles = new TileType[MAP_HEIGHT][MAP_WIDTH];
        run.roomIds = new int[MAP_HEIGHT][MAP_WIDTH];
        run.exploredTiles = new boolean[MAP_HEIGHT][MAP_WIDTH];
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                run.tiles[y][x] = TileType.WALL;
                run.roomIds[y][x] = -1;
            }
        }
        run.enemies.clear();
        run.loot.clear();

        int roomCount = 4 + random.nextInt(3);
        List<Integer> roomSlots = selectRoomSlots(roomCount);
        int[] roomIdsBySlot = new int[6];
        Arrays.fill(roomIdsBySlot, -1);
        List<int[]> centers = new ArrayList<>();
        for (int slot : roomSlots) {
            int col = slot % 3;
            int row = slot / 3;
            int left = 2 + col * 12 + random.nextInt(3);
            int top = 2 + row * 10 + random.nextInt(3);
            int roomWidth = 5 + random.nextInt(4);
            int roomHeight = 4 + random.nextInt(3);
            int roomId = centers.size();
            roomIdsBySlot[slot] = roomId;
            for (int y = top; y < top + roomHeight; y++) {
                for (int x = left; x < left + roomWidth; x++) {
                    run.tiles[y][x] = TileType.FLOOR;
                    run.roomIds[y][x] = roomId;
                }
            }
            centers.add(new int[]{left + roomWidth / 2, top + roomHeight / 2});
        }
        connectAdjacentRooms(run, roomSlots, roomIdsBySlot, centers);

        int startRoom = random.nextInt(roomCount);
        int stairsRoom = (startRoom + 1 + random.nextInt(roomCount - 1)) % roomCount;
        int[] stairs = centers.get(stairsRoom);
        int[] playerPosition = randomRoomPosition(run, startRoom);
        run.player.x = playerPosition[0];
        run.player.y = playerPosition[1];
        run.tiles[stairs[1]][stairs[0]] = TileType.STAIRS;

        placeEnemies(run, centers, stairs);
        placeLoot(run, startRoom);
    }

    private void connectAdjacentRooms(GameRun run, List<Integer> roomSlots, int[] roomIdsBySlot, List<int[]> centers) {
        for (int slot : roomSlots) {
            int col = slot % 3;
            int row = slot / 3;
            if (col < 2 && roomIdsBySlot[slot + 1] >= 0) {
                connect(run, centers.get(roomIdsBySlot[slot]), centers.get(roomIdsBySlot[slot + 1]));
            }
            if (row < 1 && roomIdsBySlot[slot + 3] >= 0) {
                connect(run, centers.get(roomIdsBySlot[slot]), centers.get(roomIdsBySlot[slot + 3]));
            }
        }
    }

    private List<Integer> selectRoomSlots(int roomCount) {
        List<Integer> selected = new ArrayList<>();
        boolean[] included = new boolean[6];
        int first = random.nextInt(6);
        selected.add(first);
        included[first] = true;
        while (selected.size() < roomCount) {
            List<Integer> candidates = new ArrayList<>();
            for (int slot : selected) {
                int col = slot % 3;
                int row = slot / 3;
                if (col > 0 && !included[slot - 1]) candidates.add(slot - 1);
                if (col < 2 && !included[slot + 1]) candidates.add(slot + 1);
                if (row > 0 && !included[slot - 3]) candidates.add(slot - 3);
                if (row < 1 && !included[slot + 3]) candidates.add(slot + 3);
            }
            int next = candidates.get(random.nextInt(candidates.size()));
            selected.add(next);
            included[next] = true;
        }
        return selected;
    }

    private void placeEnemies(GameRun run, List<int[]> centers, int[] stairs) {
        for (int roomId = 0; roomId < centers.size(); roomId++) {
            int[] enemyPosition = randomRoomPosition(run, roomId);
            if ((enemyPosition[0] != stairs[0] || enemyPosition[1] != stairs[1])
                    && (enemyPosition[0] != run.player.x || enemyPosition[1] != run.player.y)
                    && run.enemyAt(enemyPosition[0], enemyPosition[1]) == null) {
                List<EnemyType> enemyPool = enemyPoolForFloor(run.floor);
                EnemyType type = enemyPool.size() == 1
                        ? enemyPool.getFirst() : enemyPool.get(random.nextInt(enemyPool.size()));
                run.enemies.add(new GameRun.Enemy(type, enemyPosition[0], enemyPosition[1]));
            }
        }
    }

    static List<EnemyType> enemyPoolForFloor(int floor) {
        EnemyType[] types = EnemyType.values();
        int unlockedTypes = Math.min(1 + Math.max(0, floor) / 3, types.length);
        return List.copyOf(Arrays.asList(Arrays.copyOf(types, unlockedTypes)));
    }

    private void placeLoot(GameRun run, int startRoom) {
        placeRandomLoot(run, LootType.GOLD, "gold", 8 + random.nextInt(9), 2);
        if (run.floor == 1) {
            placeRoomLoot(run, new GameRun.Loot(LootType.POTION, 0, 0, 1, "healing potion"), startRoom);
        } else {
            placeRandomLoot(run, LootType.POTION, "healing potion", 1, 1);
        }
        placeRandomGear(run);
        if (random.nextDouble() < gearSpawnChance(run.floor)) placeRandomGear(run);
    }

    private double gearSpawnChance(int floor) {
        return Math.min(0.3 + (floor - 1) * 0.03, 0.6);
    }

    private void placeRandomGear(GameRun run) {
        ItemRarity rarity = rollGearRarity();
        if (random.nextBoolean()) {
            WeaponType[] available = Arrays.stream(WeaponType.values())
                    .filter(type -> type.rarity() == rarity)
                    .toArray(WeaponType[]::new);
            placeRandomLoot(run, new GameRun.Loot(available[random.nextInt(available.length)], 0, 0));
        } else {
            ArmorType[] available = Arrays.stream(ArmorType.values())
                    .filter(type -> type.rarity() == rarity)
                    .toArray(ArmorType[]::new);
            placeRandomLoot(run, new GameRun.Loot(available[random.nextInt(available.length)], 0, 0));
        }
    }

    private ItemRarity rollGearRarity() {
        int roll = random.nextInt(1000);
        if (roll < 700) return ItemRarity.COMMON;
        if (roll < 930) return ItemRarity.UNCOMMON;
        if (roll < 990) return ItemRarity.RARE;
        return ItemRarity.LEGENDARY;
    }

    private int[] randomRoomPosition(GameRun run, int roomId) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                if (run.roomIds[y][x] == roomId) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void connect(GameRun run, int[] from, int[] to) {
        int x = from[0];
        int y = from[1];
        while (x != to[0]) {
            run.tiles[y][x] = TileType.FLOOR;
            x += Integer.compare(to[0], x);
        }
        while (y != to[1]) {
            run.tiles[y][x] = TileType.FLOOR;
            y += Integer.compare(to[1], y);
        }
        run.tiles[y][x] = TileType.FLOOR;
    }

    private void placeRandomLoot(GameRun run, LootType type, String name, int amount, int count) {
        for (int i = 0; i < count; i++) {
            for (int attempts = 0; attempts < 100; attempts++) {
                int x = random.nextInt(run.width);
                int y = random.nextInt(run.height);
                if (run.tiles[y][x] == TileType.FLOOR && !(x == run.player.x && y == run.player.y)
                        && run.enemyAt(x, y) == null && run.loot.stream().noneMatch(item -> item.x == x && item.y == y)) {
                    run.loot.add(new GameRun.Loot(type, x, y, amount, name));
                    break;
                }
            }
        }
    }

    private void placeRandomLoot(GameRun run, GameRun.Loot loot) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                int lootX = x;
                int lootY = y;
                boolean occupied = (x == run.player.x && y == run.player.y)
                        || run.enemyAt(x, y) != null
                        || run.loot.stream().anyMatch(item -> item.x == lootX && item.y == lootY);
                if (run.tiles[y][x] == TileType.FLOOR && !occupied) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Could not place guaranteed gear loot.");
        }
        int[] position = candidates.get(random.nextInt(candidates.size()));
        run.loot.add(new GameRun.Loot(loot.type, position[0], position[1], loot.amount, loot.name,
                loot.weaponType, loot.armorType));
    }

    private void placeRoomLoot(GameRun run, GameRun.Loot loot, int roomId) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                int lootX = x;
                int lootY = y;
                boolean occupied = x == run.player.x && y == run.player.y
                        || run.enemyAt(x, y) != null
                        || run.loot.stream().anyMatch(item -> item.x == lootX && item.y == lootY);
                if (run.roomIds[y][x] == roomId && !occupied) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Could not place guaranteed room loot.");
        }
        int[] position = candidates.get(random.nextInt(candidates.size()));
        run.loot.add(new GameRun.Loot(loot.type, position[0], position[1], loot.amount, loot.name,
                loot.weaponType, loot.armorType));
    }

}
