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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add-tenant-view.fxml"));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTenantList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("tenant-list-view.fxml"));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRooms() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("rooms-view.fxml"));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("payment-view.fxml"));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}