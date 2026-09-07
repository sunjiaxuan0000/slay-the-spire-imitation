module com.example.thegame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.almasb.fxgl.all;

    opens com.example.thegame to javafx.fxml;
    exports com.example.thegame;
    exports com.example.thegame.controller;
    opens com.example.thegame.controller to javafx.fxml;
    exports com.example.thegame.day1;
    opens com.example.thegame.day1 to javafx.fxml;
}