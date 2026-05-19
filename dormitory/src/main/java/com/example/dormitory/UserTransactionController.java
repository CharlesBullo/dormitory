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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserTransactionController {

    @FXML
    private TableView<PaymentRecord> transactionTable;

    @FXML
    private TableColumn<PaymentRecord, String> dateColumn;

    @FXML
    private TableColumn<PaymentRecord, String> referenceColumn;

    @FXML
    private TableColumn<PaymentRecord, Double> amountColumn;

    @FXML
    private TableColumn<PaymentRecord, Integer> monthsPaidColumn;

    @FXML
    private TableColumn<PaymentRecord, String> methodColumn;

    @FXML
    private TableColumn<PaymentRecord, String> statusColumn;

    @FXML
    private Label totalPaymentsLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Label lastPaymentLabel;

    @FXML
    private TextField searchField;

    private String tenantId;
    private ObservableList<PaymentRecord> allPayments;
    private ObservableList<PaymentRecord> filteredPayments;

    /**
     * Set the tenant ID and load transaction history
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        loadTransactionHistory();
    }

    @FXML
    public void initialize() {
        // Initialize columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        referenceColumn.setCellValueFactory(new PropertyValueFactory<>("referenceNumber"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        monthsPaidColumn.setCellValueFactory(new PropertyValueFactory<>("monthsPaid"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format amount column
        amountColumn.setCellFactory(column -> new TableCell<PaymentRecord, Double>() {
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
        statusColumn.setCellFactory(column -> new TableCell<PaymentRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("COMPLETED")) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (item.equals("PENDING")) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else if (item.equals("FAILED")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        allPayments = FXCollections.observableArrayList();
        filteredPayments = FXCollections.observableArrayList();
        transactionTable.setItems(filteredPayments);

        // Add search listener
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchPayments(newText);
        });
    }

    /**
     * Load transaction history from database
     */
    private void loadTransactionHistory() {
        if (tenantId == null) {
            System.err.println("Tenant ID is null, cannot load transaction history");
            return;
        }

        allPayments.clear();
        String sql = "SELECT * FROM payments WHERE tenant_id = ? ORDER BY payment_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenantId);
            ResultSet rs = pstmt.executeQuery();

            double totalAmount = 0;
            LocalDate lastPaymentDate = null;

            while (rs.next()) {
                LocalDate paymentDate = rs.getDate("payment_date").toLocalDate();
                double amount = rs.getDouble("amount");
                int monthsPaid = rs.getInt("months_paid");
                String paymentMethod = rs.getString("payment_method");
                String referenceNumber = rs.getString("reference_number");

                PaymentRecord record = new PaymentRecord(
                        paymentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        referenceNumber != null ? referenceNumber : "N/A",
                        amount,
                        monthsPaid,
                        paymentMethod != null ? paymentMethod : "CASH",
                        "COMPLETED"
                );
                allPayments.add(record);

                totalAmount += amount;
                lastPaymentDate = paymentDate;
            }

            filteredPayments.setAll(allPayments);

            // Update statistics
            updateStatistics(totalAmount, lastPaymentDate);

            System.out.println("Loaded " + allPayments.size() + " payment records for tenant: " + tenantId);

        } catch (SQLException e) {
            System.err.println("Error loading transaction history: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load transaction history: " + e.getMessage());
        }
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics(double totalAmount, LocalDate lastPaymentDate) {
        if (totalPaymentsLabel != null) {
            totalPaymentsLabel.setText(String.valueOf(allPayments.size()));
        }

        if (totalAmountLabel != null) {
            totalAmountLabel.setText("₱" + String.format("%,.2f", totalAmount));
        }

        if (lastPaymentLabel != null) {
            if (lastPaymentDate != null) {
                lastPaymentLabel.setText(lastPaymentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            } else {
                lastPaymentLabel.setText("No payments yet");
            }
        }
    }

    /**
     * Search payments by reference number or date
     */
    private void searchPayments(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredPayments.setAll(allPayments);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            ObservableList<PaymentRecord> searchResults = FXCollections.observableArrayList();

            for (PaymentRecord record : allPayments) {
                if (record.getReferenceNumber().toLowerCase().contains(lowerCaseSearch) ||
                        record.getPaymentDate().toLowerCase().contains(lowerCaseSearch)) {
                    searchResults.add(record);
                }
            }
            filteredPayments.setAll(searchResults);
        }
        transactionTable.setItems(filteredPayments);
    }

    @FXML
    private void handleRefresh() {
        loadTransactionHistory();
        searchField.clear();
        showAlert("Refreshed", "Transaction history has been refreshed.");
    }

    @FXML
    private void handleExport() {
        // Create export content
        StringBuilder exportData = new StringBuilder();
        exportData.append("Payment History Report\n");
        exportData.append("=====================\n\n");
        exportData.append("Date,Reference Number,Amount,Months Paid,Payment Method,Status\n");

        for (PaymentRecord record : filteredPayments) {
            exportData.append(String.format("%s,%s,%.2f,%d,%s,%s\n",
                    record.getPaymentDate(),
                    record.getReferenceNumber(),
                    record.getAmount(),
                    record.getMonthsPaid(),
                    record.getPaymentMethod(),
                    record.getStatus()
            ));
        }

        // Show export dialog with data
        TextArea textArea = new TextArea(exportData.toString());
        textArea.setEditable(false);
        textArea.setPrefHeight(400);
        textArea.setPrefWidth(600);

        Alert exportAlert = new Alert(Alert.AlertType.INFORMATION);
        exportAlert.setTitle("Export Payment History");
        exportAlert.setHeaderText("Copy the data below to export:");
        exportAlert.getDialogPane().setContent(textArea);
        exportAlert.setResizable(true);

        // Add copy button
        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(exportData.toString());
            clipboard.setContent(content);
            showAlert("Copied", "Data copied to clipboard!");
        });

        exportAlert.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        exportAlert.getDialogPane().setContent(copyButton);
        exportAlert.showAndWait();
    }

    @FXML
    private void handlePrint() {
        // Simple print functionality
        StringBuilder printData = new StringBuilder();
        printData.append("╔════════════════════════════════════════════════════════════════╗\n");
        printData.append("║                    PAYMENT HISTORY REPORT                      ║\n");
        printData.append("╠════════════════════════════════════════════════════════════════╣\n");
        printData.append(String.format("║ Tenant ID: %-50s║\n", tenantId));
        printData.append(String.format("║ Total Payments: %-45s║\n", allPayments.size()));
        printData.append("╠════════════════════════════════════════════════════════════════╣\n");
        printData.append("║ Date       │ Reference    │ Amount    │ Months │ Method      ║\n");
        printData.append("╠════════════════════════════════════════════════════════════════╣\n");

        for (PaymentRecord record : filteredPayments) {
            printData.append(String.format("║ %-10s │ %-12s │ ₱%-8.2f │ %-6d │ %-11s ║\n",
                    record.getPaymentDate(),
                    record.getReferenceNumber().length() > 12 ? record.getReferenceNumber().substring(0, 10) : record.getReferenceNumber(),
                    record.getAmount(),
                    record.getMonthsPaid(),
                    record.getPaymentMethod()
            ));
        }

        printData.append("╚════════════════════════════════════════════════════════════════╝\n");

        // Show print preview
        TextArea printArea = new TextArea(printData.toString());
        printArea.setEditable(false);
        printArea.setFont(javafx.scene.text.Font.font("Monospaced", 12));

        Alert printAlert = new Alert(Alert.AlertType.INFORMATION);
        printAlert.setTitle("Print Preview");
        printAlert.setHeaderText("Payment History Report");
        printAlert.getDialogPane().setContent(printArea);
        printAlert.setResizable(true);
        printAlert.getDialogPane().setPrefSize(700, 500);
        printAlert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for payment records
     */
    public static class PaymentRecord {
        private final String paymentDate;
        private final String referenceNumber;
        private final double amount;
        private final int monthsPaid;
        private final String paymentMethod;
        private final String status;

        public PaymentRecord(String paymentDate, String referenceNumber, double amount,
                             int monthsPaid, String paymentMethod, String status) {
            this.paymentDate = paymentDate;
            this.referenceNumber = referenceNumber;
            this.amount = amount;
            this.monthsPaid = monthsPaid;
            this.paymentMethod = paymentMethod;
            this.status = status;
        }

        public String getPaymentDate() { return paymentDate; }
        public String getReferenceNumber() { return referenceNumber; }
        public double getAmount() { return amount; }
        public int getMonthsPaid() { return monthsPaid; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
    }
}