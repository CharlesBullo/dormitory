package com.example.dormitory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class UserViewController {
    @FXML
    private StackPane contentPane;

    @FXML
    public void initialize() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("user-dashboard-view.fxml")
            );

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUserDashboard(){
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("user-dashboard-view.fxml")
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
                    getClass().getResource("user-payment-view.fxml")
            );

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleTransactionHistory() {

        try {

            Node view = FXMLLoader.load(
                    getClass().getResource("user-transaction-view.fxml")
            );

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}