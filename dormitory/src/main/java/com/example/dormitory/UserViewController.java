package com.example.dormitory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserViewController {

    @FXML
    private StackPane contentPane;

    private String tenantId;
    private String tenantName;
    private String roomNumber;

    @FXML
    public void initialize() {
        System.out.println("UserViewController initialized");
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        loadTenantInfo();
        handleUserDashboard();
    }

    private void loadTenantInfo() {
        String sql = "SELECT tenant_name, room_number FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                tenantName = rs.getString("tenant_name");
                roomNumber = rs.getString("room_number");
                System.out.println("Loaded tenant: " + tenantName + " - Room: " + roomNumber);
            }
        } catch (SQLException e) {
            System.err.println("Error loading tenant info: " + e.getMessage());
        }
    }

    @FXML
    private void handleUserDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/user-dashboard-view.fxml"));
            Node view = loader.load();

            UserDashboardController controller = loader.getController();
            controller.setTenantInfo(tenantId, tenantName, roomNumber);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        try {
            // Load the payment view - NOT transaction view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/user-payment-view.fxml"));
            Node view = loader.load();

            // Get the correct controller - UserPaymentController
            UserPaymentController controller = loader.getController();
            if (controller != null) {
                controller.setTenantInfo(tenantId, tenantName, roomNumber);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load payment page: " + e.getMessage());
        }
    }

    @FXML
    private void handleTransactionHistory() {
        try {
            // Load the transaction view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/user-transaction-view.fxml"));
            Node view = loader.load();

            // Get the correct controller - UserTransactionController
            UserTransactionController controller = loader.getController();
            if (controller != null) {
                controller.setTenantId(tenantId);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load transaction history: " + e.getMessage());
        }
    }

    @FXML
    private void handleRentNotification() {
        try {
            String sql = "SELECT due_date, total_debt, monthly_rent, missed_months FROM tenants WHERE tenant_id = ?";

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, tenantId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                    double totalDebt = rs.getDouble("total_debt");
                    double monthlyRent = rs.getDouble("monthly_rent");
                    int missedMonths = rs.getInt("missed_months");

                    StringBuilder message = new StringBuilder();
                    message.append("═══════════════════════════════════════\n");
                    message.append("         RENT NOTIFICATION\n");
                    message.append("═══════════════════════════════════════\n\n");
                    message.append("Tenant: ").append(tenantName).append("\n");
                    message.append("Room: ").append(roomNumber).append("\n");
                    message.append("Monthly Rent: ₱").append(String.format("%,.2f", monthlyRent)).append("\n");
                    message.append("Due Date: ").append(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))).append("\n\n");

                    if (totalDebt > 0) {
                        message.append("⚠️ OVERDUE NOTICE ⚠️\n");
                        message.append("Total Debt: ₱").append(String.format("%,.2f", totalDebt)).append("\n");
                        message.append("Missed Months: ").append(missedMonths).append("\n\n");
                        message.append("YOUR ACCOUNT IS OVERDUE!\n");
                        message.append("Please pay immediately to avoid eviction.\n");
                    } else if (dueDate.isBefore(LocalDate.now())) {
                        message.append("⚠️ RENT PAST DUE ⚠️\n");
                        long daysOverdue = dueDate.until(LocalDate.now()).getDays();
                        message.append("Days Overdue: ").append(daysOverdue).append(" days\n\n");
                        message.append("Please make your payment as soon as possible.\n");
                    } else {
                        long daysUntilDue = LocalDate.now().until(dueDate).getDays();
                        message.append("📌 UPCOMING RENT DUE 📌\n");
                        message.append("Days until due: ").append(daysUntilDue).append(" days\n\n");

                        if (daysUntilDue <= 7) {
                            message.append("⚠️ Reminder: Your rent is due soon!\n");
                        } else {
                            message.append("You have plenty of time to prepare your payment.\n");
                        }
                    }

                    message.append("\n═══════════════════════════════════════");

                    Alert notification = new Alert(Alert.AlertType.INFORMATION);
                    notification.setTitle("Rent Notification");
                    notification.setHeaderText("Payment Status");
                    notification.setContentText(message.toString());
                    notification.showAndWait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load notification: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        showChangePasswordDialog();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/dormitory/login-view.fxml"));
                javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
                javafx.stage.Stage stage = (javafx.stage.Stage) contentPane.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Dormitory Management System - Login");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to logout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleMouseEnter(javafx.scene.input.MouseEvent event) {
        Button button = (Button) event.getSource();
        if (!button.getText().contains("Logout")) {
            button.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 15;");
        }
    }

    @FXML
    private void handleMouseExit(javafx.scene.input.MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button.getText().contains("Logout")) {
            button.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 15;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 15;");
        }
    }

    private void showChangePasswordDialog() {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change Your Password");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setStyle("-fx-padding: 20;");

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current Password");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");

        content.getChildren().addAll(
                new javafx.scene.control.Label("Current Password:"),
                currentPasswordField,
                new javafx.scene.control.Label("New Password:"),
                newPasswordField,
                new javafx.scene.control.Label("Confirm Password:"),
                confirmPasswordField
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == javafx.scene.control.ButtonType.OK) {
                String currentPassword = currentPasswordField.getText();
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();

                if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                    showAlert("Error", "Please fill in all fields.");
                    return null;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showAlert("Error", "New passwords do not match.");
                    return null;
                }

                if (newPassword.length() < 6) {
                    showAlert("Error", "Password must be at least 6 characters.");
                    return null;
                }

                if (verifyCurrentPassword(currentPassword)) {
                    return newPassword;
                } else {
                    showAlert("Error", "Current password is incorrect.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(this::updatePassword);
    }

    private boolean verifyCurrentPassword(String currentPassword) {
        String sql = "SELECT password FROM users WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return BCrypt.checkpw(currentPassword, hashedPassword);
            }
        } catch (SQLException e) {
            System.err.println("Error verifying password: " + e.getMessage());
        }

        return false;
    }

    private void updatePassword(String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        String sql = "UPDATE users SET password = ? WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, tenantId);
            pstmt.executeUpdate();

            showAlert("Success", "Password changed successfully! Please login again.");
            handleLogout();
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            showAlert("Error", "Failed to change password.");
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