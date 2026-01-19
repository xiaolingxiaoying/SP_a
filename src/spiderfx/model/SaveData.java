package spiderfx.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final SpiderGame game;
    private final String label;
    private final String remark;
    private final LocalDateTime saveTime;

    public SaveData(SpiderGame game, String label, String remark) {
        this.game = game;
        this.label = label;
        this.remark = remark;
        this.saveTime = LocalDateTime.now();
    }

    public SpiderGame getGame() {
        return game;
    }

    public String getLabel() {
        return label;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getSaveTime() {
        return saveTime;
    }

    public String getFormattedSaveTime() {
        return saveTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", getFormattedSaveTime(), label, remark);
    }
}
