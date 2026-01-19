package spiderfx.view;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import spiderfx.model.Card;

public class FoundationView extends StackPane {
    private final int index;

    public FoundationView(int index) {
        this.index = index;
        setMinSize(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
        setPrefSize(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
        getStyleClass().add("foundation-slot");
        Rectangle placeholder = new Rectangle(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
        placeholder.setArcWidth(10);
        placeholder.setArcHeight(10);
        placeholder.setFill(Color.TRANSPARENT);
        placeholder.setStrokeWidth(1.5);
        placeholder.setStroke(Color.rgb(255, 255, 255, 0.45));
        getChildren().add(placeholder);
    }

    public int getIndex() {
        return index;
    }

    public void setSequenceTop(Card card) {
        getChildren().clear();
        if (card == null) {
            Rectangle placeholder = new Rectangle(ColumnView.CARD_WIDTH, ColumnView.CARD_HEIGHT);
            placeholder.setArcWidth(10);
            placeholder.setArcHeight(10);
            placeholder.setFill(Color.TRANSPARENT);
            placeholder.setStrokeWidth(1.5);
            placeholder.setStroke(Color.rgb(255, 255, 255, 0.45));
            getChildren().add(placeholder);
        } else {
            CardView view = new CardView(card);
            getChildren().add(view);
        }
    }
}

