package com.example.delver.game;

import com.example.delver.game.content.ArmorType;
import com.example.delver.game.content.EnemyType;
import com.example.delver.game.content.ItemRarity;
import com.example.delver.game.content.WeaponType;
import com.example.delver.game.map.TileType;

import java.util.List;
import java.util.UUID;

public record GameSnapshot(
        UUID runId,
        String playerName,
        int floor,
        int turn,
        int width,
        int height,
        List<List<TileType>> map,
        PlayerView player,
        List<EnemyView> enemies,
        List<LootView> loot,
        List<String> messages,
        boolean gameOver,
        int score
) {
    public record PlayerView(int x, int y, int hp, int maxHp, int gold, int attack,
                             WeaponView equippedWeapon, ArmorView equippedArmor,
                             List<InventoryItemView> inventory, int inventoryCapacity) {
    }

    public record EnemyView(EnemyType type, String name, int x, int y, int hp) {
    }

    public record LootView(String type, String name, int x, int y, int amount,
                           WeaponType weaponType, ArmorType armorType, ItemRarity rarity, int sellValue) {
    }

    public record WeaponView(WeaponType type, String name, int bonus, ItemRarity rarity, int sellValue) {
    }

    public record ArmorView(ArmorType type, String name, int bonus, ItemRarity rarity, int sellValue) {
    }

    public record InventoryItemView(String type, String name, int bonus,
                                    WeaponType weaponType, ArmorType armorType, ItemRarity rarity, int sellValue) {
    }
}
