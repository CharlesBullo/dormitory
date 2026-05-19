package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PaymentController {

    @FXML
    private TableView<Tenant> paymentTable;

    @FXML
    private TableColumn<Tenant, String> nameColumn;

    @FXML
    private TableColumn<Tenant, String> roomColumn;

    @FXML
    private TableColumn<Tenant, Boolean> paidColumn;

    @FXML
    private TableColumn<Tenant, Double> rentColumn;

    @FXML
    private TableColumn<Tenant, Double> debtColumn;

    @FXML
    private TableColumn<Tenant, Integer> missedColumn;

    @FXML
    private TableColumn<Tenant, Boolean> evictedColumn;

    private DataManager dataManager;
    private ObservableList<Tenant> tenantList;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();
        tenantList = dataManager.getTenantList();

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("tenantName"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        paidColumn.setCellValueFactory(new PropertyValueFactory<>("isPaid"));
        rentColumn.setCellValueFactory(new PropertyValueFactory<>("monthlyRent"));
        debtColumn.setCellValueFactory(new PropertyValueFactory<>("totalDebt"));
        missedColumn.setCellValueFactory(new PropertyValueFactory<>("missedMonths"));
        evictedColumn.setCellValueFactory(new PropertyValueFactory<>("isEvicted"));

        // Format evicted column
        evictedColumn.setCellFactory(column -> new TableCell<Tenant, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item) {
                        setText("EVICTED");
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setText("ACTIVE");
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }
            }
        });

        paymentTable.setItems(tenantList);
    }

    @FXML
    private void handleMarkPaid() {
        Tenant selected = paymentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a tenant.");
            return;
        }

        if (selected.getIsEvicted()) {
            showAlert("Cannot mark paid - tenant is evicted!");
            return;
        }

        selected.setPaid(true);
        selected.setTotalDebt(0);
        selected.setMissedMonths(0);
        selected.setPaidStreak(selected.getPaidStreak() + 1);
        selected.setEvicted(false);

        // Update in database
        updateTenantInDatabase(selected);

        paymentTable.refresh();
        showAlert("Payment marked as paid for " + selected.getTenantName());
    }

    @FXML
    private void handleMissPayment() {
        Tenant selected = paymentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a tenant.");
            return;
        }

        if (selected.getIsEvicted()) {
            showAlert("Tenant is already evicted!");
            return;
        }

        selected.setPaid(false);
        int newMissed = selected.getMissedMonths() + 1;
        selected.setMissedMonths(newMissed);
        double updatedDebt = selected.getTotalDebt() + selected.getMonthlyRent();
        selected.setTotalDebt(updatedDebt);
        selected.setPaidStreak(0);

        // EVICT AFTER 3 MISSED MONTHS
        if (newMissed >= 3) {
            selected.setEvicted(true);
            showAlert("⚠️ " + selected.getTenantName() + " has been EVICTED due to 3 missed payments!");
        }

        // Update in database
        updateTenantInDatabase(selected);

        paymentTable.refresh();
        showAlert("Missed payment recorded for " + selected.getTenantName());
    }

    private void updateTenantInDatabase(Tenant tenant) {
        String sql = "UPDATE tenants SET is_paid = ?, total_debt = ?, missed_months = ?, " +
                "paid_streak = ?, is_evicted = ? WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, tenant.getIsPaid());
            pstmt.setDouble(2, tenant.getTotalDebt());
            pstmt.setInt(3, tenant.getMissedMonths());
            pstmt.setInt(4, tenant.getPaidStreak());
            pstmt.setBoolean(5, tenant.getIsEvicted());
            pstmt.setString(6, tenant.getTenantId());
            pstmt.executeUpdate();

            // If evicted, also deactivate user account
            if (tenant.getIsEvicted()) {
                deactivateUserAccount(tenant.getTenantId());
            }

        } catch (SQLException e) {
            System.err.println("Error updating tenant: " + e.getMessage());
        }
    }

    private void deactivateUserAccount(String tenantId) {
        String sql = "UPDATE users SET is_active = FALSE WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            pstmt.executeUpdate();
            System.out.println("User account deactivated for tenant: " + tenantId);

        } catch (SQLException e) {
            System.err.println("Error deactivating user: " + e.getMessage());
        }
    }


    @FXML
    private void handleRefresh() {
        dataManager.refreshData();
        tenantList = dataManager.getTenantList();
        paymentTable.setItems(tenantList);
        paymentTable.refresh();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}