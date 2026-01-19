package spiderfx.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import javafx.util.Duration;
import spiderfx.controller.GameController;
import spiderfx.model.Card;
import spiderfx.model.SpiderGame;

public class GameView extends StackPane {
    private final BorderPane board;
    private final Pane dragLayer;
    private final ColumnView[] columns;
    private final FoundationView[] foundations;
    private final Button newGameButton;
    private final Button undoButton;
    private final Button hintButton;
    private final Button dealButton;
    private final Button saveButton;
    private final Button loadButton;
    private final Button achievementButton; // 新增：成就/记录按钮
    private final Button customizeButton; // 新增：自定义背景
    private final MenuButton gameModeMenuButton;
    private final MenuItem singleSuitItem;
    private final MenuItem twoSuitsItem;
    private final MenuItem fourSuitsItem;
    private final StackPane stockPileView; // 新增：可视化发牌堆
    private final Label titleLabel;
    private final Label statusLabel;
    private final Label scoreLabel;
    private final Label timeLabel;
    private Label stockCountLabel; // 新增：显示剩余牌数

    public GameView(SpiderGame game) {
        this.board = new BorderPane();
        this.dragLayer = new Pane();
        dragLayer.setPickOnBounds(false);

        getChildren().addAll(board, dragLayer);

        newGameButton = new Button("新游戏");
        newGameButton.getStyleClass().add("primary-button");
        undoButton = new Button("撤销");
        undoButton.getStyleClass().add("primary-button");
        hintButton = new Button("提示");
        hintButton.getStyleClass().add("primary-button");
        dealButton = new Button("发牌");
        dealButton.getStyleClass().add("primary-button");

        saveButton = new Button("保存游戏");
        saveButton.getStyleClass().add("primary-button");
        loadButton = new Button("加载游戏");
        loadButton.getStyleClass().add("primary-button");

        achievementButton = new Button("成就与记录");
        achievementButton.getStyleClass().add("primary-button");

        customizeButton = new Button("✎");
        customizeButton.getStyleClass().add("customize-button");
        // 使用一个简单的画笔/齿轮图标占位，或者留空由CSS设置图标
        customizeButton.setTooltip(new javafx.scene.control.Tooltip("自定义背景"));

        singleSuitItem = new MenuItem("单花色");
        twoSuitsItem = new MenuItem("双花色");
        fourSuitsItem = new MenuItem("四花色");

        gameModeMenuButton = new MenuButton("选择难度");
        gameModeMenuButton.getItems().addAll(singleSuitItem, twoSuitsItem, fourSuitsItem);
        gameModeMenuButton.getStyleClass().add("primary-button");
        
        // 更新按钮文字以反映当前模式
        updateMenuButtonText(game.getGameMode());

        titleLabel = new Label();
        titleLabel.getStyleClass().add("title-label");
        updateTitleForGameMode(game.getGameMode());
        // 新增：单独给标题一个HBox和足够空间，避免被裁剪
        HBox titleBox = new HBox(titleLabel);
        titleBox.setMinWidth(370); // 宽度根据字号调整
        titleBox.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");

        scoreLabel = new Label("得分: 0");
        scoreLabel.getStyleClass().add("score-label");

        timeLabel = new Label("时间: 00:00");
        timeLabel.getStyleClass().add("time-label");

        // 游戏状态信息栏
        HBox infoBar = new HBox(40, titleBox, statusLabel, scoreLabel, timeLabel);
        infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setPadding(new Insets(8, 50, 4, 50));
        infoBar.setMinHeight(60);
        infoBar.getStyleClass().add("top-info-bar");

        // 顶部菜单栏 - 包含所有控制按键，类似VSCode风格
        HBox menuBar = new HBox(12);
        menuBar.setAlignment(Pos.TOP_LEFT);
        menuBar.setPadding(new Insets(12, 50, 12, 50));
        menuBar.getStyleClass().add("menu-bar");
        
        // 添加控制按键到菜单栏
        menuBar.getChildren().add(newGameButton);
        menuBar.getChildren().add(undoButton);
        
        // 剩余发牌次数显示
        this.stockCountLabel = new Label();
        stockCountLabel.getStyleClass().add("stock-count-label");
        
        // 发牌按钮组合
        VBox dealBox = new VBox(2, dealButton);
        dealBox.setAlignment(Pos.TOP_CENTER);
        menuBar.getChildren().add(dealBox);

        menuBar.getChildren().add(saveButton);
        menuBar.getChildren().add(loadButton);
        menuBar.getChildren().add(achievementButton);
        menuBar.getChildren().add(hintButton);
        
        menuBar.getChildren().add(gameModeMenuButton);
        
        // 可视化发牌堆
        stockPileView = new StackPane();
        stockPileView.getStyleClass().add("stock-pile-view");
        stockPileView.setPrefSize(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
        stockPileView.setMinSize(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
        // 初始添加几张背面牌的视觉效果
        for (int i = 0; i < 3; i++) {
            Region cardBack = new Region();
            cardBack.getStyleClass().add("card-back-visual");
            cardBack.setTranslateX(i * 2);
            cardBack.setTranslateY(i * 2);
            stockPileView.getChildren().add(cardBack);
        }
        
        // 将剩余次数标签添加到发牌堆上，并设置为默认隐藏
        stockCountLabel.setVisible(false);
        stockCountLabel.setMouseTransparent(true); // 避免干扰点击
        stockPileView.getChildren().add(stockCountLabel);
        StackPane.setAlignment(stockCountLabel, Pos.CENTER);
        
        // 鼠标悬停显示剩余次数
        stockPileView.setOnMouseEntered(e -> {
            stockCountLabel.toFront();
            stockCountLabel.setVisible(true);
        });
        stockPileView.setOnMouseExited(e -> stockCountLabel.setVisible(false));
        
        // 点击牌堆也可以发牌（增强交互）
        stockPileView.setCursor(javafx.scene.Cursor.HAND);
        stockPileView.setOnMouseClicked(e -> {
            if (dealButton.getOnAction() != null) {
                dealButton.getOnAction().handle(new javafx.event.ActionEvent());
            }
        });
        
        // 牌堆区域 - 显示8个完成牌堆的位置
        foundations = new FoundationView[8];
        HBox foundationBox = new HBox(15);
        foundationBox.setAlignment(Pos.CENTER);
        foundationBox.setPadding(new Insets(10, 20, 10, 20));
        foundationBox.getStyleClass().add("foundation-box");
        
        // 将发牌堆添加到完成牌堆左侧，并增加间距
        HBox stockWrapper = new HBox(stockPileView);
        stockWrapper.setPadding(new Insets(0, 40, 0, 0)); // 与完成牌堆保持距离
        foundationBox.getChildren().add(stockWrapper);

        for (int i = 0; i < foundations.length; i++) {
            FoundationView view = new FoundationView(i);
            foundations[i] = view;
            foundationBox.getChildren().add(view);
        }
        
        // 顶部布局：信息栏 + 菜单栏 + 牌堆区域
        VBox topWrapper = new VBox(4);
        topWrapper.getChildren().add(infoBar);
        topWrapper.getChildren().add(menuBar);
        topWrapper.getChildren().add(foundationBox);
        board.setTop(topWrapper);

        HBox columnsBox = new HBox(10);
        columnsBox.setPadding(new Insets(20, 50, 40, 50));
        columnsBox.setAlignment(Pos.TOP_CENTER);
        columnsBox.setFillHeight(true);
        columnsBox.getStyleClass().add("columns-box");
        columns = new ColumnView[SpiderGame.COLUMN_COUNT];
        for (int i = 0; i < SpiderGame.COLUMN_COUNT; i++) {
            ColumnView columnView = new ColumnView(i);
            columns[i] = columnView;
            columnsBox.getChildren().add(columnView);
        }
        columnsBox.heightProperty().addListener((obs, o, n) -> {
            double h = n.doubleValue() - columnsBox.getPadding().getTop() - columnsBox.getPadding().getBottom();
            if (h <= 0) return;
            for (ColumnView c : columns) {
                c.setMinHeight(h);
                c.setPrefHeight(h);
            }
        });
        columnsBox.widthProperty().addListener((obs, oldVal, newVal) -> updateColumnLayout(columnsBox));
        javafx.application.Platform.runLater(() -> updateColumnLayout(columnsBox));
        board.setCenter(columnsBox);

        updateStatus(game);
        updateFoundations(game);
        updateStockCount(game);

        // 添加自定义背景按钮到右下角
        StackPane.setAlignment(customizeButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(customizeButton, new Insets(0, 20, 20, 0));
        getChildren().add(customizeButton);
    }

    public void bindController(GameController controller) {
        newGameButton.setOnAction(e -> controller.onNewGame());
        undoButton.setOnAction(e -> controller.onUndo());
        saveButton.setOnAction(e -> controller.onSaveGame());
        loadButton.setOnAction(e -> controller.onLoadGame());
        achievementButton.setOnAction(e -> controller.onShowAchievements());
        customizeButton.setOnAction(e -> controller.onCustomizeBackground());
        dealButton.setOnAction(e -> controller.onDeal());
        hintButton.setOnAction(e -> controller.onHint());
        
        singleSuitItem.setOnAction(e -> {
            controller.onChangeGameMode(SpiderGame.GameMode.SINGLE_SUIT);
            updateMenuButtonText(SpiderGame.GameMode.SINGLE_SUIT);
        });
        twoSuitsItem.setOnAction(e -> {
            controller.onChangeGameMode(SpiderGame.GameMode.TWO_SUITS);
            updateMenuButtonText(SpiderGame.GameMode.TWO_SUITS);
        });
        fourSuitsItem.setOnAction(e -> {
            controller.onChangeGameMode(SpiderGame.GameMode.FOUR_SUITS);
            updateMenuButtonText(SpiderGame.GameMode.FOUR_SUITS);
        });
    }

    private void updateMenuButtonText(SpiderGame.GameMode mode) {
        switch (mode) {
            case SINGLE_SUIT: gameModeMenuButton.setText("单花色"); break;
            case TWO_SUITS: gameModeMenuButton.setText("双花色"); break;
            case FOUR_SUITS: gameModeMenuButton.setText("四花色"); break;
        }
    }

    public ColumnView[] getColumns() {
        return columns;
    }

    public FoundationView[] getFoundations() {
        return foundations;
    }

    public Pane getDragLayer() {
        return dragLayer;
    }

    public void setBackgroundImage(String url) {
        if (url == null || url.isEmpty()) {
            setBackground(null);
            return;
        }
        try {
            Image image = new Image(url);
            if (image.isError()) {
                throw new Exception("Image loading error");
            }
            BackgroundImage bgImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true) // Fill/Cover
            );
            setBackground(new Background(bgImage));
        } catch (Exception e) {
            System.err.println("无法加载背景图片: " + e.getMessage());
        }
    }

    public void updateStatus(SpiderGame game) {
        if (game.isGameWon()) {
            statusLabel.setText("恭喜通关");
        } else {
            statusLabel.setText("已完成 " + game.getCompletedSequences() + " 组");
        }
        updateStockCount(game);
    }

    public void updateScore(SpiderGame game) {
        scoreLabel.setText("得分: " + game.getScore());
    }

    public void setTimeText(String text) {
        timeLabel.setText("时间: " + text);
    }

    // 新增：刷新剩余发牌显示
    public void updateStockCount(SpiderGame game) {
        if (stockCountLabel == null) return;
        int remaining = game.getStock().size() / SpiderGame.COLUMN_COUNT;
        stockCountLabel.setText(String.valueOf(remaining));
        
        // 更新可视化牌堆的厚度
        stockPileView.getChildren().removeIf(node -> node.getStyleClass().contains("card-back-visual"));
        int visualCards = Math.min(remaining, 5);
        for (int i = 0; i < visualCards; i++) {
            Region cardBack = new Region();
            cardBack.getStyleClass().add("card-back-visual");
            cardBack.setTranslateX(i * 2);
            cardBack.setTranslateY(i * 2);
            stockPileView.getChildren().add(0, cardBack);
        }
        stockPileView.setVisible(remaining > 0);
    }

    public void updateFoundations(SpiderGame game) {
        for (int i = 0; i < foundations.length; i++) {
            Card top = null;
            if (i < game.getFoundations().size()) {
                var sequence = game.getFoundations().get(i);
                if (!sequence.isEmpty()) {
                    top = sequence.get(sequence.size() - 1);
                }
            }
            foundations[i].setSequenceTop(top);
        }
    }

    public void playFoundationAnimation(int index) {
        if (index < 0 || index >= foundations.length) {
            return;
        }
        FoundationView view = foundations[index];
        FadeTransition fade = new FadeTransition(Duration.millis(260), view);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(260), view);
        scale.setFromX(0.85);
        scale.setFromY(0.85);
        scale.setToX(1.0);
        scale.setToY(1.0);
        new ParallelTransition(fade, scale).play();
    }

