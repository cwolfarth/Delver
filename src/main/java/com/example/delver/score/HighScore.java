package com.example.delver.score;

import java.time.OffsetDateTime;

public record HighScore(String playerName, int score, int floor, int gold, OffsetDateTime achievedAt) {}
