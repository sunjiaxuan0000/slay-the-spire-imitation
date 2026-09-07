import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class main extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, JavaFX!");
        Button button = new Button("点我");
        button.setOnAction(e -> label.setText("你点击了按钮！"));
        VBox root = new VBox(10, label, button);   // 垂直布局，间距 10px
        Scene scene = new Scene(root, 400, 300);   // 创建场景，宽 400 高 300
        stage.setTitle("JavaFX 入门");              // 设置窗口标题
        stage.setScene(scene);                      // 装载场景
        stage.show();                               // 显示窗口
    }

    public static void main(String[] args) {
        launch(args);
    }
}
