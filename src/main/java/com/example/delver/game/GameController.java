package com.example.delver.game;

import com.example.delver.score.HighScore;
import com.example.delver.score.HighScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GameController {
    private final GameService gameService;
    private final HighScoreService highScoreService;

    public GameController(GameService gameService, HighScoreService highScoreService) {
        this.gameService = gameService;
        this.highScoreService = highScoreService;
    }

    @PostMapping("/game/runs")
    public GameSnapshot start(@RequestBody(required = false) StartRequest request) {
        return gameService.start(request == null ? null : request.playerName());
    }

    @GetMapping("/game/runs/{id}")
    public GameSnapshot get(@PathVariable UUID id) {
        return gameService.get(id);
    }

    @PostMapping("/game/runs/{id}/actions")
    public GameSnapshot act(@PathVariable UUID id, @RequestBody ActionRequest request) {
        return gameService.act(id, request.action(), request.inventorySlot());
    }

    @GetMapping("/highscores")
    public List<HighScore> highscores() {
        return highScoreService.topScores();
    }

    public record StartRequest(String playerName) {}
    public record ActionRequest(ActionType action, Integer inventorySlot) {}
}
