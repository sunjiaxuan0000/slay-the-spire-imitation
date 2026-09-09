package com.example.thegame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.IOException;

public class  Main extends Application {
    public static void main(String[]args){
        launch(args);
    }
    @Override
    public void start(Stage stage) throws IOException {
        StackPane root=new StackPane();
        Scene scene = new Scene(root, 1080, 641);
        stage.setWidth(1080);
        stage.setHeight(641);
        stage.setResizable(false);
        stage.setTitle("Slay the Spire");
        stage.setScene(scene);
        Image icon=new Image(getClass().getResourceAsStream("/assets/thegame/images/icon.png"));
        stage.getIcons().add(icon);
        stage.show();
    }

    @Override
    public void stop() throws Exception{
        super.stop();
    }
}
