package spiderfx.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StatsManager {
    private static final String STATS_FILE = "stats.dat";
    private static Statistics instance;

    public static Statistics getInstance() {
        if (instance == null) {
            loadStats();
        }
        return instance;
    }

    public static void loadStats() {
        Path path = Paths.get(STATS_FILE);
        if (Files.exists(path)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STATS_FILE))) {
                instance = (Statistics) ois.readObject();
            } catch (Exception e) {
                System.err.println("Failed to load stats: " + e.getMessage());
                instance = new Statistics();
            }
        } else {
            instance = new Statistics();
        }
    }

    public static void saveStats() {
        if (instance == null) return;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATS_FILE))) {
            oos.writeObject(instance);
        } catch (IOException e) {
            System.err.println("Failed to save stats: " + e.getMessage());
        }
    }

    public static List<String> checkAchievements(SpiderGame game) {
        List<String> newlyUnlocked = new ArrayList<>();
        List<Achievement> all = Achievement.getAllAchievements();
        Statistics stats = getInstance();

        for (Achievement a : all) {
            if (!stats.isAchievementUnlocked(a.getId()) && a.isMet(stats, game)) {
                stats.unlockAchievement(a.getId());
                newlyUnlocked.add(a.getName());
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            saveStats();
        }
        return newlyUnlocked;
    }
}
