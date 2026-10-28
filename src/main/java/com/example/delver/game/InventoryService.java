package com.example.delver.game;

import com.example.delver.game.content.LootType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
final class InventoryService {
    void usePotion(GameRun run, Integer requestedSlot) {
        int slot = requestedSlot == null ? firstPotionSlot(run.player.inventory) : requestedSlot;
        if (slot < 0 || slot >= run.player.inventory.size()
                || run.player.inventory.get(slot).type() != LootType.POTION) {
            run.messages.add("You have no healing potions.");
            return;
        }
        if (run.player.hp == run.player.maxHp) {
            run.messages.add("You are already at full health.");
            return;
        }
        run.player.inventory.remove(slot);
        int healed = Math.min(8, run.player.maxHp - run.player.hp);
        run.player.hp += healed;
        run.messages.add("You drink a potion and recover " + healed + " health.");
    }

    void equipItem(GameRun run, Integer slot) {
        if (isInvalidInventorySlot(run, slot)) {
            return;
        }
        GameRun.Item item = run.player.inventory.get(slot);
        switch (item.type()) {
            case WEAPON -> {
                GameRun.Weapon previous = run.player.equippedWeapon;
                run.player.equippedWeapon = new GameRun.Weapon(item.weaponType());
                run.player.inventory.set(slot, GameRun.Item.weapon(previous.type()));
                run.messages.add("You equip " + item.name() + " and stow " + previous.name() + ".");
            }
            case ARMOR -> {
                GameRun.Armor previous = run.player.equippedArmor;
                run.player.equippedArmor = new GameRun.Armor(item.armorType());
                run.player.inventory.set(slot, GameRun.Item.armor(previous.type()));
                run.messages.add("You equip " + item.name() + " and stow " + previous.name() + ".");
            }
            default -> run.messages.add("That item cannot be equipped.");
        }
    }

    void discardItem(GameRun run, Integer slot) {
        if (!isInvalidInventorySlot(run, slot)) {
            GameRun.Item discarded = run.player.inventory.remove((int) slot);
            run.messages.add("You discard " + discarded.name() + ".");
        }
    }

    void collectLoot(GameRun run) {
        List<GameRun.Loot> collected = run.loot.stream()
                .filter(item -> item.x == run.player.x && item.y == run.player.y)
                .toList();
        for (GameRun.Loot item : collected) {
            run.loot.remove(item);
            switch (item.type) {
                case GOLD -> {
                    run.player.gold += item.amount;
                    run.messages.add("You pick up " + item.amount + " gold.");
                }
                case POTION -> collectInventoryItem(run, item, GameRun.Item.potion());
                case WEAPON -> collectInventoryItem(run, item, GameRun.Item.weapon(item.weaponType));
                case ARMOR -> collectInventoryItem(run, item, GameRun.Item.armor(item.armorType));
            }
        }
    }

    private int firstPotionSlot(List<GameRun.Item> inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).type() == LootType.POTION) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isInvalidInventorySlot(GameRun run, Integer slot) {
        if (slot == null || slot < 0 || slot >= run.player.inventory.size()) {
            run.messages.add("That inventory slot is empty.");
            return true;
        }
        return false;
    }

    private void collectInventoryItem(GameRun run, GameRun.Loot loot, GameRun.Item item) {
        if (run.player.inventory.size() >= run.player.inventoryCapacity) {
            run.messages.add("Your inventory is full. Discard an item before picking this up.");
            run.loot.add(loot);
            return;
        }
        run.player.inventory.add(item);
        if (item.type() == LootType.WEAPON) {
            run.messages.add("You pick up " + item.name() + " (" + item.weaponType().rarity()
                    + ", worth " + item.weaponType().sellValue() + " gold).");
        } else if (item.type() == LootType.ARMOR) {
            run.messages.add("You pick up " + item.name() + " (" + item.armorType().rarity()
                    + ", worth " + item.armorType().sellValue() + " gold).");
        } else {
            run.messages.add("You pick up " + item.name() + ".");
        }
    }
}
