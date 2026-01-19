package spiderfx.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Statistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalGamesPlayed = 0;
    private int totalWins = 0;
    private long totalMoves = 0;
    private int totalSequences = 0;
    
    // 不同难度的最高分
    private Map<SpiderGame.GameMode, Integer> bestScores = new HashMap<>();
    // 不同难度的最快时间 (秒)
    private Map<SpiderGame.GameMode, Long> fastestTimes = new HashMap<>();
    // 不同难度的胜场数
    private Map<SpiderGame.GameMode, Integer> winsByMode = new HashMap<>();

    // 已获得的成就ID列表
    private Map<String, Long> unlockedAchievements = new HashMap<>(); // ID -> 解锁时间戳

    public Statistics() {
        for (SpiderGame.GameMode mode : SpiderGame.GameMode.values()) {
            bestScores.put(mode, 0);
            fastestTimes.put(mode, Long.MAX_VALUE);
            winsByMode.put(mode, 0);
        }
    }

    // Getters and Setters
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void incrementGamesPlayed() { totalGamesPlayed++; }

    public int getTotalWins() { return totalWins; }
    public void incrementWins() { totalWins++; }

    public long getTotalMoves() { return totalMoves; }
    public void addMoves(int moves) { totalMoves += moves; }

    public int getTotalSequences() { return totalSequences; }
    public void addSequences(int count) { totalSequences += count; }

    public int getBestScore(SpiderGame.GameMode mode) { return bestScores.getOrDefault(mode, 0); }
    public void updateBestScore(SpiderGame.GameMode mode, int score) {
        if (score > getBestScore(mode)) {
            bestScores.put(mode, score);
        }
    }

    public long getFastestTime(SpiderGame.GameMode mode) { return fastestTimes.getOrDefault(mode, Long.MAX_VALUE); }
    public void updateFastestTime(SpiderGame.GameMode mode, long seconds) {
        if (seconds < getFastestTime(mode)) {
            fastestTimes.put(mode, seconds);
        }
    }

    public int getWinsByMode(SpiderGame.GameMode mode) { return winsByMode.getOrDefault(mode, 0); }
    public void incrementWinsByMode(SpiderGame.GameMode mode) {
        winsByMode.put(mode, getWinsByMode(mode) + 1);
    }

    public Map<String, Long> getUnlockedAchievements() { return unlockedAchievements; }
    public boolean isAchievementUnlocked(String id) { return unlockedAchievements.containsKey(id); }
    public void unlockAchievement(String id) {
        if (!unlockedAchievements.containsKey(id)) {
            unlockedAchievements.put(id, System.currentTimeMillis());
        }
    }
}
