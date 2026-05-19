package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserPaymentController {

    @FXML
    private ComboBox<String> paymentOptionBox;

    @FXML
    private Label nameLabel;

    @FXML
    private Label dueDateLabel;

    @FXML
    private Label roomNumberLabel;

    @FXML
    private Label rentAmountLabel;

    @FXML
    private Label totalPaymentLabel;

    @FXML
    private StackPane contentPane;

    private String tenantId;
    private String tenantName;
    private String roomNumber;
    private double monthlyRent;
    private LocalDate dueDate;
    private double totalDebt;

    @FXML
    public void initialize() {
        System.out.println("UserPaymentController initialized");

        // Initialize payment options
        paymentOptionBox.setItems(FXCollections.observableArrayList(
                "1 Month",
                "3 Months",
                "6 Months",
                "1 Year"
        ));

        paymentOptionBox.setValue("1 Month");

        // Add listener to update total payment
        paymentOptionBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalPayment();
        });
    }

    public void setTenantInfo(String tenantId, String tenantName, String roomNumber) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.roomNumber = roomNumber;
        loadTenantData();
    }

    private void loadTenantData() {
        if (tenantId == null) {
            System.err.println("Tenant ID is null");
            return;
        }

        nameLabel.setText(tenantName);
        roomNumberLabel.setText(roomNumber);

        String sql = "SELECT monthly_rent, due_date, total_debt FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                monthlyRent = rs.getDouble("monthly_rent");
                dueDate = rs.getDate("due_date").toLocalDate();
                totalDebt = rs.getDouble("total_debt");

                rentAmountLabel.setText("₱" + String.format("%,.2f", monthlyRent));
                dueDateLabel.setText(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

                updateTotalPayment();
            }
        } catch (SQLException e) {
            System.err.println("Error loading tenant data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTotalPayment() {
        String selected = paymentOptionBox.getValue();
        if (selected == null || monthlyRent == 0) return;

        int monthsToPay = 0;
        switch (selected) {
            case "1 Month": monthsToPay = 1; break;
            case "3 Months": monthsToPay = 3; break;
            case "6 Months": monthsToPay = 6; break;
            case "1 Year": monthsToPay = 12; break;
            default: monthsToPay = 1;
        }

        double totalAmount = monthlyRent * monthsToPay;
        totalPaymentLabel.setText("₱" + String.format("%,.2f", totalAmount));
    }

    @FXML
    private void handlePayment() {
        String selected = paymentOptionBox.getValue();

        if (selected == null) {
            showAlert("Error", "Please select a payment option.");
            return;
        }

        int monthsToPay = 0;
        switch (selected) {
            case "1 Month": monthsToPay = 1; break;
            case "3 Months": monthsToPay = 3; break;
            case "6 Months": monthsToPay = 6; break;
            case "1 Year": monthsToPay = 12; break;
        }

        double totalAmount = monthlyRent * monthsToPay;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText("Confirm Your Payment");
        confirm.setContentText("Please confirm your payment details:\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Room: " + roomNumber + "\n" +
                "Payment Option: " + selected + "\n" +
                "Amount: ₱" + String.format("%,.2f", totalAmount) + "\n\n" +
                "Click OK to proceed.");

        if (confirm.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            processPayment(monthsToPay, totalAmount);
        }
    }

    private void processPayment(int monthsToPay, double amount) {
        String referenceNumber = "PAY-" + System.currentTimeMillis();
        String sql = "INSERT INTO payments (tenant_id, amount, payment_date, months_paid, payment_method, reference_number) " +
                "VALUES (?, ?, CURDATE(), ?, 'CASH', ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, monthsToPay);
            pstmt.setString(4, referenceNumber);
            pstmt.executeUpdate();

            // Update tenant
            updateTenantAfterPayment(monthsToPay);

            showPaymentSuccess(monthsToPay, amount, referenceNumber);

        } catch (SQLException e) {
            System.err.println("Error processing payment: " + e.getMessage());
            showAlert("Payment Failed", "Failed to process payment. Please try again.");
        }
    }

    private void updateTenantAfterPayment(int monthsToPay) {
        String sql = "UPDATE tenants SET " +
                "is_paid = TRUE, total_debt = 0, missed_months = 0, " +
                "paid_streak = paid_streak + ?, is_evicted = FALSE, " +
                "due_date = DATE_ADD(due_date, INTERVAL ? MONTH) " +
                "WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, monthsToPay);
            pstmt.setInt(2, monthsToPay);
            pstmt.setString(3, tenantId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating tenant: " + e.getMessage());
        }
    }

    private void showPaymentSuccess(int monthsToPay, double amount, String referenceNumber) {
        LocalDate newDueDate = dueDate.plusMonths(monthsToPay);

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Payment Successful");
        success.setHeaderText("✓ Payment Completed Successfully!");
        success.setContentText(
                "Reference Number: " + referenceNumber + "\n" +
                        "Amount Paid: ₱" + String.format("%,.2f", amount) + "\n" +
                        "New Due Date: " + newDueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + "\n\n" +
                        "Thank you for your payment!"
        );
        success.showAndWait();

        // Reload data
        loadTenantData();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/user-main-view.fxml"));
            Node view = loader.load();

            UserViewController controller = loader.getController();
            if (controller != null) {
                controller.setTenantId(tenantId);
            }

            if (contentPane != null) {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to go back: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewPaymentHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/user-transaction-view.fxml"));
            Node view = loader.load();

            UserTransactionController controller = loader.getController();
            if (controller != null) {
                controller.setTenantId(tenantId);
            }

            if (contentPane != null) {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load payment history: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}