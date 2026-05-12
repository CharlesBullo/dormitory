package com.example.dormitory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane contentPane;

    @FXML
    private void handleDashboard(){
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("dashboard-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterTenant() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("add-tenant-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTenantList() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("tenant-list-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRooms() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("rooms-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePayment() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("payment-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}