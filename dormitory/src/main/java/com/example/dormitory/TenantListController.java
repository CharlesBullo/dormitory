package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class TenantListController {

    @FXML
    private TableView<Tenant> tenantTable;

    @FXML
    private TableColumn<Tenant, String> tenantIdColumn;

    @FXML
    private TableColumn<Tenant, String> nameColumn;

    @FXML
    private TableColumn<Tenant, String> contactColumn;

    @FXML
    private TableColumn<Tenant, String> emailColumn;

    @FXML
    private TableColumn<Tenant, String> roomColumn;

    @FXML
    private TableColumn<Tenant, Double> rentColumn;

    @FXML
    private TableColumn<Tenant, String> moveInDateColumn;

    @FXML
    private TableColumn<Tenant, String> dueDateColumn;

    @FXML
    private TableColumn<Tenant, String> statusColumn;

    @FXML
    private TableColumn<Tenant, String> usernameColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Label totalTenantsLabel;

    @FXML
    private Label activeTenantsLabel;

    @FXML
    private Label evictedTenantsLabel;

    @FXML
    private Label totalRentLabel;

    @FXML
    private StackPane contentPane;

    private DataManager dataManager;
    private ObservableList<Tenant> tenantList;
    private ObservableList<Tenant> filteredList;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();
        tenantList = dataManager.getTenantList();
        filteredList = FXCollections.observableArrayList();

        // Initialize table columns
        tenantIdColumn.setCellValueFactory(new PropertyValueFactory<>("tenantId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("tenantName"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        rentColumn.setCellValueFactory(new PropertyValueFactory<>("monthlyRent"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        // Status column
        statusColumn.setCellValueFactory(cellData -> {
            Tenant tenant = cellData.getValue();
            String status = tenant.getIsEvicted() ? "EVICTED" : "ACTIVE";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        // Format move-in date column
        moveInDateColumn.setCellValueFactory(cellData -> {
            Tenant tenant = cellData.getValue();
            String date = tenant.getMoveInDate() != null ?
                    tenant.getMoveInDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "";
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        // Format due date column
        dueDateColumn.setCellValueFactory(cellData -> {
            Tenant tenant = cellData.getValue();
            String date = tenant.getDueDate() != null ?
                    tenant.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "";
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        // Format rent column
        rentColumn.setCellFactory(column -> new TableCell<Tenant, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("₱" + String.format("%,.2f", item));
                }
            }
        });

        // Format status column with colors
        statusColumn.setCellFactory(column -> new TableCell<Tenant, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("EVICTED")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }
            }
        });

        loadTenantData();
        updateStatistics();

        // Add search listener
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchTenants(newText);
        });
    }

    private void loadTenantData() {
        filteredList.setAll(tenantList);
        tenantTable.setItems(filteredList);
    }

    private void searchTenants(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setAll(tenantList);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            ObservableList<Tenant> searchResults = FXCollections.observableArrayList();

            for (Tenant tenant : tenantList) {
                if (tenant.getTenantName().toLowerCase().contains(lowerCaseSearch) ||
                        tenant.getTenantId().toLowerCase().contains(lowerCaseSearch) ||
                        tenant.getRoomNumber().toLowerCase().contains(lowerCaseSearch) ||
                        tenant.getContactNumber().contains(searchText) ||
                        tenant.getUsername().toLowerCase().contains(lowerCaseSearch)) {
                    searchResults.add(tenant);
                }
            }
            filteredList.setAll(searchResults);
        }
        tenantTable.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleRemoveTenant() {
        Tenant selected = tenantTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a tenant to remove.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Tenant: " + selected.getTenantName());
        confirm.setContentText("Are you sure you want to permanently remove this tenant?\n\n" +
                "Tenant ID: " + selected.getTenantId() + "\n" +
                "Name: " + selected.getTenantName() + "\n" +
                "Room: " + selected.getRoomNumber() + "\n" +
                "Monthly Rent: ₱" + String.format("%,.2f", selected.getMonthlyRent()) + "\n\n" +
                "⚠ This action will:\n" +
                "• Permanently delete the tenant from the database\n" +
                "• Delete the user account\n" +
                "• Delete all payment records\n" +
                "• Vacate the room\n" +
                "• This action CANNOT be undone!");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            removeTenant(selected);
        }
    }

    private void removeTenant(Tenant tenant) {
        try {
            String tenantName = tenant.getTenantName();
            String roomNumber = tenant.getRoomNumber();

            dataManager.removeTenant(tenant);
            refreshTenantList();
            updateStatistics();

            showAlert("Success",
                    "Tenant " + tenantName + " has been removed successfully!\n\n" +
                            "Room " + roomNumber + " is now vacant.",
                    Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("Error", "Failed to remove tenant: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEvictTenant() {
        Tenant selected = tenantTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a tenant to evict.", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getIsEvicted()) {
            showAlert("Already Evicted", "This tenant is already evicted.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Eviction");
        confirm.setHeaderText("Evict Tenant: " + selected.getTenantName());
        confirm.setContentText("Are you sure you want to evict this tenant?\n\n" +
                "Tenant: " + selected.getTenantName() + "\n" +
                "Room: " + selected.getRoomNumber() + "\n" +
                "Total Debt: ₱" + String.format("%,.2f", selected.getTotalDebt()) + "\n\n" +
                "⚠️ This will:\n" +
                "• Mark the tenant as EVICTED\n" +
                "• Deactivate their user account\n" +
                "• Prevent them from logging in\n" +
                "• Vacate the room\n\n" +
                "This action can be undone by marking payment.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            evictTenant(selected);
        }
    }

    private void evictTenant(Tenant tenant) {
        // Mark as evicted
        tenant.setEvicted(true);

        // Deactivate user account
        deactivateUserAccount(tenant.getTenantId());

        // Vacate the room
        Room room = dataManager.findRoomByNumber(tenant.getRoomNumber());
        if (room != null) {
            room.setStatus("VACANT");
            room.setTenantName("");
            room.setTenantId("");
            room.setRentDue(0);
            dataManager.updateRoom(room);
        }

        // Update tenant in database
        String sql = "UPDATE tenants SET is_evicted = TRUE, is_paid = FALSE WHERE tenant_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tenant.getTenantId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating tenant eviction: " + e.getMessage());
        }

        refreshTenantList();
        updateStatistics();

        showAlert("Success", "Tenant " + tenant.getTenantName() + " has been evicted.\n" +
                "Their user account has been deactivated.\n" +
                "Room " + tenant.getRoomNumber() + " is now vacant.", Alert.AlertType.INFORMATION);
    }

    /**
     * Deactivate user account for evicted tenant
     */
    private void deactivateUserAccount(String tenantId) {
        String sql = "UPDATE users SET is_active = FALSE WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✓ User account deactivated for tenant: " + tenantId);
            } else {
                System.out.println("No user account found for tenant: " + tenantId);
            }

        } catch (SQLException e) {
            System.err.println("Error deactivating user account: " + e.getMessage());
        }
    }

    /**
     * Reactivate user account (when tenant pays debt)
     */
    private void reactivateUserAccount(String tenantId) {
        String sql = "UPDATE users SET is_active = TRUE WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            pstmt.executeUpdate();
            System.out.println("✓ User account reactivated for tenant: " + tenantId);

        } catch (SQLException e) {
            System.err.println("Error reactivating user account: " + e.getMessage());
        }
    }

    public void refreshTenantList() {
        dataManager.refreshData();
        tenantList = dataManager.getTenantList();
        filteredList.setAll(tenantList);
        tenantTable.setItems(filteredList);
        updateStatistics();
        System.out.println("Tenant list refreshed, total: " + tenantList.size());
    }

    @FXML
    private void handleViewTenantDetails() {
        Tenant selected = tenantTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a tenant to view details.", Alert.AlertType.WARNING);
            return;
        }

        showTenantDetails(selected);
    }

    private void showTenantDetails(Tenant tenant) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Tenant Details");
        details.setHeaderText("Tenant Information: " + tenant.getTenantName());

        String content = String.format(
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "         TENANT INFORMATION\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Tenant ID: %s\n" +
                        "Username: %s\n" +
                        "Full Name: %s\n" +
                        "Contact #: %s\n" +
                        "Email: %s\n" +
                        "Address: %s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "         ROOM INFORMATION\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Room Number: %s\n" +
                        "Monthly Rent: ₱%,.2f\n" +
                        "Move-in Date: %s\n" +
                        "Due Date: %s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "         PAYMENT STATUS\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Payment Status: %s\n" +
                        "Total Debt: ₱%,.2f\n" +
                        "Missed Months: %d\n" +
                        "Paid Streak: %d months\n" +
                        "Eviction Status: %s\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                tenant.getTenantId(),
                tenant.getUsername(),
                tenant.getTenantName(),
                tenant.getContactNumber(),
                tenant.getEmail(),
                tenant.getAddress() == null || tenant.getAddress().isEmpty() ? "Not provided" : tenant.getAddress(),
                tenant.getRoomNumber(),
                tenant.getMonthlyRent(),
                tenant.getMoveInDate() != null ? tenant.getMoveInDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A",
                tenant.getDueDate() != null ? tenant.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A",
                tenant.getIsPaid() ? "✓ PAID" : "✗ UNPAID",
                tenant.getTotalDebt(),
                tenant.getMissedMonths(),
                tenant.getPaidStreak(),
                tenant.getIsEvicted() ? "EVICTED" : "ACTIVE"
        );

        details.setContentText(content);
        details.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        refreshTenantList();
        showAlert("Refreshed", "Tenant list has been refreshed from database.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleShowActive() {
        ObservableList<Tenant> activeTenants = FXCollections.observableArrayList();
        for (Tenant tenant : tenantList) {
            if (!tenant.getIsEvicted()) {
                activeTenants.add(tenant);
            }
        }
        filteredList.setAll(activeTenants);
        tenantTable.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleShowEvicted() {
        ObservableList<Tenant> evictedTenants = FXCollections.observableArrayList();
        for (Tenant tenant : tenantList) {
            if (tenant.getIsEvicted()) {
                evictedTenants.add(tenant);
            }
        }
        filteredList.setAll(evictedTenants);
        tenantTable.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleShowAll() {
        filteredList.setAll(tenantList);
        tenantTable.setItems(filteredList);
        updateStatistics();
        searchField.clear();
    }

    private void updateStatistics() {
        int total = filteredList.size();
        long active = filteredList.stream().filter(t -> !t.getIsEvicted()).count();
        long evicted = filteredList.stream().filter(t -> t.getIsEvicted()).count();
        double totalMonthlyRent = filteredList.stream().filter(t -> !t.getIsEvicted()).mapToDouble(Tenant::getMonthlyRent).sum();

        totalTenantsLabel.setText(String.valueOf(total));
        activeTenantsLabel.setText(String.valueOf(active));
        evictedTenantsLabel.setText(String.valueOf(evicted));
        totalRentLabel.setText("₱" + String.format("%,.2f", totalMonthlyRent));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}