package com.example.dormitory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserDashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label roomNumberLabel;
    @FXML
    private Label rentAmountLabel;
    @FXML
    private Label dueDateLabel;
    @FXML
    private Label paymentStatusLabel;
    @FXML
    private Label debtAmountLabel;
    @FXML
    private Label missedMonthsLabel;
    @FXML
    private Label paidStreakLabel;
    @FXML
    private Label nextDueDateLabel;

    private String tenantId;
    private String tenantName;
    private String roomNumber;

    @FXML
    public void initialize() {
        System.out.println("UserDashboardController initialized");
    }

    public void setTenantInfo(String tenantId, String tenantName, String roomNumber) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.roomNumber = roomNumber;
        loadDashboardData();
    }

    private void loadDashboardData() {
        System.out.println("Loading dashboard data for tenant: " + tenantId);

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + tenantName + "!");
        }

        if (roomNumberLabel != null) {
            roomNumberLabel.setText(roomNumber);
        }

        String sql = "SELECT monthly_rent, due_date, is_paid, total_debt, missed_months, paid_streak FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double monthlyRent = rs.getDouble("monthly_rent");
                LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                boolean isPaid = rs.getBoolean("is_paid");
                double totalDebt = rs.getDouble("total_debt");
                int missedMonths = rs.getInt("missed_months");
                int paidStreak = rs.getInt("paid_streak");

                if (rentAmountLabel != null) {
                    rentAmountLabel.setText("₱" + String.format("%,.2f", monthlyRent));
                }

                if (dueDateLabel != null) {
                    dueDateLabel.setText(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                    if (dueDate.isBefore(LocalDate.now()) && !isPaid) {
                        dueDateLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }

                if (paymentStatusLabel != null) {
                    if (isPaid) {
                        paymentStatusLabel.setText("PAID");
                        paymentStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        paymentStatusLabel.setText("UNPAID");
                        paymentStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }

                if (debtAmountLabel != null) {
                    debtAmountLabel.setText("₱" + String.format("%,.2f", totalDebt));
                    if (totalDebt > 0) {
                        debtAmountLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }

                if (missedMonthsLabel != null) {
                    missedMonthsLabel.setText(String.valueOf(missedMonths));
                    if (missedMonths > 0) {
                        missedMonthsLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                }

                if (paidStreakLabel != null) {
                    paidStreakLabel.setText(paidStreak + " months");
                }

                if (nextDueDateLabel != null) {
                    nextDueDateLabel.setText(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadDashboardData();
    }
}