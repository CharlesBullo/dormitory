package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegisterTenantController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField contactField;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private Label roomRentLabel;

    @FXML
    private DatePicker moveInDatePicker;

    @FXML
    private Label dueDateLabel;

    @FXML
    private StackPane contentPane;

    private ObservableList<Room> availableRooms = FXCollections.observableArrayList();
    private ObservableList<Tenant> tenantList;
    private ObservableList<Room> allRooms;

    @FXML
    public void initialize() {
        // Set up date picker
        moveInDatePicker.setValue(LocalDate.now());
        updateDueDate();

        // Add listener to date picker
        moveInDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            updateDueDate();
        });

        // Add listener to room selection
        roomComboBox.valueProperty().addListener((obs, oldRoom, newRoom) -> {
            if (newRoom != null) {
                updateRoomRent(newRoom);
            }
        });

        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        // This would normally come from a database
        // For demo, we'll create sample vacant rooms
        availableRooms.clear();
        availableRooms.addAll(
                new Room("R001", "101", "VACANT", 5000),
                new Room("R002", "102", "VACANT", 4500),
                new Room("R003", "103", "VACANT", 5500),
                new Room("R005", "105", "VACANT", 4800),
                new Room("R007", "202", "VACANT", 5200),
                new Room("R009", "204", "VACANT", 5000)
        );

        roomComboBox.getItems().clear();
        for (Room room : availableRooms) {
            roomComboBox.getItems().add(room.getRoomNumber() + " - ₱" + String.format("%.2f", room.getMonthlyRent()));
        }
    }

    private void updateRoomRent(String roomSelection) {
        if (roomSelection != null && !roomSelection.isEmpty()) {
            String roomNumber = roomSelection.split(" - ")[0];
            for (Room room : availableRooms) {
                if (room.getRoomNumber().equals(roomNumber)) {
                    roomRentLabel.setText("Monthly Rent: ₱" + String.format("%.2f", room.getMonthlyRent()));
                    break;
                }
            }
        }
    }

    private void updateDueDate() {
        LocalDate moveInDate = moveInDatePicker.getValue();
        if (moveInDate != null) {
            LocalDate dueDate = moveInDate.plusMonths(1);
            dueDateLabel.setText("First Due Date: " + dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    @FXML
    private void handleRegister() {
        // Validate inputs
        if (nameField.getText().isEmpty()) {
            showAlert("Please enter tenant name.");
            return;
        }

        if (contactField.getText().isEmpty()) {
            showAlert("Please enter contact number.");
            return;
        }

        if (emailField.getText().isEmpty()) {
            showAlert("Please enter email address.");
            return;
        }

        if (roomComboBox.getValue() == null) {
            showAlert("Please select a room.");
            return;
        }

        if (moveInDatePicker.getValue() == null) {
            showAlert("Please select move-in date.");
            return;
        }

        // Extract room details
        String roomSelection = roomComboBox.getValue();
        String roomNumber = roomSelection.split(" - ")[0];
        Room selectedRoom = null;

        for (Room room : availableRooms) {
            if (room.getRoomNumber().equals(roomNumber)) {
                selectedRoom = room;
                break;
            }
        }

        if (selectedRoom == null) {
            showAlert("Selected room not found.");
            return;
        }

        // Create tenant ID (simple generation)
        String tenantId = "T" + System.currentTimeMillis();

        // Create new tenant
        Tenant newTenant = new Tenant(
                tenantId,
                nameField.getText(),
                contactField.getText(),
                emailField.getText(),
                selectedRoom.getRoomNumber(),
                selectedRoom.getRoomId(),
                selectedRoom.getMonthlyRent(),
                moveInDatePicker.getValue()
        );

        // Update room status to OCCUPIED
        selectedRoom.setStatus("OCCUPIED");
        selectedRoom.setTenantName(newTenant.getTenantName());

        // Here you would save to database or your data structure
        // For now, we'll just show success message

        showAlert("Tenant registered successfully!\n\n" +
                "Tenant: " + newTenant.getTenantName() + "\n" +
                "Room: " + newTenant.getRoomNumber() + "\n" +
                "Monthly Rent: ₱" + String.format("%.2f", newTenant.getMonthlyRent()) + "\n" +
                "Move-in Date: " + newTenant.getMoveInDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + "\n" +
                "First Due Date: " + newTenant.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Clear form
        clearForm();

        // Refresh the available rooms list
        loadAvailableRooms();
    }

    private void clearForm() {
        nameField.clear();
        contactField.clear();
        emailField.clear();
        roomComboBox.setValue(null);
        roomRentLabel.setText("Monthly Rent: --");
        moveInDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleCancel() {
        // Go back to dashboard or clear form
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to cancel? All entered data will be lost.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            clearForm();
            // Optionally navigate back to dashboard
            try {
                Node view = FXMLLoader.load(
                        getClass().getResource("dashboard-view.fxml")
                );
                contentPane.getChildren().setAll(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewRooms() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("rooms-view.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}