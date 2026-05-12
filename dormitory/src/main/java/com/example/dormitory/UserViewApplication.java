package com.example.dormitory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UserViewApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(UserViewApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 320, 240);
            stage.setTitle("Welcome to Dormitory");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        launch();
    }
}