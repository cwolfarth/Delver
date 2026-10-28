package com.example.delver.game.content;

public enum ArmorType {
    CLOTH_ARMOR("Cloth armor", 0, ItemRarity.COMMON, 1),
    LEATHER_ARMOR("leather armor", 1, ItemRarity.COMMON, 6),
    CHAINMAIL("chainmail", 2, ItemRarity.UNCOMMON, 19),
    PLATE_ARMOR("plate armor", 3, ItemRarity.RARE, 46),
    DRAGON_SCALE("dragon scale", 5, ItemRarity.LEGENDARY, 120);

    private final String displayName;
    private final int defenseBonus;
    private final ItemRarity rarity;
    private final int sellValue;

    ArmorType(String displayName, int defenseBonus, ItemRarity rarity, int sellValue) {
        this.displayName = displayName;
        this.defenseBonus = defenseBonus;
        this.rarity = rarity;
        this.sellValue = sellValue;
    }

    public String displayName() {
        return displayName;
    }

    public int defenseBonus() {
        return defenseBonus;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public int sellValue() {
        return sellValue;
    }
}
