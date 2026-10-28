package com.example.delver.score;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class HighScoreService {
    private final HighScoreRepository repository;

    public HighScoreService(HighScoreRepository repository) {
        this.repository = repository;
    }

    public void record(String playerName, int score, int floor, int gold) {
        repository.save(new HighScore(playerName, score, floor, gold, OffsetDateTime.now()));
    }

    public List<HighScore> topScores() {
        return repository.topScores();
    }
}