    public void playDealAnimation() {
        // 流行的发牌动画：从 stockView 匀速飞入每个 column 尾部
        javafx.application.Platform.runLater(() -> {
            for (int i = 0; i < columns.length; i++) {
                ColumnView column = columns[i];
                if (column.getChildren().isEmpty()) continue;
                Node targetCard = column.getChildren().get(column.getChildren().size() - 1);
                // 创建临时飞行动画卡片
                CardView temp = new CardView(((CardView) targetCard).getCard());
                temp.setOpacity(0.95);
                temp.setScaleX(0.62); temp.setScaleY(0.62);
                dragLayer.getChildren().add(temp);
                // 动画起点（发牌堆中心）
                Bounds start = stockPileView.localToScene(stockPileView.getBoundsInLocal());
                Point2D pointStart = dragLayer.sceneToLocal(start.getMinX() + start.getWidth() / 2, start.getMinY() + start.getHeight() / 2);
                // 动画终点
                Bounds end = targetCard.localToScene(targetCard.getBoundsInLocal());
                Point2D pointEnd = dragLayer.sceneToLocal(end.getMinX(), end.getMinY());
                temp.setLayoutX(pointStart.getX());
                temp.setLayoutY(pointStart.getY());
                // 动画：位移+缩放+淡入
                TranslateTransition tt = new TranslateTransition(Duration.millis(390), temp);
                tt.setFromX(0); tt.setFromY(0);
                tt.setToX(pointEnd.getX() - pointStart.getX());
                tt.setToY(pointEnd.getY() - pointStart.getY());
                ScaleTransition st = new ScaleTransition(Duration.millis(230), temp);
                st.setFromX(0.62); st.setFromY(0.62);
                st.setToX(1.0); st.setToY(1.0);
                FadeTransition ft = new FadeTransition(Duration.millis(315), temp);
                ft.setFromValue(0.22); ft.setToValue(1.0);
                ParallelTransition pt = new ParallelTransition(tt, st, ft);
                pt.setDelay(Duration.millis(i * 47));
                pt.setOnFinished(e2 -> {
                    dragLayer.getChildren().remove(temp);
                    targetCard.setVisible(true);
                    column.setHiddenFromIndex(Integer.MAX_VALUE);
                    column.playDealAnimation();
                });
                pt.play();
            }
        });
    }

