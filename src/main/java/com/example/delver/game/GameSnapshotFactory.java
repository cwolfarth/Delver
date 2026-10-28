package com.example.delver.game;

import com.example.delver.game.content.ItemRarity;
import com.example.delver.game.map.TileType;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@Component
final class GameSnapshotFactory {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    GameSnapshot create(GameRun run) {
        int currentRoom = run.roomIds[run.player.y][run.player.x];
        boolean[][] visibleTiles = visibleTiles(run, currentRoom);
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                if (visibleTiles[y][x]) {
                    run.exploredTiles[y][x] = true;
                }
            }
        }

        List<List<TileType>> map = new ArrayList<>();
        for (int y = 0; y < run.height; y++) {
            List<TileType> row = new ArrayList<>();
            for (int x = 0; x < run.width; x++) {
                if (visibleTiles[y][x] || run.exploredTiles[y][x]) {
                    row.add(run.tiles[y][x]);
                } else {
                    row.add(run.tiles[y][x] == TileType.WALL ? TileType.FOGGED_WALL : TileType.FOGGED_FLOOR);
                }
            }
            map.add(List.copyOf(row));
        }
        List<GameSnapshot.EnemyView> enemies = run.enemies.stream()
                .filter(enemy -> visibleTiles[enemy.y][enemy.x])
                .map(enemy -> new GameSnapshot.EnemyView(enemy.type, enemy.type.displayName(), enemy.x, enemy.y, enemy.hp))
                .toList();
        List<GameSnapshot.LootView> loot = run.loot.stream()
                .filter(item -> visibleTiles[item.y][item.x])
                .map(item -> new GameSnapshot.LootView(item.type.name(), item.name, item.x, item.y, item.amount,
                        item.weaponType, item.armorType, lootRarity(item), lootSellValue(item)))
                .toList();
        GameSnapshot.PlayerView player = new GameSnapshot.PlayerView(run.player.x, run.player.y,
                run.player.hp, run.player.maxHp, run.player.gold, run.player.attack + run.player.equippedWeapon.bonus(),
                weaponView(run.player.equippedWeapon), armorView(run.player.equippedArmor),
                run.player.inventory.stream().map(this::inventoryItemView).toList(), run.player.inventoryCapacity);
        return new GameSnapshot(run.id, run.playerName, run.floor, run.turn, run.width, run.height,
                map, player, enemies, loot, List.copyOf(run.messages), run.gameOver, run.score());
    }

    private boolean[][] visibleTiles(GameRun run, int currentRoom) {
        boolean[][] visible = new boolean[run.height][run.width];
        if (currentRoom >= 0) {
            for (int y = 0; y < run.height; y++) {
                for (int x = 0; x < run.width; x++) {
                    if (run.roomIds[y][x] != currentRoom) {
                        continue;
                    }
                    visible[y][x] = true;
                    for (int[] direction : DIRECTIONS) {
                        int adjacentX = x + direction[0];
                        int adjacentY = y + direction[1];
                        if (isCorridorTile(run, adjacentX, adjacentY)) {
                            visible[adjacentY][adjacentX] = true;
                        }
                    }
                }
            }
            return visible;
        }

        ArrayDeque<int[]> pending = new ArrayDeque<>();
        pending.add(new int[]{run.player.x, run.player.y});
        visible[run.player.y][run.player.x] = true;
        while (!pending.isEmpty()) {
            int[] position = pending.removeFirst();
            if (corridorDegree(run, position[0], position[1]) > 2) {
                continue;
            }
            for (int[] direction : DIRECTIONS) {
                int x = position[0] + direction[0];
                int y = position[1] + direction[1];
                if (isCorridorTile(run, x, y) && !visible[y][x]) {
                    visible[y][x] = true;
                    pending.addLast(new int[]{x, y});
                }
            }
        }
        for (int[] direction : DIRECTIONS) {
            int roomX = run.player.x + direction[0];
            int roomY = run.player.y + direction[1];
            if (roomX >= 0 && roomY >= 0 && roomX < run.width && roomY < run.height
                    && run.roomIds[roomY][roomX] >= 0) {
                revealRoom(run, visible, run.roomIds[roomY][roomX]);
            }
        }
        return visible;
    }

    private void revealRoom(GameRun run, boolean[][] visible, int roomId) {
        for (int y = 0; y < run.height; y++) {
            for (int x = 0; x < run.width; x++) {
                if (run.roomIds[y][x] == roomId) {
                    visible[y][x] = true;
                }
            }
        }
    }

    private int corridorDegree(GameRun run, int x, int y) {
        int degree = 0;
        for (int[] direction : DIRECTIONS) {
            if (isCorridorTile(run, x + direction[0], y + direction[1])) {
                degree++;
            }
        }
        return degree;
    }

    private boolean isCorridorTile(GameRun run, int x, int y) {
        return x >= 0 && y >= 0 && x < run.width && y < run.height
                && run.roomIds[y][x] == -1 && run.tiles[y][x] != TileType.WALL;
    }

    private GameSnapshot.WeaponView weaponView(GameRun.Weapon weapon) {
        return weapon == null ? null : new GameSnapshot.WeaponView(weapon.type(), weapon.name(), weapon.bonus(),
                weapon.type().rarity(), weapon.type().sellValue());
    }

    private GameSnapshot.ArmorView armorView(GameRun.Armor armor) {
        return armor == null ? null : new GameSnapshot.ArmorView(armor.type(), armor.name(), armor.bonus(),
                armor.type().rarity(), armor.type().sellValue());
    }

    private GameSnapshot.InventoryItemView inventoryItemView(GameRun.Item item) {
        int bonus = switch (item.type()) {
            case WEAPON -> item.weaponType().attackBonus();
            case ARMOR -> item.armorType().defenseBonus();
            default -> 0;
        };
        return new GameSnapshot.InventoryItemView(item.type().name(), item.name(), bonus,
                item.weaponType(), item.armorType(), itemRarity(item), itemSellValue(item));
    }

    private ItemRarity lootRarity(GameRun.Loot item) {
        if (item.weaponType != null) return item.weaponType.rarity();
        if (item.armorType != null) return item.armorType.rarity();
        return null;
    }

    private int lootSellValue(GameRun.Loot item) {
        if (item.weaponType != null) return item.weaponType.sellValue();
        if (item.armorType != null) return item.armorType.sellValue();
        return 0;
    }

    private ItemRarity itemRarity(GameRun.Item item) {
        if (item.weaponType() != null) return item.weaponType().rarity();
        if (item.armorType() != null) return item.armorType().rarity();
        return null;
    }

    private int itemSellValue(GameRun.Item item) {
        if (item.weaponType() != null) return item.weaponType().sellValue();
        if (item.armorType() != null) return item.armorType().sellValue();
        return 0;
    }
}
