package com.example.delver.game.content;

public enum WeaponType {
    WORN_BLADE("Worn blade", 0, ItemRarity.COMMON, 1),
    IRON_SWORD("iron sword", 2, ItemRarity.COMMON, 8),
    STEEL_SWORD("steel sword", 3, ItemRarity.UNCOMMON, 20),
    KNIGHT_BLADE("knight blade", 5, ItemRarity.RARE, 48),
    DRAGONFANG("dragonfang", 8, ItemRarity.LEGENDARY, 125);

    private final String displayName;
    private final int attackBonus;
    private final ItemRarity rarity;
    private final int sellValue;

    WeaponType(String displayName, int attackBonus, ItemRarity rarity, int sellValue) {
        this.displayName = displayName;
        this.attackBonus = attackBonus;
        this.rarity = rarity;
        this.sellValue = sellValue;
    }

    public String displayName() {
        return displayName;
    }

    public int attackBonus() {
        return attackBonus;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public int sellValue() {
        return sellValue;
    }
}