    public void playNewGameAnimation() {
        javafx.application.Platform.runLater(() -> {
            int rounds = 0;
            for (ColumnView column : columns) {
                rounds = Math.max(rounds, column.getChildren().size());
            }
            for (int r = 0; r < rounds; r++) {
                for (int i = 0; i < columns.length; i++) {
                    ColumnView column = columns[i];
                    if (r >= column.getChildren().size()) {
                        continue;
                    }
                    Node targetCard = column.getChildren().get(r);
                    CardView temp = new CardView(((CardView) targetCard).getCard());
                    temp.setOpacity(0.95);
                    temp.setScaleX(0.6);
                    temp.setScaleY(0.6);
                    dragLayer.getChildren().add(temp);
                    // 动画起点（发牌堆中心）
                    Bounds start = stockPileView.localToScene(stockPileView.getBoundsInLocal());
                    Point2D pStart = dragLayer.sceneToLocal(
                            start.getMinX() + start.getWidth() / 2,
                            start.getMinY() + start.getHeight() / 2
                    );
                    Bounds end = targetCard.localToScene(targetCard.getBoundsInLocal());
                    Point2D pEnd = dragLayer.sceneToLocal(end.getMinX(), end.getMinY());
                    temp.setLayoutX(pStart.getX());
                    temp.setLayoutY(pStart.getY());
                    TranslateTransition tt = new TranslateTransition(Duration.millis(360), temp);
                    tt.setFromX(0);
                    tt.setFromY(0);
                    tt.setToX(pEnd.getX() - pStart.getX());
                    tt.setToY(pEnd.getY() - pStart.getY());
                    ScaleTransition st = new ScaleTransition(Duration.millis(220), temp);
                    st.setFromX(0.6);
                    st.setFromY(0.6);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    FadeTransition ft = new FadeTransition(Duration.millis(300), temp);
                    ft.setFromValue(0.2);
                    ft.setToValue(1.0);
                    ParallelTransition pt = new ParallelTransition(tt, st, ft);
                    pt.setDelay(Duration.millis(r * 120L + i * 40L));
                    pt.setOnFinished(e -> {
                        dragLayer.getChildren().remove(temp);
                        targetCard.setVisible(true);
                    });
                    pt.play();
                }
            }
        });
    }

