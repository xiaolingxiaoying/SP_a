package spiderfx.model;
// 导入输入输出流、集合框架相关类
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpiderGame implements Serializable { // 声明类实现序列化接口
    private static final long serialVersionUID = 1L; // 定义序列化版本号
    public static final int COLUMN_COUNT = 10; // 定义列数常量（10列
    public static final int COMPLETE_SEQUENCE_LENGTH = 13; // 定义完成序列长度常量（13张牌）

    public enum GameMode { // 定义三种游戏难度：单花色、双花色、四花色
        SINGLE_SUIT,
        TWO_SUITS,
        FOUR_SUITS
    }

    private List<List<Card>> columns;      // 10列纸牌
    private List<Card> stock;              // 牌堆
    private GameMode gameMode;             // 游戏模式
    private int completedSequences;        // 已完成序列数
    private List<List<Card>> foundations;  // 基础牌堆
    private int score;                     // 得分
    private int moves;                     // 移动次数
    private int deals;                     // 发牌次数
    private long elapsedSeconds;           // 经过秒数

    public SpiderGame(GameMode gameMode) {
        this.gameMode = gameMode;
        this.columns = new ArrayList<>();
        for (int i = 0; i < COLUMN_COUNT; i++) {
            columns.add(new ArrayList<>());
        }
        this.stock = new ArrayList<>();
        this.foundations = new ArrayList<>();
        newGame();
    }

    public SpiderGame() {
        this(GameMode.SINGLE_SUIT);
    } // 默认构造函数，使用单花色模式

    /**
     * 深拷贝当前游戏状态
     * 通过序列化/反序列化实现深拷贝
     * 捕获并处理可能的异常
     */
    public SpiderGame copy() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (SpiderGame) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从另一个对象恢复状态
     * 将当前对象状态替换为另一个对象的状态
     */
    public void restoreFrom(SpiderGame other) {
        this.columns = other.columns;
        this.stock = other.stock;
        this.gameMode = other.gameMode;
        this.completedSequences = other.completedSequences;
        this.foundations = other.foundations;
        this.score = other.score;
        this.moves = other.moves;
        this.deals = other.deals;
        this.elapsedSeconds = other.elapsedSeconds;
    }

        /**
     * 根据游戏模式获取对应的花色列表
     *
     * @param mode 游戏模式，决定返回哪些花色
     * @return 包含相应花色的列表
     * @throws IllegalArgumentException 当传入未知的游戏模式时抛出异常
     */
    private List<Card.Suit> getSuitsForMode(GameMode mode) {
        switch (mode) {
            case SINGLE_SUIT:
                return Arrays.asList(Card.Suit.SPADE);
            case TWO_SUITS:
                return Arrays.asList(Card.Suit.SPADE, Card.Suit.HEART);
            case FOUR_SUITS:
                return Arrays.asList(Card.Suit.SPADE, Card.Suit.HEART, Card.Suit.DIAMOND, Card.Suit.CLUB);
            default:
                throw new IllegalArgumentException("Unknown game mode: " + mode);
        }
    }


    public void newGame() { // 清空所有现有数据，重置游戏统计
        for (List<Card> column : columns) {
            column.clear();
        }
        stock.clear();
        completedSequences = 0;
        foundations.clear();
        score = 0;
        moves = 0;
        deals = 0;

        List<Card> deck = new ArrayList<>();
        List<Card.Suit> suits = getSuitsForMode(gameMode);
        int copiesPerSuit = 8 / suits.size();
        for (Card.Suit suit : suits) {
            for (int copy = 0; copy < copiesPerSuit; copy++) {
                for (int rank = 1; rank <= 13; rank++) {
                    deck.add(new Card(suit, rank));
                }
            }
        }
        Collections.shuffle(deck); // 随机打乱牌组,洗牌算法

        for (int col = 0; col < COLUMN_COUNT; col++) {
            int cardsInColumn = col < 4 ? 6 : 5; // 前4列放6张牌，后6列放5张牌
            List<Card> column = columns.get(col);
            for (int j = 0; j < cardsInColumn; j++) {
                Card card = deck.remove(deck.size() - 1);
                column.add(card);
            }
            if (!column.isEmpty()) {
                column.get(column.size() - 1).setFaceUp(true); // 每列最后一张牌翻开
            }
        }
        stock.addAll(deck); // 剩余牌放入牌堆
    }

    public List<Card> getColumn(int index) {
        return columns.get(index);
    }

    public List<Card> getStock() {
        return stock;
    }

    public int getCompletedSequences() {
        return completedSequences;
    }

    public List<List<Card>> getFoundations() {
        return foundations;
    }

    public boolean isGameWon() {
        return completedSequences == 8;
    }  // 胜利判断

    public boolean canDealRow() {
        return stock.size() >= COLUMN_COUNT;
    } // 判断是否可以发牌（牌堆至少有10张牌

    public boolean dealRow() { // 从牌堆顶取牌，翻开并放到每列顶部
        if (!canDealRow()) {
            return false;
        }
        for (List<Card> column : columns) { // 更新统计数据
            Card card = stock.remove(stock.size() - 1);
            card.setFaceUp(true);
            column.add(card);
        }
        onDeal();
        return true;
    }

//     拖拽检查方法，检查从指定位置开始能否拖拽牌序列
//条件：起始牌及后续牌都翻开，同花色，递减顺
    public boolean canStartDrag(int columnIndex, int cardIndex) {
        List<Card> column = columns.get(columnIndex);
        if (cardIndex < 0 || cardIndex >= column.size()) {
            return false;
        }
        Card start = column.get(cardIndex);
        if (!start.isFaceUp()) {
            return false;
        }
        for (int i = cardIndex; i < column.size() - 1; i++) {
            Card current = column.get(i);
            Card next = column.get(i + 1);
            if (!next.isFaceUp()) {
                return false;
            }
            if (current.getSuit() != next.getSuit()) {
                return false;
            }
            if (current.getRank() != next.getRank() + 1) {
                return false;
            }
        }
        return true;
    }
// 放置检查方法，检查能否将牌移动到目标列
//目标列为空或顶部牌比移动牌大1
    public boolean canDrop(int fromColumn, int startIndex, int toColumn) {
        if (fromColumn == toColumn) {
            return false;
        }
        if (!canStartDrag(fromColumn, startIndex)) {
            return false;
        }
        List<Card> target = columns.get(toColumn);
        List<Card> source = columns.get(fromColumn);
        Card moving = source.get(startIndex);
        if (target.isEmpty()) {
            return true;
        }
        Card targetTop = target.get(target.size() - 1);
        return targetTop.isFaceUp()
                && targetTop.getRank() == moving.getRank() + 1;
    }
// 移动序列方法，执行牌序列移动
//检查是否形成完整序列
//更新移动计数
    public boolean moveSequence(int fromColumn, int startIndex, int toColumn) {
        if (!canDrop(fromColumn, startIndex, toColumn)) {
            return false;
        }
        List<Card> source = columns.get(fromColumn);
        List<Card> target = columns.get(toColumn);
        List<Card> moving = new ArrayList<>(source.subList(startIndex, source.size()));
        source.subList(startIndex, source.size()).clear();
        target.addAll(moving);
        if (!source.isEmpty()) {
            Card last = source.get(source.size() - 1);
            last.setFaceUp(true);
        }
        checkCompleteSequence(toColumn);
        onMove();
        return true;
    }
// 检查完整序列方法，检查列末尾是否形成完整序列（13张同花色递减牌）
//如是，移除序列并增加分数
    private void checkCompleteSequence(int columnIndex) {
        List<Card> column = columns.get(columnIndex);
        if (column.size() < COMPLETE_SEQUENCE_LENGTH) {
            return;
        }
        int start = column.size() - COMPLETE_SEQUENCE_LENGTH;
        Card first = column.get(start);
        Card.Suit suit = first.getSuit();
        int expectedRank = 13;
        for (int i = start; i < column.size(); i++) {
            Card card = column.get(i);
            if (!card.isFaceUp()) {
                return;
            }
            if (card.getSuit() != suit) {
                return;
            }
            if (card.getRank() != expectedRank) {
                return;
            }
            expectedRank--;
        }
        List<Card> sequence = new ArrayList<>(column.subList(start, column.size()));
        column.subList(start, column.size()).clear();
        completedSequences++;
        foundations.add(sequence);
        if (!column.isEmpty()) {
            column.get(column.size() - 1).setFaceUp(true);
        }
        score += 100;
    }

    public List<Card> getMovableSequence(int columnIndex, int startIndex) {
        List<Card> column = columns.get(columnIndex);
        if (!canStartDrag(columnIndex, startIndex)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(column.subList(startIndex, column.size()));
    }

    public void onMove() {
        moves++;
    }

    public void onDeal() {
        score = Math.max(0, score - 5);
        deals++;
    }

    public int getScore() {
        return score;
    }

    public int getMoves() {
        return moves;
    }

    public int getDeals() {
        return deals;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }
}
