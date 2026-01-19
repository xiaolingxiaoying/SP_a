package spiderfx.model;

import java.io.Serializable; // 导入序列化接口

public class Card implements Serializable { // 声明 Card 类并实现 Serializable 接口，使卡片对象可被序列化
    private static final long serialVersionUID = 1L; // 定义序列化版本号
    public enum Suit { // 定义花色枚举
        SPADE,
        HEART,
        DIAMOND,
        CLUB
    }

    private final Suit suit; // 存储卡片花色
    private final int rank; // 存储卡牌点数（1-13）
    private boolean faceUp; // 标识卡牌是否正面朝上（翻开状态）

    public Card(Suit suit, int rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = false;
    }

    public Suit getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public String getRankSymbol() { // 根据点数返回对应的符号表示（A、J、Q、K 或数字字符串）
        switch (rank) {
            case 1:
                return "A";
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            default:
                return String.valueOf(rank);
        }
    }
}

