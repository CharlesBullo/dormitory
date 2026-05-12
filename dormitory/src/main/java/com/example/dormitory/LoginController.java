package com.example.dormitory;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("user-main-view.fxml")
            );

            Scene scene = new Scene(loader.load());

            // Get current window
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setMinWidth(1000);
            stage.setMinHeight(520);
            stage.setScene(scene);
            stage.setTitle("Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}