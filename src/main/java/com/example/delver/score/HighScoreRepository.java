package com.example.delver.score;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HighScoreRepository {
    private final JdbcClient jdbc;

    public HighScoreRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void save(HighScore score) {
        jdbc.sql("INSERT INTO high_scores (player_name, score, floor_reached, gold) VALUES (:name, :score, :floor, :gold)")
                .param("name", score.playerName())
                .param("score", score.score())
                .param("floor", score.floor())
                .param("gold", score.gold())
                .update();
    }

    public List<HighScore> topScores() {
        return jdbc.sql("SELECT player_name, score, floor_reached, gold, achieved_at FROM high_scores " +
                        "ORDER BY score DESC, achieved_at ASC LIMIT 10")
                .query((rs, rowNum) -> new HighScore(rs.getString("player_name"), rs.getInt("score"),
                        rs.getInt("floor_reached"), rs.getInt("gold"), rs.getObject("achieved_at", java.time.OffsetDateTime.class)))
                .list();
    }
}
