package spiderfx.view;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import spiderfx.model.Card;
import spiderfx.controller.GameController;

import java.util.List;

public class ColumnView extends Pane {
    public static final double CARD_WIDTH = 80;
    public static final double CARD_HEIGHT = 110;
    public static final double CARD_GAP = 30;
    public static final double TOP_MARGIN = 20;

    private final int columnIndex;
    private int hiddenFromIndex = Integer.MAX_VALUE;
    private double currentGap = CARD_GAP;
    private List<Card> cards;
    private GameController controller;

    public ColumnView(int columnIndex) {
        this.columnIndex = columnIndex;
        setMinWidth(CARD_WIDTH + 20);
        setPrefWidth(CARD_WIDTH + 20);
        heightProperty().addListener((obs, oldVal, newVal) -> layoutCards());
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setHiddenFromIndex(int hiddenFromIndex) {
        this.hiddenFromIndex = hiddenFromIndex;
    }

    public void render(List<Card> cards, GameController controller) {
        this.cards = cards;
        this.controller = controller;
        layoutCards();
    }

    public void playDealAnimation() {
        if (getChildren().isEmpty()) {
            return;
        }
        Node last = getChildren().get(getChildren().size() - 1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(220), last);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        TranslateTransition translate = new TranslateTransition(Duration.millis(220), last);
        translate.setFromY(-25);
        translate.setToY(0);
        new ParallelTransition(scale, translate).play();
    }

    public boolean containsScenePoint(double sceneX, double sceneY) {
        var p = sceneToLocal(sceneX, sceneY);
        var bounds = getLayoutBounds();
        return p.getX() >= bounds.getMinX() && p.getX() <= bounds.getMaxX()
                && p.getY() >= bounds.getMinY() && p.getY() <= bounds.getMaxY();
    }

    public double getCurrentGap() {
        return currentGap;
    }

    private void layoutCards() {
        getChildren().clear();
        if (cards == null || controller == null) {
            return;
        }
        double gap = CARD_GAP;
        if (!cards.isEmpty()) {
            double available = getHeight();
            if (available > 0 && cards.size() > 1) {
                double availableGap = (available - CARD_HEIGHT - TOP_MARGIN) / (cards.size() - 1);
                gap = Math.max(8, Math.min(CARD_GAP, availableGap));
            }
        }
        currentGap = gap;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardView view = new CardView(card);
            view.setLayoutX(10);
            view.setLayoutY(TOP_MARGIN + i * gap);
            if (i >= hiddenFromIndex) {
                view.setVisible(false);
            }
            final int cardIndex = i;
            view.setOnMousePressed(e -> controller.onCardPressed(columnIndex, cardIndex, e));
            getChildren().add(view);
        }
    }
}
