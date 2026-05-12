package com.example.dormitory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;

public class UserPaymentController {

    @FXML
    private ComboBox<String> paymentOptionBox;

    @FXML
    private Label nameLabel;

    @FXML
    private Label dueDateLabel;

    @FXML
    private StackPane contentPane;

    // REMOVED sample tenant data - will be null or empty
    private String tenantName = ""; // Empty instead of "John Doe"
    private LocalDate dueDate = LocalDate.now(); // Current date instead of hardcoded

    @FXML
    public void initialize() {

        if (tenantName != null && !tenantName.isEmpty()) {
            nameLabel.setText("Tenant: " + tenantName);
        } else {
            nameLabel.setText("Tenant: Not assigned");
        }

        dueDateLabel.setText("Due Date: " + dueDate);

        paymentOptionBox.getItems().addAll(
                "1 Month",
                "3 Months",
                "6 Months",
                "1 Year"
        );
    }

    @FXML
    private void handlePayment() {

        String selected = paymentOptionBox.getValue();

        if (selected == null) {
            showAlert("Please select a payment option.");
            return;
        }

        int monthsToAdd = 0;
        switch (selected) {
            case "1 Month":
                monthsToAdd = 1;
                break;
            case "3 Month":
                monthsToAdd = 3;
                break;
            case "6 Month":
                monthsToAdd = 6;
                break;
            case "1 Year":
                monthsToAdd = 12;
                break;
            default:
                monthsToAdd = 0;
        };

        dueDate = dueDate.plusMonths(monthsToAdd);

        // Update label
        dueDateLabel.setText("Due Date: " + dueDate);

        showAlert("Payment Successful!\nNew Due Date: " + dueDate);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("user-main-view.fxml")
            );

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}