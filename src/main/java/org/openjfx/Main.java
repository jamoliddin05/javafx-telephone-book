package org.openjfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load the initial scene from LoginScene.fxml
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("LoginScene.fxml")));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            // Optionally, you can show an alert to the user here
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
