package com.example.delver.game;

import com.example.delver.game.map.TileType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
final class CombatService {
    private static final int HIT_CHANCE_PERCENT = 70;
    private final Random random;

    CombatService() {
        this(new Random());
    }

    CombatService(Random random) {
        this.random = random;
    }

    void attack(GameRun run, GameRun.Enemy enemy) {
        if (!rollToHit(random)) {
            run.messages.add("You miss the " + enemy.type.displayName() + ".");
            return;
        }
        int damage = run.player.attack + run.player.equippedWeapon.bonus() + random.nextInt(3);
        enemy.hp -= damage;
        run.messages.add("You strike the " + enemy.type.displayName() + " for " + damage + " damage.");
        if (enemy.hp <= 0) {
            run.enemies.remove(enemy);
            run.player.gold += enemy.type.goldReward();
            run.messages.add("The " + enemy.type.displayName() + " falls. You collect "
                    + enemy.type.goldReward() + " gold.");
        }
    }

    void resolveEnemyTurn(GameRun run) {
        for (GameRun.Enemy enemy : List.copyOf(run.enemies)) {
            if (run.player.hp <= 0) {
                break;
            }
            int distance = Math.abs(enemy.x - run.player.x) + Math.abs(enemy.y - run.player.y);
            if (distance == 1) {
                attackPlayer(run, enemy);
            } else if (distance <= 8) {
                moveEnemy(run, enemy);
            }
        }
    }

    static boolean rollToHit(Random random) {
        return random.nextInt(100) < HIT_CHANCE_PERCENT;
    }

    private void attackPlayer(GameRun run, GameRun.Enemy enemy) {
        if (!rollToHit(random)) {
            run.messages.add("The " + enemy.type.displayName() + " misses you.");
            return;
        }
        int damage = Math.max(1, enemy.type.attackPower() + random.nextInt(2)
                - run.player.equippedArmor.bonus());
        run.player.hp -= damage;
        run.messages.add("The " + enemy.type.displayName() + " hits you for " + damage + " damage.");
    }

    private void moveEnemy(GameRun run, GameRun.Enemy enemy) {
        int dx = Integer.compare(run.player.x, enemy.x);
        int dy = Integer.compare(run.player.y, enemy.y);
        int[][] choices = Math.abs(run.player.x - enemy.x) >= Math.abs(run.player.y - enemy.y)
                ? new int[][]{{dx, 0}, {0, dy}}
                : new int[][]{{0, dy}, {dx, 0}};
        for (int[] step : choices) {
            int x = enemy.x + step[0];
            int y = enemy.y + step[1];
            if (step[0] == 0 && step[1] == 0) continue;
            if (isWalkable(run, x, y) && run.enemyAt(x, y) == null
                    && !(x == run.player.x && y == run.player.y)) {
                enemy.x = x;
                enemy.y = y;
                return;
            }
        }
    }

    private boolean isWalkable(GameRun run, int x, int y) {
        return x >= 0 && y >= 0 && x < run.width && y < run.height
                && run.tiles[y][x] != TileType.WALL;
    }
}
