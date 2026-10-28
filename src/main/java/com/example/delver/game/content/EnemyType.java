package com.example.delver.game.content;

public enum EnemyType {
    GOBLIN("goblin", 7, 2, 8),
    SKELETON("skeleton", 10, 3, 12),
    ORC("orc", 14, 4, 16),
    TROLL("troll", 19, 5, 22),
    WRAITH("wraith", 25, 7, 30),
    DEMON("demon", 32, 8, 40);

    private final String displayName;
    private final int maxHp;
    private final int attackPower;
    private final int goldReward;

    EnemyType(String displayName, int maxHp, int attackPower, int goldReward) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.attackPower = attackPower;
        this.goldReward = goldReward;
    }

    public String displayName() {
        return displayName;
    }

    public int maxHp() {
        return maxHp;
    }

    public int attackPower() {
        return attackPower;
    }

    public int goldReward() {
        return goldReward;
    }
}