    public void setMessage(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-label-success", "status-label-error");
        if (message.contains("不能")==true || message.contains("不足") || message.contains("空列")) {
            if (!statusLabel.getStyleClass().contains("status-label-error")) statusLabel.getStyleClass().add("status-label-error");
        } else if (message.contains("恭喜")||message.contains("成功")) {
            if (!statusLabel.getStyleClass().contains("status-label-success")) statusLabel.getStyleClass().add("status-label-success");
        }
    }

    public void showHintMove(int fromColumn, int startIndex, int toColumn) {
        ColumnView src = columns[fromColumn];
        ColumnView dst = columns[toColumn];
        if (startIndex < 0 || startIndex >= src.getChildren().size()) return;
        Group ghost = new Group();
        double gap = src.getCurrentGap();
        for (int i = startIndex; i < src.getChildren().size(); i++) {
            Node n = src.getChildren().get(i);
            CardView cv = (CardView) n;
            CardView copy = new CardView(cv.getCard());
            copy.setLayoutX(0);
            copy.setLayoutY(ColumnView.TOP_MARGIN + (i - startIndex) * gap);
            ghost.getChildren().add(copy);
        }
        dragLayer.getChildren().add(ghost);
        Bounds start = src.localToScene(src.getBoundsInLocal());
        Point2D pStart = dragLayer.sceneToLocal(start.getMinX() + 10, start.getMinY() + ColumnView.TOP_MARGIN + startIndex * gap);
        Bounds endBounds;
        if (dst.getChildren().isEmpty()) {
            endBounds = dst.localToScene(dst.getBoundsInLocal());
        } else {
            Node last = dst.getChildren().get(dst.getChildren().size() - 1);
            endBounds = last.localToScene(last.getBoundsInLocal());
        }
        Point2D pEnd = dragLayer.sceneToLocal(endBounds.getMinX() + 10, endBounds.getMinY() + (dst.getChildren().isEmpty() ? ColumnView.TOP_MARGIN : 0));
        ghost.setLayoutX(pStart.getX());
        ghost.setLayoutY(pStart.getY());
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), ghost);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(pEnd.getX() - pStart.getX());
        tt.setToY(pEnd.getY() - pStart.getY());
        ScaleTransition st = new ScaleTransition(Duration.millis(260), ghost);
        st.setFromX(0.95);
        st.setFromY(0.95);
        st.setToX(1.0);
        st.setToY(1.0);
        FadeTransition ft = new FadeTransition(Duration.millis(260), ghost);
        ft.setFromValue(0.3);
        ft.setToValue(0.95);
        ParallelTransition pt = new ParallelTransition(tt, st, ft);
        pt.setOnFinished(e -> dragLayer.getChildren().remove(ghost));
        pt.play();
    }

    public void pulseStockHint() {
        ScaleTransition st = new ScaleTransition(Duration.millis(280), dealButton);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.08); st.setToY(1.08);
        FadeTransition ft = new FadeTransition(Duration.millis(280), dealButton);
        ft.setFromValue(1.0); ft.setToValue(0.8);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setAutoReverse(true);
        pt.setCycleCount(2);
        pt.play();
    }

    private void updateColumnLayout(HBox columnsBox) {
        if (columns == null || columns.length == 0) {
            return;
        }
        double paddingLeft = columnsBox.getPadding().getLeft();
        double paddingRight = columnsBox.getPadding().getRight();
        double available = columnsBox.getWidth() - paddingLeft - paddingRight;
        if (available <= 0) {
            return;
        }
        int count = columns.length;
        double desiredWidth = ColumnView.CARD_WIDTH + 20;
        double minWidth = ColumnView.CARD_WIDTH + 6;
        double minSpacing = 4.0;
        double maxColumnsWidth = available - (count - 1) * minSpacing;
        if (maxColumnsWidth <= 0) {
            return;
        }
        double columnWidth = desiredWidth;
        if (maxColumnsWidth < columnWidth * count) {
            columnWidth = Math.max(minWidth, maxColumnsWidth / count);
        }
        double spacing;
        if (count == 1) {
            spacing = 0;
        } else {
            spacing = Math.max(minSpacing, (available - columnWidth * count) / (count - 1));
        }
        columnsBox.setSpacing(spacing);
        for (ColumnView column : columns) {
            column.setMinWidth(columnWidth);
            column.setPrefWidth(columnWidth);
        }
    }

    private void updateTitleForGameMode(SpiderGame.GameMode mode) {
        // 固定显示"蜘蛛纸牌"，不根据模式变化
        titleLabel.setText("蜘蛛纸牌");
    }

    public void updateGameMode(SpiderGame.GameMode mode) {
        updateTitleForGameMode(mode);
        updateMenuButtonText(mode);
    }
}
