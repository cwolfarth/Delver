package com.example.delver.game.content;

public enum ArmorType {
    CLOTH_ARMOR("Cloth armor", ArmorSlot.CHEST, 0, ItemRarity.COMMON, 1),
    CLOTH_HELMET("Cloth hood", ArmorSlot.HELMET, 0, ItemRarity.COMMON, 1),
    CLOTH_GAUNTLETS("Cloth gloves", ArmorSlot.GAUNTLETS, 0, ItemRarity.COMMON, 1),
    CLOTH_BOOTS("Cloth boots", ArmorSlot.BOOTS, 0, ItemRarity.COMMON, 1),
    LEATHER_ARMOR("leather armor", ArmorSlot.CHEST, 1, ItemRarity.COMMON, 6),
    LEATHER_HELMET("leather helmet", ArmorSlot.HELMET, 1, ItemRarity.COMMON, 5),
    LEATHER_GAUNTLETS("leather gauntlets", ArmorSlot.GAUNTLETS, 1, ItemRarity.COMMON, 5),
    LEATHER_BOOTS("leather boots", ArmorSlot.BOOTS, 1, ItemRarity.COMMON, 5),
    CHAINMAIL("chainmail", ArmorSlot.CHEST, 2, ItemRarity.UNCOMMON, 19),
    CHAINMAIL_HELMET("chainmail coif", ArmorSlot.HELMET, 1, ItemRarity.UNCOMMON, 15),
    CHAINMAIL_GAUNTLETS("chainmail gauntlets", ArmorSlot.GAUNTLETS, 1, ItemRarity.UNCOMMON, 15),
    CHAINMAIL_BOOTS("chainmail boots", ArmorSlot.BOOTS, 1, ItemRarity.UNCOMMON, 15),
    PLATE_ARMOR("plate armor", ArmorSlot.CHEST, 3, ItemRarity.RARE, 46),
    PLATE_HELMET("plate helmet", ArmorSlot.HELMET, 2, ItemRarity.RARE, 36),
    PLATE_GAUNTLETS("plate gauntlets", ArmorSlot.GAUNTLETS, 2, ItemRarity.RARE, 36),
    PLATE_BOOTS("plate boots", ArmorSlot.BOOTS, 2, ItemRarity.RARE, 36),
    DRAGON_SCALE("dragon scale", ArmorSlot.CHEST, 5, ItemRarity.LEGENDARY, 120),
    DRAGON_SCALE_HELMET("dragon scale helm", ArmorSlot.HELMET, 3, ItemRarity.LEGENDARY, 95),
    DRAGON_SCALE_GAUNTLETS("dragon scale gauntlets", ArmorSlot.GAUNTLETS, 3, ItemRarity.LEGENDARY, 95),
    DRAGON_SCALE_BOOTS("dragon scale boots", ArmorSlot.BOOTS, 3, ItemRarity.LEGENDARY, 95);

    private final String displayName;
    private final ArmorSlot slot;
    private final int defenseBonus;
    private final ItemRarity rarity;
    private final int sellValue;

    ArmorType(String displayName, ArmorSlot slot, int defenseBonus, ItemRarity rarity, int sellValue) {
        this.displayName = displayName;
        this.slot = slot;
        this.defenseBonus = defenseBonus;
        this.rarity = rarity;
        this.sellValue = sellValue;
    }

    public String displayName() {
        return displayName;
    }

    public ArmorSlot slot() {
        return slot;
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
