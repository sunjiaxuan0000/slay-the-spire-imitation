module com.example.thegame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.almasb.fxgl.all;

    opens com.example.thegame to javafx.fxml;
    exports com.example.thegame;
}