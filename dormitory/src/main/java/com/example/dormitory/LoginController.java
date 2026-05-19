package com.example.dormitory;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private CheckBox showPasswordCheck;

    private String currentUserId;
    private String currentUserRole;
    private String currentTenantId;
    private boolean isEvicted;

    @FXML
    public void initialize() {
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        showPasswordCheck.setOnAction(event -> {
            if (showPasswordCheck.isSelected()) {
                visiblePasswordField.setText(passwordField.getText());
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
            } else {
                passwordField.setText(visiblePasswordField.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
            }
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!showPasswordCheck.isSelected()) {
                visiblePasswordField.setText(newVal);
            }
        });

        visiblePasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setText(newVal);
            }
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = showPasswordCheck.isSelected() ? visiblePasswordField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter both username and password.");
            return;
        }

        // Authenticate user
        if (authenticateUser(username, password)) {
            // Check if tenant is evicted
            if (currentUserRole.equals("TENANT") && isEvicted) {
                showEvictedAlert();
                return;
            }

            // Check if this is a tenant (first login) and needs password change
            if (currentUserRole.equals("TENANT") && password.equals("123456")) {
                showChangePasswordDialog(event);
            } else {
                proceedToDashboard(event);
            }
        } else {
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    private boolean authenticateUser(String username, String password) {
        String sql = "SELECT u.*, t.is_evicted as tenant_evicted FROM users u " +
                "LEFT JOIN tenants t ON u.tenant_id = t.tenant_id " +
                "WHERE u.username = ? AND u.is_active = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");

                // Verify password with BCrypt
                if (BCrypt.checkpw(password, hashedPassword)) {
                    currentUserId = rs.getString("user_id");
                    currentUserRole = rs.getString("role");
                    currentTenantId = rs.getString("tenant_id");

                    // Check eviction status for tenants
                    if (currentUserRole.equals("TENANT")) {
                        isEvicted = rs.getBoolean("tenant_evicted");
                    }

                    // Update last login
                    updateLastLogin(currentUserId);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private void showEvictedAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access Denied");
        alert.setHeaderText("⚠️ ACCOUNT EVICTED ⚠️");

        // Get eviction details
        String sql = "SELECT total_debt, missed_months, due_date FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, currentTenantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double totalDebt = rs.getDouble("total_debt");
                int missedMonths = rs.getInt("missed_months");

                String content = String.format(
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "         EVICTION NOTICE\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                                "Your account has been EVICTED due to:\n\n" +
                                "• Missed Payments: %d month(s)\n" +
                                "• Total Debt: ₱%,.2f\n\n" +
                                "You no longer have access to the system.\n\n" +
                                "Please contact the administrator for more information.\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                        missedMonths, totalDebt
                );
                alert.setContentText(content);
            } else {
                alert.setContentText("Your account has been evicted. Please contact the administrator.");
            }

        } catch (SQLException e) {
            alert.setContentText("Your account has been evicted. Please contact the administrator.");
        }

        alert.showAndWait();
    }

    private void updateLastLogin(String userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    private void showChangePasswordDialog(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("First Time Login - Change Your Password");

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");

        Label infoLabel = new Label("This is your first login with temporary password '123456'.");
        infoLabel.setStyle("-fx-text-fill: #e74c3c;");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");

        TextField visibleNewPasswordField = new TextField();
        visibleNewPasswordField.setPromptText("New Password");
        visibleNewPasswordField.setVisible(false);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        TextField visibleConfirmField = new TextField();
        visibleConfirmField.setPromptText("Confirm Password");
        visibleConfirmField.setVisible(false);

        CheckBox showPasswordCheck = new CheckBox("Show Password");
        showPasswordCheck.setOnAction(e -> {
            boolean selected = showPasswordCheck.isSelected();
            if (selected) {
                visibleNewPasswordField.setText(newPasswordField.getText());
                visibleConfirmField.setText(confirmPasswordField.getText());
                visibleNewPasswordField.setVisible(true);
                visibleConfirmField.setVisible(true);
                newPasswordField.setVisible(false);
                confirmPasswordField.setVisible(false);
            } else {
                newPasswordField.setText(visibleNewPasswordField.getText());
                confirmPasswordField.setText(visibleConfirmField.getText());
                newPasswordField.setVisible(true);
                confirmPasswordField.setVisible(true);
                visibleNewPasswordField.setVisible(false);
                visibleConfirmField.setVisible(false);
            }
        });

        content.getChildren().addAll(
                infoLabel,
                new Label("New Password:"),
                newPasswordField,
                visibleNewPasswordField,
                new Label("Confirm Password:"),
                confirmPasswordField,
                visibleConfirmField,
                showPasswordCheck
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String newPassword = showPasswordCheck.isSelected() ? visibleNewPasswordField.getText() : newPasswordField.getText();
                String confirmPassword = showPasswordCheck.isSelected() ? visibleConfirmField.getText() : confirmPasswordField.getText();

                if (newPassword.isEmpty()) {
                    showAlert("Error", "Password cannot be empty.");
                    return null;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showAlert("Error", "Passwords do not match.");
                    return null;
                }

                if (newPassword.length() < 6) {
                    showAlert("Error", "Password must be at least 6 characters.");
                    return null;
                }

                return newPassword;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPassword -> {
            changePassword(newPassword);
            proceedToDashboard(event);
        });
    }

    private void changePassword(String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, currentUserId);
            pstmt.executeUpdate();

            showAlert("Success", "Password changed successfully!");

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            showAlert("Error", "Failed to change password.");
        }
    }

    private void proceedToDashboard(ActionEvent event) {
        try {
            String fxmlFile;
            String title;

            if (currentUserRole.equals("ADMIN") || currentUserRole.equals("STAFF")) {
                fxmlFile = "/com/example/dormitory/main-view.fxml";
                title = "Dormitory Management System - Admin Dashboard";
            } else {
                fxmlFile = "/com/example/dormitory/user-main-view.fxml";
                title = "Dormitory Management System - Tenant Portal";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);

            if (currentUserRole.equals("TENANT") && currentTenantId != null) {
                UserViewController controller = loader.getController();
                controller.setTenantId(currentTenantId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load dashboard: " + e.getMessage());
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