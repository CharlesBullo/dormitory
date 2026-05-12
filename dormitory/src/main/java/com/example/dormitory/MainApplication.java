package com.example.dormitory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 320, 240);
            stage.setTitle("Dormitory / Boarding House Management System");
            stage.setScene(scene);
            stage.setMinWidth(760);
            stage.setMinHeight(520);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        launch();
    }
}