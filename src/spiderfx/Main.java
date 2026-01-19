package spiderfx;
// 导入 JavaFX 的基础类：Application（应用程序基类）、Scene（场景类）、Stage（舞台/窗口类）
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import spiderfx.controller.GameController;
import spiderfx.model.SpiderGame;
import spiderfx.view.GameView;
// 导入用于文件操作的 NIO 包中的工具类
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends Application {
    @Override
    // 重写 Application 类的 start 方法，这是 JavaFX 应用程序的入口点
    public void start(Stage primaryStage) {
        SpiderGame game = new SpiderGame(); //游戏模型实例
        GameView view = new GameView(game); // 游戏视图实例，并传入游戏模型
        GameController controller = new GameController(game, view); // 游戏控制器实例，连接模型和视图
        view.bindController(controller); // 将控制器绑定到视图，建立双向连接

        double baseWidth = 1200;
        double baseHeight = 850;

        Scene scene = new Scene(view, baseWidth, baseHeight); // 使用视图作为根节点创建场景对象
        controller.installSceneHandlers(scene); // 在场景上安装事件处理器

        var css = Main.class.getResource("spider.css"); // 获取 CSS 资源
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            Path cssPath = Paths.get("src", "spiderfx", "spider.css");
            if (Files.exists(cssPath)) {
                scene.getStylesheets().add(cssPath.toUri().toString());
            }
        }

        primaryStage.setTitle("蜘蛛纸牌"); // 主窗口标题
        primaryStage.setResizable(false); // 窗口不可缩放
        primaryStage.setScene(scene); // 场景设置到主窗口
        primaryStage.show(); // 显示主窗口
    }

    public static void main(String[] args) {
        launch(args);
    }
}
