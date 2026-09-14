package com.example.demo.view;

import com.example.demo.card.Card;
import com.example.demo.card.CardFaceView;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 卡牌奖励弹层：屏幕中央出现三张随机牌，点一张加入牌组，
 * 或点击“跳过（不加牌）”放弃奖励。选择后通过回调通知外部。
 *
 * <p>原本是战斗胜利专用，现在两处共用：
 * <ul>
 *   <li>战斗胜利（{@code BattleView.showCardReward}）—— 默认标题「战斗胜利！」</li>
 *   <li>起点遗物「混沌」的三连卡牌奖励（{@code RoomView.giveChaosCardRewards}）
 *       —— 用 {@link #setHeader} 换成自己的标题，不写「战斗胜利」</li>
 * </ul>
 * 抽什么牌由调用方决定（两边都走 {@code CardRewardPool.draw}）。</p>
 */
public class RewardOverlay extends StackPane {
    private final HBox rewardBox = new HBox(16);
    private boolean rewardChosen = false;
    private final Runnable onSkip;

    /** 标题 / 副标题（混沌的卡牌奖励会用 setHeader 改写） */
    private final Label titleLabel = new Label("战斗胜利！");
    private final Label subLabel = new Label("选择一张牌加入牌组");

    /**
     * @param onSkip 跳过按钮回调（点击跳过时触发，外部负责隐藏弹层与结束战斗）
     */
    public RewardOverlay(Runnable onSkip) {
        this.onSkip = onSkip;
        buildRewardOverlay();
        setVisible(false);
    }

    /**
     * 改写标题与副标题。默认是战斗胜利的那一套文案，其它场景
     * （如遗物「混沌」的卡牌奖励）复用时用它换掉，免得张冠李戴。
     */
    public void setHeader(String title, String sub) {
        titleLabel.setText(title);
        subLabel.setText(sub);
    }

    private void buildRewardOverlay() {
        Pane dim = new Pane();
        dim.setStyle("-fx-background-color: rgba(2, 6, 23, 0.78);");

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #111827; -fx-background-radius: 18; -fx-padding: 22 30 18 30;");

        titleLabel.setTextFill(Color.rgb(251, 191, 36));
        titleLabel.setFont(Font.font(26));
        titleLabel.setStyle("-fx-font-weight: bold;");

        subLabel.setTextFill(Color.rgb(203, 213, 225));
        subLabel.setFont(Font.font(15));

        rewardBox.setAlignment(Pos.CENTER);

        Button skip = new Button("跳过（不加牌）");
        skip.setFont(Font.font(14));
        skip.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand;");
        skip.setOnAction(e -> {
            if (rewardChosen) return;
            rewardChosen = true;
            onSkip.run();
        });

        box.getChildren().addAll(titleLabel, subLabel, rewardBox, skip);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.CENTER);
        getChildren().addAll(dim, box);
    }

    /**
     * 展示三张奖励牌。
     *
     * @param offers   待选奖励牌（通常 3 张）
     * @param onChoose 选中某张牌时的回调，参数为被选中的牌和被点的按钮（按钮可作飞行起点）；
     *                 外部负责隐藏弹层与结束战斗
     */
    public void show(List<Card> offers, BiConsumer<Card, Node> onChoose) {
        rewardChosen = false;
        rewardBox.getChildren().clear();
        for (Card c : offers) {
            Button b = new Button();
            b.setGraphic(CardFaceView.build(c)); // 多层贴图卡面
            b.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
            b.setOnAction(e -> {
                if (rewardChosen) return;
                rewardChosen = true;
                onChoose.accept(c, b);
            });
            rewardBox.getChildren().add(b);
        }
        setVisible(true);
    }

    /** 隐藏弹层。 */
    public void hide() {
        setVisible(false);
    }

    /** 是否已经做出选择（选牌或跳过均算已选）。 */
    public boolean isChosen() {
        return rewardChosen;
    }
}
