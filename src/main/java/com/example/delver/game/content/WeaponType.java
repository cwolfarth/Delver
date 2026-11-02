package com.example.delver.game.content;

public enum WeaponType {
    WORN_BLADE("Worn blade", 0, -10, ItemRarity.COMMON, 1),
    DULL_AXE("Dull axe", 1, -5, ItemRarity.COMMON, 4),
    BRONZE_SWORD("Bronze sword", 1, 0, ItemRarity.COMMON, 6),
    ARM_SWORD("Arming sword", 2, 3, ItemRarity.COMMON, 8),
    SHORT_SPEAR("Short spear", 3, 9, ItemRarity.UNCOMMON, 15),
    LONG_SWORD("Long sword", 4, 6, ItemRarity.UNCOMMON, 25),
    KNIGHT_BLADE("Knight blade", 5, 10, ItemRarity.RARE, 48),
    HALBERD("Halberd", 7, 12, ItemRarity.RARE, 60),
    DRAGONFANG("Dragonfang", 10, 15, ItemRarity.LEGENDARY, 125);

    private final String displayName;
    private final int attackBonus;
    private final int accuracy;
    private final ItemRarity rarity;
    private final int sellValue;

    WeaponType(String displayName, int attackBonus, int accuracy, ItemRarity rarity, int sellValue) {
        this.displayName = displayName;
        this.attackBonus = attackBonus;
        this.accuracy = accuracy;
        this.rarity = rarity;
        this.sellValue = sellValue;
    }

    public String displayName() {
        return displayName;
    }

    public int attackBonus() {
        return attackBonus;
    }

    public int accuracy() {
        return accuracy;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public int sellValue() {
        return sellValue;
    }
}
