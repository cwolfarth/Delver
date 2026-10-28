package com.example.delver.game;

import com.example.delver.game.map.TileType;
import com.example.delver.score.HighScoreService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final Map<UUID, GameRun> runs = new ConcurrentHashMap<>();
    private final HighScoreService highScoreService;
    private final DungeonGenerator dungeonGenerator;
    private final GameSnapshotFactory snapshotFactory;
    private final InventoryService inventoryService;
    private final CombatService combatService;

    GameService(HighScoreService highScoreService, DungeonGenerator dungeonGenerator,
                GameSnapshotFactory snapshotFactory, InventoryService inventoryService,
                CombatService combatService) {
        this.highScoreService = highScoreService;
        this.dungeonGenerator = dungeonGenerator;
        this.snapshotFactory = snapshotFactory;
        this.inventoryService = inventoryService;
        this.combatService = combatService;
    }

    public GameSnapshot start(String requestedName) {
        String name = requestedName == null || requestedName.isBlank() ? "Delver" : requestedName.trim();
        if (name.length() > 24) {
            throw new IllegalArgumentException("Player name must be 24 characters or fewer.");
        }
        GameRun run = new GameRun(UUID.randomUUID(), name);
        dungeonGenerator.generate(run);
        run.addMessage("You enter the dungeon. Find the stairs and descend.");
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
        run.beginTurn();
        switch (action) {
            case MOVE_NORTH -> move(run, 0, -1);
            case MOVE_SOUTH -> move(run, 0, 1);
            case MOVE_EAST -> move(run, 1, 0);
            case MOVE_WEST -> move(run, -1, 0);
            case USE_POTION -> inventoryService.usePotion(run, inventorySlot);
            case EQUIP_ITEM -> inventoryService.equipItem(run, inventorySlot);
            case DISCARD_ITEM -> inventoryService.discardItem(run, inventorySlot);
            case WAIT -> run.addMessage("You wait and listen to the dungeon.");
        }
        if (!run.gameOver) {
            combatService.resolveEnemyTurn(run);
        }
        if (run.finishIfPlayerDied()) {
            highScoreService.record(run.playerName, run.score(), run.floor, run.player.gold);
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
            run.addMessage("A stone wall blocks your way.");
            return;
        }
        GameRun.Enemy enemy = run.enemyAt(x, y);
        if (enemy != null) {
            combatService.attack(run, enemy);
            return;
        }
        run.movePlayerTo(x, y);
        inventoryService.collectLoot(run);
        if (run.tiles[y][x] == TileType.STAIRS) {
            run.descend();
            dungeonGenerator.generate(run);
            run.addMessage("You descend to floor " + run.floor + ".");
        }
    }

}
