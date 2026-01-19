package spiderfx.model;

import java.util.ArrayList;
import java.util.List;

public class Achievement {
    private final String id;
    private final String name;
    private final String description;
    private final AchievementCondition condition;

    public Achievement(String id, String name, String description, AchievementCondition condition) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.condition = condition;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public boolean isMet(Statistics stats, SpiderGame currentGame) {
        return condition.check(stats, currentGame);
    }

    public interface AchievementCondition {
        boolean check(Statistics stats, SpiderGame currentGame);
    }

    public static List<Achievement> getAllAchievements() {
        List<Achievement> list = new ArrayList<>();
        
        list.add(new Achievement("NOVICE", "初出茅庐", "累计开始 1 场游戏", 
            (stats, game) -> stats.getTotalGamesPlayed() >= 1));
            
        list.add(new Achievement("FIRST_WIN", "旗开得胜", "赢得第 1 场胜利", 
            (stats, game) -> stats.getTotalWins() >= 1));

        list.add(new Achievement("SINGLE_EXPERT", "单色专家", "赢得 10 场单色模式胜利", 
            (stats, game) -> stats.getWinsByMode(SpiderGame.GameMode.SINGLE_SUIT) >= 10));

        list.add(new Achievement("TWO_SUIT_PRO", "双色达人", "赢得 5 场双色模式胜利", 
            (stats, game) -> stats.getWinsByMode(SpiderGame.GameMode.TWO_SUITS) >= 5));

        list.add(new Achievement("FOUR_SUIT_KING", "四色之王", "赢得 1 场四色模式胜利", 
            (stats, game) -> stats.getWinsByMode(SpiderGame.GameMode.FOUR_SUITS) >= 1));

        list.add(new Achievement("SPEED_DEMON", "速度激情", "在 10 分钟 (600秒) 内赢得一场比赛", 
            (stats, game) -> {
                for (SpiderGame.GameMode mode : SpiderGame.GameMode.values()) {
                    if (stats.getFastestTime(mode) <= 600) return true;
                }
                return false;
            }));

        list.add(new Achievement("ECONOMIST", "精打细算", "以少于 500 步赢得一场比赛", 
            (stats, game) -> {
                // 这个需要实时检查或者在胜利时记录
                return game != null && game.isGameWon() && game.getMoves() < 500;
            }));

        list.add(new Achievement("PERSISTENT", "百折不挠", "累计移动超过 10,000 步", 
            (stats, game) -> stats.getTotalMoves() >= 10000));

        return list;
    }
}
