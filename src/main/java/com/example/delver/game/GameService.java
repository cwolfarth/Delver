package com.example.delver.game;

import com.example.delver.game.map.TileType;
import com.example.delver.score.HighScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private static final int HIT_CHANCE_PERCENT = 70;
    private final Map<UUID, GameRun> runs = new ConcurrentHashMap<>();
    private final Random random;
    private final HighScoreService highScoreService;
    private final DungeonGenerator dungeonGenerator;
    private final GameSnapshotFactory snapshotFactory;
    private final InventoryService inventoryService;

    @Autowired
    GameService(HighScoreService highScoreService, DungeonGenerator dungeonGenerator,
                GameSnapshotFactory snapshotFactory, InventoryService inventoryService) {
        this(highScoreService, dungeonGenerator, snapshotFactory, inventoryService, new Random());
    }

    GameService(HighScoreService highScoreService, DungeonGenerator dungeonGenerator,
                GameSnapshotFactory snapshotFactory, InventoryService inventoryService, Random random) {
        this.highScoreService = highScoreService;
        this.dungeonGenerator = dungeonGenerator;
        this.snapshotFactory = snapshotFactory;
        this.inventoryService = inventoryService;
        this.random = random;
    }

    public GameSnapshot start(String requestedName) {
        String name = requestedName == null || requestedName.isBlank() ? "Delver" : requestedName.trim();
        if (name.length() > 24) {
            throw new IllegalArgumentException("Player name must be 24 characters or fewer.");
        }
        GameRun run = new GameRun(UUID.randomUUID(), name);
        dungeonGenerator.generate(run);
        run.messages.add("You enter the dungeon. Find the stairs and descend.");
        runs.put(run.id, run);
        return snapshotFactory.create(run);
    }

    public GameSnapshot get(UUID id) {
        GameRun run = requireRun(id);
        synchronized (run) {
            return snapshotFactory.create(run);
        }
    }

    public GameSnapshot act(UUID id, ActionType action) {
        return act(id, action, null);
    }

    public GameSnapshot act(UUID id, ActionType action, Integer inventorySlot) {
        GameRun run = requireRun(id);
        synchronized (run) {
            return advance(run, action, inventorySlot);
        }
    }

    private GameSnapshot advance(GameRun run, ActionType action, Integer inventorySlot) {
        if (run.gameOver) {
            throw new IllegalArgumentException("This run has ended. Start a new run to continue.");
        }
        if (action == null) {
            throw new IllegalArgumentException("An action is required.");
        }
        run.messages.clear();
        run.turn++;
        switch (action) {
            case MOVE_NORTH -> move(run, 0, -1);
            case MOVE_SOUTH -> move(run, 0, 1);
            case MOVE_EAST -> move(run, 1, 0);
            case MOVE_WEST -> move(run, -1, 0);
            case USE_POTION -> inventoryService.usePotion(run, inventorySlot);
            case EQUIP_ITEM -> inventoryService.equipItem(run, inventorySlot);
            case DISCARD_ITEM -> inventoryService.discardItem(run, inventorySlot);
            case WAIT -> run.messages.add("You wait and listen to the dungeon.");
        }
        if (!run.gameOver) {
            resolveEnemies(run);
        }
        if (run.player.hp <= 0) {
            run.player.hp = 0;
            run.gameOver = true;
            run.messages.add("You have fallen. Your run is over.");
            highScoreService.record(run.playerName, score(run), run.floor, run.player.gold);
        }
        return snapshotFactory.create(run);
    }

    private GameRun requireRun(UUID id) {
        GameRun run = runs.get(id);
        if (run == null) {
            throw new RunNotFoundException(id);
        }
        return run;
    }

    private void move(GameRun run, int dx, int dy) {
        int x = run.player.x + dx;
        int y = run.player.y + dy;
        if (x < 0 || y < 0 || x >= run.width || y >= run.height || run.tiles[y][x] == TileType.WALL) {
            run.messages.add("A stone wall blocks your way.");
            return;
        }
        GameRun.Enemy enemy = enemyAt(run, x, y);
        if (enemy != null) {
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
            return;
        }
        run.player.x = x;
        run.player.y = y;
        inventoryService.collectLoot(run);
        if (run.tiles[y][x] == TileType.STAIRS) {
            run.floor++;
            dungeonGenerator.generate(run);
            run.messages.add("You descend to floor " + run.floor + ".");
        }
    }

    private void resolveEnemies(GameRun run) {
        for (GameRun.Enemy enemy : List.copyOf(run.enemies)) {
            if (run.player.hp <= 0) {
                break;
            }
            int distance = Math.abs(enemy.x - run.player.x) + Math.abs(enemy.y - run.player.y);
            if (distance == 1) {
                if (!rollToHit(random)) {
                    run.messages.add("The " + enemy.type.displayName() + " misses you.");
                    continue;
                }
                int damage = Math.max(1, enemy.type.attackPower() + random.nextInt(2)
                        - run.player.equippedArmor.bonus());
                run.player.hp -= damage;
                run.messages.add("The " + enemy.type.displayName() + " hits you for " + damage + " damage.");
            } else if (distance <= 8) {
                moveEnemy(run, enemy);
            }
        }
    }

    static boolean rollToHit(Random random) {
        return random.nextInt(100) < HIT_CHANCE_PERCENT;
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
            if (isWalkable(run, x, y) && enemyAt(run, x, y) == null
                    && !(x == run.player.x && y == run.player.y)) {
                enemy.x = x;
                enemy.y = y;
                return;
            }
        }
    }

    private GameRun.Enemy enemyAt(GameRun run, int x, int y) {
        return run.enemies.stream().filter(enemy -> enemy.x == x && enemy.y == y).findFirst().orElse(null);
    }

    private boolean isWalkable(GameRun run, int x, int y) {
        return x >= 0 && y >= 0 && x < run.width && y < run.height && run.tiles[y][x] != TileType.WALL;
    }

    private int score(GameRun run) {
        return run.player.gold + (run.floor - 1) * 100;
    }
}
