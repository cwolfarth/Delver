package com.example.delver.game;

import com.example.delver.game.content.ArmorSlot;
import com.example.delver.game.content.ArmorType;
import com.example.delver.game.content.EnemyType;
import com.example.delver.game.content.LootType;
import com.example.delver.game.content.WeaponType;
import com.example.delver.game.map.TileType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class GameRun {
    final UUID id;
    final String playerName;
    int floor = 1;
    int turn;
    int width;
    int height;
    TileType[][] tiles;
    int[][] roomIds;
    boolean[][] exploredTiles;
    final Player player = new Player();
    final List<Enemy> enemies = new ArrayList<>();
    final List<Loot> loot = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    boolean gameOver;

    GameRun(UUID id, String playerName) {
        this.id = id;
        this.playerName = playerName;
    }

    int score() {
        return player.gold + (floor - 1) * 100;
    }

    void beginTurn() {
        messages.clear();
        turn++;
    }

    void addMessage(String message) {
        messages.add(message);
    }

    void movePlayerTo(int x, int y) {
        player.x = x;
        player.y = y;
    }

    void descend() {
        floor++;
    }

    boolean finishIfPlayerDied() {
        if (player.hp > 0) {
            return false;
        }
        player.hp = 0;
        gameOver = true;
        addMessage("You have fallen. Your run is over.");
        return true;
    }

    Enemy enemyAt(int x, int y) {
        return enemies.stream().filter(enemy -> enemy.x == x && enemy.y == y).findFirst().orElse(null);
    }

    static final class Player {
        int x;
        int y;
        int hp = 20;
        int maxHp = 20;
        int gold;
        int attack = 4;
        int inventoryCapacity = 5;
        final List<Item> inventory = new ArrayList<>(List.of(Item.potion()));
        Weapon equippedWeapon = new Weapon(WeaponType.WORN_BLADE);
        Armor equippedArmor = new Armor(ArmorType.CLOTH_ARMOR);
        Armor equippedHelmet;
        Armor equippedGauntlets;
        Armor equippedBoots;

        Armor equippedArmor(ArmorSlot slot) {
            return switch (slot) {
                case CHEST -> equippedArmor;
                case HELMET -> equippedHelmet;
                case GAUNTLETS -> equippedGauntlets;
                case BOOTS -> equippedBoots;
            };
        }

        void equipArmor(ArmorSlot slot, Armor armor) {
            switch (slot) {
                case CHEST -> equippedArmor = armor;
                case HELMET -> equippedHelmet = armor;
                case GAUNTLETS -> equippedGauntlets = armor;
                case BOOTS -> equippedBoots = armor;
            }
        }

        int armorDefense() {
            return armorBonus(equippedArmor) + armorBonus(equippedHelmet)
                    + armorBonus(equippedGauntlets) + armorBonus(equippedBoots);
        }

        private int armorBonus(Armor armor) {
            return armor == null ? 0 : armor.bonus();
        }
    }

    static final class Enemy {
        final EnemyType type;
        int x;
        int y;
        int hp;

        Enemy(EnemyType type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = type.maxHp();
        }
    }

    static final class Loot {
        final LootType type;
        final int x;
        final int y;
        final int amount;
        final String name;
        final WeaponType weaponType;
        final ArmorType armorType;

        Loot(LootType type, int x, int y, int amount, String name) {
            this(type, x, y, amount, name, null, null);
        }

        Loot(WeaponType type, int x, int y) {
            this(LootType.WEAPON, x, y, type.attackBonus(), type.displayName(), type, null);
        }

        Loot(ArmorType type, int x, int y) {
            this(LootType.ARMOR, x, y, type.defenseBonus(), type.displayName(), null, type);
        }

        Loot(LootType type, int x, int y, int amount, String name,
             WeaponType weaponType, ArmorType armorType) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.amount = amount;
            this.name = name;
            this.weaponType = weaponType;
            this.armorType = armorType;
        }
    }

    record Weapon(WeaponType type) {
        String name() { return type.displayName(); }
        int bonus() { return type.attackBonus(); }
        int accuracy() { return type.accuracy(); }
    }

    record Armor(ArmorType type) {
        String name() { return type.displayName(); }
        int bonus() { return type.defenseBonus(); }
    }

    record Item(LootType type, String name, WeaponType weaponType, ArmorType armorType) {
        static Item potion() {
            return new Item(LootType.POTION, "healing potion", null, null);
        }

        static Item weapon(WeaponType type) {
            return new Item(LootType.WEAPON, type.displayName(), type, null);
        }

        static Item armor(ArmorType type) {
            return new Item(LootType.ARMOR, type.displayName(), null, type);
        }
    }
}
