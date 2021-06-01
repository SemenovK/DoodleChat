package com.ksemenov.doodlechat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/DoodleChatWindow.fxml"));
        primaryStage.setTitle("Doodle Chat");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Platform.exit();
            }
        });

    }



    public static void main(String[] args) {
        launch(args);
    }
}
