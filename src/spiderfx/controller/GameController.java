package spiderfx.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import spiderfx.model.Card;
import spiderfx.model.SaveData;
import spiderfx.model.SpiderGame;
import spiderfx.model.StatsManager;
import spiderfx.model.Statistics;
import spiderfx.model.Achievement;
import spiderfx.view.CardView;
import spiderfx.view.ColumnView;
import spiderfx.view.GameView;

import javafx.stage.FileChooser;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GameController {
    private static final String SAVE_DIR = "saves";
    private static final int MAX_UNDO_STEPS = 50;
    private SpiderGame game;
    private final GameView view;
    private final Deque<SpiderGame> undoStack = new ArrayDeque<>();

    private int dragFromColumn = -1;
    private int dragFromIndex = -1;
    private boolean dragging = false;
    private Group dragGroup;
    private double pressSceneX;
    private double pressSceneY;
    private int lastFoundationCount;
    private Timeline timer;
    private long startMillis;

    public GameController(SpiderGame game, GameView view) {
        this.game = game;
        this.view = view;
        refreshColumns();
        lastFoundationCount = game.getFoundations().size();
        view.updateScore(game);
        initTimer();
    }

    private void initTimer() {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            game.setElapsedSeconds(game.getElapsedSeconds() + 1);
            long elapsed = game.getElapsedSeconds();
            long mm = elapsed / 60;
            long ss = elapsed % 60;
            view.setTimeText(String.format("%02d:%02d", mm, ss));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void pushUndo() {
        undoStack.push(game.copy());
        if (undoStack.size() > MAX_UNDO_STEPS) {
            undoStack.removeLast();
        }
    }

    public void installSceneHandlers(Scene scene) {
        scene.setOnMouseDragged(this::onMouseDragged);
        scene.setOnMouseReleased(this::onMouseReleased);
    }

    public void onNewGame() {
        // 创建一个自定义对话框来选择难度
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("新游戏");
        alert.setHeaderText("请选择游戏难度");
        alert.setContentText("选择花色数量：");

        ButtonType singleSuit = new ButtonType("单色 (简单)");
        ButtonType twoSuits = new ButtonType("双色 (中等)");
        ButtonType fourSuits = new ButtonType("四色 (困难)");
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(singleSuit, twoSuits, fourSuits, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != cancel) {
            SpiderGame.GameMode mode = SpiderGame.GameMode.SINGLE_SUIT;
            if (result.get() == twoSuits) {
                mode = SpiderGame.GameMode.TWO_SUITS;
            } else if (result.get() == fourSuits) {
                mode = SpiderGame.GameMode.FOUR_SUITS;
            }
            
            undoStack.clear();
            game = new SpiderGame(mode);
            dragFromColumn = -1;
            dragFromIndex = -1;
            dragging = false;
            clearDragGroup();
            ColumnView[] columns = view.getColumns();
            for (ColumnView columnView : columns) {
                columnView.setHiddenFromIndex(0);
            }
            refreshColumns();
            view.updateGameMode(mode);
            view.playNewGameAnimation();
            view.updateStatus(game);
            view.updateScore(game);
            game.setElapsedSeconds(0);
            initTimer();
            lastFoundationCount = game.getFoundations().size();
            
            // 记录统计数据
            StatsManager.getInstance().incrementGamesPlayed();
            StatsManager.saveStats();
            StatsManager.checkAchievements(game);
        }
    }

    public void onDeal() {
        pushUndo();
        if (game.dealRow()) {
            ColumnView[] columns = view.getColumns();
            for (int i = 0; i < columns.length; i++) {
                int lastIndex = game.getColumn(i).size() - 1;
                columns[i].setHiddenFromIndex(lastIndex);
            }
            refreshColumns();
            handleFoundationsChanged();
            view.playDealAnimation();
            view.updateStatus(game);
            view.updateStockCount(game);
            view.updateScore(game);
        } else {
            view.setMessage("不能发牌：牌堆不足");
        }
    }

    public void onCardPressed(int columnIndex, int cardIndex, MouseEvent event) {
        if (!game.canStartDrag(columnIndex, cardIndex)) {
            return;
        }
        dragFromColumn = columnIndex;
        dragFromIndex = cardIndex;
        dragging = true;
        pressSceneX = event.getSceneX();
        pressSceneY = event.getSceneY();
        event.consume();
    }

    private Group buildDragGroup(List<Card> sequence, double gap) {
        Group group = new Group();
        for (int i = 0; i < sequence.size(); i++) {
            Card card = sequence.get(i);
            CardView view = new CardView(card);
            view.setLayoutX(0);
            view.setLayoutY(ColumnView.TOP_MARGIN + i * gap);
            group.getChildren().add(view);
        }
        return group;
    }

    private void onMouseDragged(MouseEvent event) {
        if (!dragging) {
            return;
        }
        if (dragGroup == null) {
            double dx = event.getSceneX() - pressSceneX;
            double dy = event.getSceneY() - pressSceneY;
            if (Math.hypot(dx, dy) < 3) {
                return;
            }
            ColumnView[] columns = view.getColumns();
            columns[dragFromColumn].setHiddenFromIndex(dragFromIndex);
            refreshColumns();
            List<Card> sequence = game.getMovableSequence(dragFromColumn, dragFromIndex);
            ColumnView srcCol = view.getColumns()[dragFromColumn];
            double gap = srcCol.getCurrentGap();
            dragGroup = buildDragGroup(sequence, gap);
            dragGroup.setScaleX(1.05);
            dragGroup.setScaleY(1.05);
            dragGroup.setOpacity(0.9);
            Pane dragLayer = view.getDragLayer();
            dragLayer.getChildren().add(dragGroup);
        }
        Pane dragLayer = view.getDragLayer();
        Point2D p = dragLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
        dragGroup.setLayoutX(p.getX() - ColumnView.CARD_WIDTH / 2);
        dragGroup.setLayoutY(p.getY() - ColumnView.TOP_MARGIN);
        event.consume();
    }

    private void onMouseReleased(MouseEvent event) {
        if (!dragging) {
            return;
        }
        int targetColumn = -1;
        boolean moved = false;
        if (dragGroup != null) {
            targetColumn = findTargetColumn(event.getSceneX(), event.getSceneY());
            if (targetColumn >= 0) {
                // 在尝试移动前先保存状态，如果移动失败再处理（或者先判定能否移动）
                if (game.canDrop(dragFromColumn, dragFromIndex, targetColumn)) {
                    pushUndo();
                    if (game.moveSequence(dragFromColumn, dragFromIndex, targetColumn)) {
                        moved = true;
                    } else {
                        // 如果因为某种原因 moveSequence 失败了（理论上 canDrop 过了就不会失败），移除刚放进去的状态
                        undoStack.pop();
                    }
                }
            }
        }
        dragging = false;
        dragFromColumn = -1;
        dragFromIndex = -1;
        clearDragGroup();
        clearHidden();
        refreshColumns();
        if (moved) {
            handleFoundationsChanged();
            view.updateStatus(game);
            view.updateScore(game);
            
            // 记录步数
            StatsManager.getInstance().addMoves(1);
            StatsManager.saveStats();
        }
        event.consume();
    }

    private int findTargetColumn(double sceneX, double sceneY) {
        ColumnView[] columns = view.getColumns();
        for (ColumnView columnView : columns) {
            if (columnView.containsScenePoint(sceneX, sceneY)) {
                return columnView.getColumnIndex();
            }
        }
        return -1;
    }

    private void clearDragGroup() {
        Pane dragLayer = view.getDragLayer();
        dragLayer.getChildren().clear();
        dragGroup = null;
    }

    private void clearHidden() {
        ColumnView[] columns = view.getColumns();
        for (ColumnView columnView : columns) {
            columnView.setHiddenFromIndex(Integer.MAX_VALUE);
        }
    }

    private void refreshColumns() {
        ColumnView[] columns = view.getColumns();
        for (int i = 0; i < columns.length; i++) {
            columns[i].render(game.getColumn(i), this);
        }
        view.updateFoundations(game);
    }

    private void handleFoundationsChanged() {
        int current = game.getFoundations().size();
        if (current > lastFoundationCount) {
            int newSequences = current - lastFoundationCount;
            StatsManager.getInstance().addSequences(newSequences);
            
            for (int i = lastFoundationCount; i < current; i++) {
                view.playFoundationAnimation(i);
            }
            if (game.isGameWon() && timer != null) {
                timer.stop();
                
                // 记录胜利数据
                Statistics stats = StatsManager.getInstance();
                stats.incrementWins();
                stats.incrementWinsByMode(game.getGameMode());
                stats.updateBestScore(game.getGameMode(), game.getScore());
                stats.updateFastestTime(game.getGameMode(), game.getElapsedSeconds());
                StatsManager.saveStats();
                
                // 检查成就并提示
                List<String> unlocked = StatsManager.checkAchievements(game);
                if (!unlocked.isEmpty()) {
                    view.setMessage("达成成就: " + String.join(", ", unlocked));
                }
            } else {
                // 即使没赢也检查一下成就（有些成就跟累计数据有关）
                StatsManager.checkAchievements(game);
            }
        }
        lastFoundationCount = current;
    }

    public void onHint() {
        for (int from = 0; from < SpiderGame.COLUMN_COUNT; from++) {
            List<Card> col = game.getColumn(from);
            for (int i = 0; i < col.size(); i++) {
                if (!game.canStartDrag(from, i)) continue;
                for (int to = 0; to < SpiderGame.COLUMN_COUNT; to++) {
                    if (to == from) continue;
                    if (game.canDrop(from, i, to)) {
                        view.setMessage("可移动：从第" + (from + 1) + "列到第" + (to + 1) + "列");
                        view.showHintMove(from, i, to);
                        return;
                    }
                }
            }
        }
        if (game.canDealRow()) {
            view.setMessage("无可移动，建议点击发牌");
            view.pulseStockHint();
        } else {
            view.setMessage("无可移动且不能发牌");
        }
    }

    public void onCustomizeBackground() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择背景图片");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(view.getScene().getWindow());
        if (selectedFile != null) {
            String imageUrl = selectedFile.toURI().toString();
            view.setBackgroundImage(imageUrl);
        }
    }

    public void onSaveGame() {
        // 弹出对话框输入标签和备注
        Dialog<SaveData> dialog = new Dialog<>();
        dialog.setTitle("保存游戏");
        dialog.setHeaderText("请输入存档的标签和备注");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField labelField = new TextField();
        labelField.setPromptText("标签 (例如: 关卡1)");
        TextField remarkField = new TextField();
        remarkField.setPromptText("备注 (例如: 差一张就赢了)");

        grid.add(new Label("标签:"), 0, 0);
        grid.add(labelField, 1, 0);
        grid.add(new Label("备注:"), 0, 1);
        grid.add(remarkField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SaveData(game, labelField.getText(), remarkField.getText());
            }
            return null;
        });

        Optional<SaveData> result = dialog.showAndWait();

        result.ifPresent(saveData -> {
            try {
                Path path = Paths.get(SAVE_DIR);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
                String fileName = "save_" + System.currentTimeMillis() + ".dat";
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(SAVE_DIR, fileName)))) {
                    oos.writeObject(saveData);
                    view.setMessage("游戏已保存: " + saveData.getLabel());
                }
            } catch (IOException e) {
                view.setMessage("保存失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void onLoadGame() {
        Path path = Paths.get(SAVE_DIR);
        if (!Files.exists(path)) {
            view.setMessage("没有找到存档目录");
            return;
        }

        try {
            List<File> saveFiles = Files.list(path)
                    .filter(p -> p.toString().endsWith(".dat"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            if (saveFiles.isEmpty()) {
                view.setMessage("没有找到任何存档文件");
                return;
            }

            List<SaveData> saves = new ArrayList<>();
            List<File> validFiles = new ArrayList<>();
            for (File file : saveFiles) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    Object obj = ois.readObject();
                    if (obj instanceof SaveData) {
                        saves.add((SaveData) obj);
                        validFiles.add(file);
                    }
                } catch (Exception e) {
                    // 忽略无效存档
                }
            }

            if (saves.isEmpty()) {
                view.setMessage("没有有效的存档文件");
                return;
            }

            // 弹出对话框选择存档
            ChoiceDialog<SaveData> dialog = new ChoiceDialog<>(saves.get(0), saves);
            dialog.setTitle("加载游戏");
            dialog.setHeaderText("请选择要加载的存档");
            dialog.setContentText("选择存档:");

            Optional<SaveData> result = dialog.showAndWait();
            result.ifPresent(saveData -> {
                this.game = saveData.getGame();
                
                // 刷新UI
                dragFromColumn = -1;
                dragFromIndex = -1;
                dragging = false;
                clearDragGroup();
                clearHidden();
                refreshColumns();
                
                view.updateGameMode(game.getGameMode());
                view.updateStatus(game);
                view.updateScore(game);
                view.updateFoundations(game);
                view.updateStockCount(game);
                
                // 恢复计时器
                initTimer();
                lastFoundationCount = game.getFoundations().size();
                
                view.setMessage("已加载存档: " + saveData.getLabel());
            });

        } catch (IOException e) {
            view.setMessage("读取存档列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onUndo() {
        if (undoStack.isEmpty()) {
            view.setMessage("没有可撤销的操作");
            return;
        }

        // 创建自定义的 Apple 风格确认对话框
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("确认撤销");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // 加载 CSS
        var cssResource = getClass().getResource("/spiderfx/spider.css");
        if (cssResource != null) {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        } else {
            java.nio.file.Path cssPath = java.nio.file.Paths.get("src", "spiderfx", "spider.css");
            if (java.nio.file.Files.exists(cssPath)) {
                alert.getDialogPane().getStylesheets().add(cssPath.toUri().toString());
            }
        }

        // 主容器
        VBox root = new VBox(20);
        root.getStyleClass().add("apple-dialog");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));
        root.setPrefWidth(300);

        // 图标（可选，这里用一个圆形的问号）
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(60, 60);
        iconPane.setMaxSize(60, 60);
        iconPane.setStyle("-fx-background-color: #007AFF; -fx-background-radius: 30;");
        Label iconLabel = new Label("?");
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        iconPane.getChildren().add(iconLabel);

        // 文本
        Label contentLabel = new Label("你想好回退一步吗？");
        contentLabel.getStyleClass().add("apple-dialog-content");
        contentLabel.setWrapText(true);

        // 按钮容器
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        ButtonType okType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okType, cancelType);

        // 自定义按钮样式
        Button okButton = (Button) alert.getDialogPane().lookupButton(okType);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(cancelType);
        
        okButton.getStyleClass().add("apple-button-primary");
        cancelButton.getStyleClass().add("apple-button-secondary");

        root.getChildren().addAll(iconPane, contentLabel);
        alert.getDialogPane().setContent(root);

        // 隐藏默认的按钮栏背景，让它看起来更统一
        alert.getDialogPane().lookup(".button-bar").setStyle("-fx-background-color: transparent; -fx-padding: 10 20 20 20;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == okType) {
            SpiderGame previousState = undoStack.pop();
            this.game.restoreFrom(previousState);
            
            // 刷新UI
            dragFromColumn = -1;
            dragFromIndex = -1;
            dragging = false;
            clearDragGroup();
            clearHidden();
            refreshColumns();
            
            view.updateGameMode(game.getGameMode());
            view.updateStatus(game);
            view.updateScore(game);
            view.updateFoundations(game);
            view.updateStockCount(game);
            view.setMessage("已撤销上一步操作");
        }
    }

    public void onShowAchievements() {
        Statistics stats = StatsManager.getInstance();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成就与记录");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        
        // 主容器
        VBox root = new VBox(20);
        root.getStyleClass().add("apple-dialog");
        root.setPrefWidth(450);
        root.setPadding(new Insets(0, 0, 20, 0)); // 底部留白
        
        // 标题
        Label titleLabel = new Label("游戏记录与成就");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #1C1C1E;");
        root.getChildren().add(titleLabel);
        
        // 1. 基础统计卡片
        VBox statsCard = new VBox(10);
        statsCard.getStyleClass().add("apple-card");
        
        Label statsHeader = new Label("累计统计");
        statsHeader.getStyleClass().add("apple-header");
        
        statsCard.getChildren().add(statsHeader);
        statsCard.getChildren().addAll(
            createStatRow("累计游戏", String.valueOf(stats.getTotalGamesPlayed())),
            createStatRow("累计胜利", String.valueOf(stats.getTotalWins())),
            createStatRow("累计移动", stats.getTotalMoves() + " 步"),
            createStatRow("完成序列", stats.getTotalSequences() + " 组")
        );
        
        // 2. 最佳纪录卡片
        VBox recordsCard = new VBox(10);
        recordsCard.getStyleClass().add("apple-card");
        
        Label recordsHeader = new Label("最佳纪录");
        recordsHeader.getStyleClass().add("apple-header");
        
        recordsCard.getChildren().add(recordsHeader);
        for (SpiderGame.GameMode mode : SpiderGame.GameMode.values()) {
            String modeName = "";
            switch(mode) {
                case SINGLE_SUIT: modeName = "单色难度"; break;
                case TWO_SUITS: modeName = "双色难度"; break;
                case FOUR_SUITS: modeName = "四色难度"; break;
            }
            long time = stats.getFastestTime(mode);
            String timeStr = (time == Long.MAX_VALUE) ? "--:--" : String.format("%02d:%02d", time / 60, time % 60);
            
            HBox modeRow = new HBox(15);
            Label nameLabel = new Label(modeName);
            nameLabel.getStyleClass().add("apple-label");
            nameLabel.setPrefWidth(80);
            
            Label scoreVal = new Label("分: " + stats.getBestScore(mode));
            scoreVal.getStyleClass().add("apple-value");
            
            Label timeVal = new Label("时: " + timeStr);
            timeVal.getStyleClass().add("apple-value");
            
            modeRow.getChildren().addAll(nameLabel, scoreVal, timeVal);
            recordsCard.getChildren().add(modeRow);
        }
        
        // 3. 成就列表
        VBox achievementList = new VBox(10);
        Label achHeader = new Label("获得成就");
        achHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #1C1C1E; -fx-padding: 10 0 5 0;");
        
        VBox achievementsContainer = new VBox(8);
        List<Achievement> all = Achievement.getAllAchievements();
        for (Achievement a : all) {
            boolean unlocked = stats.isAchievementUnlocked(a.getId());
            
            HBox row = new HBox(12);
            row.getStyleClass().add("achievement-row");
            if (unlocked) {
                row.getStyleClass().add("achievement-unlocked");
            } else {
                row.getStyleClass().add("achievement-locked");
            }
            
            Label icon = new Label(unlocked ? "🏆" : "🔒");
            icon.getStyleClass().add("achievement-icon");
            
            VBox textInfo = new VBox(2);
            Label name = new Label(a.getName());
            name.getStyleClass().add("achievement-name");
            Label desc = new Label(a.getDescription());
            desc.getStyleClass().add("achievement-desc");
            
            textInfo.getChildren().addAll(name, desc);
            row.getChildren().addAll(icon, textInfo);
            achievementsContainer.getChildren().add(row);
        }
        
        root.getChildren().addAll(statsCard, recordsCard, achHeader, achievementsContainer);
        
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setMinWidth(480);
        
        // 加载 CSS
        var cssResource = getClass().getResource("/spiderfx/spider.css");
        if (cssResource != null) {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        } else {
            // 备选方案：尝试从文件系统加载
            java.nio.file.Path cssPath = java.nio.file.Paths.get("src", "spiderfx", "spider.css");
            if (java.nio.file.Files.exists(cssPath)) {
                alert.getDialogPane().getStylesheets().add(cssPath.toUri().toString());
            }
        }
        
        alert.showAndWait();
    }

    private HBox createStatRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("apple-label");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.getStyleClass().add("apple-value");
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    public void onChangeGameMode(SpiderGame.GameMode newMode) {
        if (game.getGameMode() == newMode) {
            return;
        }
        undoStack.clear();
        game = new SpiderGame(newMode);
        dragFromColumn = -1;
        dragFromIndex = -1;
        dragging = false;
        clearDragGroup();
        // 设置所有列的起始隐藏索引为0，确保新游戏发牌动画开始前牌是不可见的
        for (ColumnView columnView : view.getColumns()) {
            columnView.setHiddenFromIndex(0);
        }
        refreshColumns();
        view.updateGameMode(newMode);
        view.updateStatus(game);
        view.updateScore(game);
        view.updateFoundations(game);
        view.updateStockCount(game);
        game.setElapsedSeconds(0);
        initTimer();
        lastFoundationCount = game.getFoundations().size();
        view.playNewGameAnimation();
        
        // 记录统计数据
        StatsManager.getInstance().incrementGamesPlayed();
        StatsManager.saveStats();
        StatsManager.checkAchievements(game);
    }
}
