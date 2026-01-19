package spiderfx.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import spiderfx.model.Card;

public class CardView extends StackPane {
    private static final double CARD_WIDTH = 80;
    private static final double CARD_HEIGHT = 110;

    private final Card card;

    public CardView(Card card) {
        this.card = card;
        getStyleClass().add("card");
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        if (card.isFaceUp()) {
            getStyleClass().add("face-up");
            ImageView imageView = tryLoadFaceImage(card);
            if (imageView != null) {
                getChildren().add(imageView);
            } else {
                Label rankTop = new Label(card.getRankSymbol() + getSuitSymbol(card.getSuit()));
                rankTop.getStyleClass().add("card-rank");
                if (card.getSuit() == Card.Suit.HEART || card.getSuit() == Card.Suit.DIAMOND) {
                    rankTop.getStyleClass().add("card-red");
                } else {
                    rankTop.getStyleClass().add("card-black");
                }
                setAlignment(rankTop, Pos.TOP_LEFT);
                getChildren().add(rankTop);

                Label rankBottom = new Label(card.getRankSymbol() + getSuitSymbol(card.getSuit()));
                rankBottom.getStyleClass().add("card-rank");
                if (card.getSuit() == Card.Suit.HEART || card.getSuit() == Card.Suit.DIAMOND) {
                    rankBottom.getStyleClass().add("card-red");
                } else {
                    rankBottom.getStyleClass().add("card-black");
                }
                setAlignment(rankBottom, Pos.BOTTOM_RIGHT);
                getChildren().add(rankBottom);
            }
        } else {
            getStyleClass().add("face-down");
            ImageView back = tryLoadBackImage();
            if (back != null) {
                getChildren().add(back);
            }
        }
    }

    private String getSuitSymbol(Card.Suit suit) {
        switch (suit) {
            case SPADE:
                return "♠";
            case HEART:
                return "♥";
            case DIAMOND:
                return "♦";
            case CLUB:
                return "♣";
            default:
                return "?";
        }
    }

    private ImageView tryLoadFaceImage(Card card) {
        String suitName;
        switch (card.getSuit()) {
            case SPADE:
                suitName = "spade";
                break;
            case HEART:
                suitName = "heart";
                break;
            case DIAMOND:
                suitName = "diamond";
                break;
            case CLUB:
                suitName = "club";
                break;
            default:
                suitName = "spade";
        }
        String fileName = suitName + "_" + card.getRank() + ".png";
        var url = CardView.class.getResource("/cards/" + fileName);
        if (url == null) {
            return null;
        }
        Image image = new Image(url.toExternalForm(), CARD_WIDTH, CARD_HEIGHT, true, true);
        return new ImageView(image);
    }

    private ImageView tryLoadBackImage() {
        var url = CardView.class.getResource("/cards/back.png");
        if (url == null) {
            return null;
        }
        Image image = new Image(url.toExternalForm(), CARD_WIDTH, CARD_HEIGHT, true, true);
        return new ImageView(image);
    }

    public Card getCard() {
        return card;
    }
}

