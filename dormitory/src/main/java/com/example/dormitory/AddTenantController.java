package com.example.dormitory;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddTenantController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField contactField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private Label roomRentLabel;

    @FXML
    private Label roomStatusLabel;

    @FXML
    private DatePicker moveInDatePicker;

    @FXML
    private Label dueDateLabel;

    @FXML
    private Label availableRoomsLabel;

    @FXML
    private StackPane contentPane;

    private DataManager dataManager;
    private ObservableList<Room> vacantRooms;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

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
                updateRoomDetails(newRoom);
            }
        });

        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        vacantRooms = dataManager.getVacantRooms();
        roomComboBox.getItems().clear();

        for (Room room : vacantRooms) {
            roomComboBox.getItems().add(room.getRoomNumber() + " - ₱" + String.format("%.2f", room.getMonthlyRent()));
        }

        availableRoomsLabel.setText("Available Rooms: " + vacantRooms.size());

        if (vacantRooms.isEmpty()) {
            roomComboBox.setDisable(true);
            roomRentLabel.setText("No available rooms!");
            roomStatusLabel.setText("Please add rooms first");
        } else {
            roomComboBox.setDisable(false);
        }
    }

    private void updateRoomDetails(String roomSelection) {
        if (roomSelection != null && !roomSelection.isEmpty()) {
            String roomNumber = roomSelection.split(" - ")[0];
            Room selectedRoom = dataManager.findRoomByNumber(roomNumber);

            if (selectedRoom != null) {
                roomRentLabel.setText("₱" + String.format("%.2f", selectedRoom.getMonthlyRent()));
                roomStatusLabel.setText(selectedRoom.getStatus());

                // Set style based on status
                if (selectedRoom.getStatus().equals("VACANT")) {
                    roomStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else if (selectedRoom.getStatus().equals("OCCUPIED")) {
                    roomStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    roomStatusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                }
            }
        }
    }

    private void updateDueDate() {
        LocalDate moveInDate = moveInDatePicker.getValue();
        if (moveInDate != null) {
            LocalDate dueDate = moveInDate.plusMonths(1);
            dueDateLabel.setText(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    @FXML
    private void handleSave() {
        // Validate inputs
        if (nameField.getText().isEmpty()) {
            showAlert("Error", "Please enter tenant name.");
            return;
        }

        if (contactField.getText().isEmpty()) {
            showAlert("Error", "Please enter contact number.");
            return;
        }

        if (emailField.getText().isEmpty()) {
            showAlert("Error", "Please enter email address.");
            return;
        }

        if (roomComboBox.getValue() == null) {
            showAlert("Error", "Please select a room.");
            return;
        }

        if (moveInDatePicker.getValue() == null) {
            showAlert("Error", "Please select move-in date.");
            return;
        }

        // Extract room details
        String roomSelection = roomComboBox.getValue();
        String roomNumber = roomSelection.split(" - ")[0];
        Room selectedRoom = dataManager.findRoomByNumber(roomNumber);

        if (selectedRoom == null) {
            showAlert("Error", "Selected room not found.");
            return;
        }

        // Check if room is still vacant
        if (!selectedRoom.getStatus().equals("VACANT")) {
            showAlert("Error", "This room is no longer available. Please select another room.");
            loadAvailableRooms();
            return;
        }

        // Create tenant ID
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

        // Add address if provided
        if (addressField.getText() != null && !addressField.getText().isEmpty()) {
            newTenant.setAddress(addressField.getText());
        }

        // Update room to OCCUPIED and assign tenant
        selectedRoom.setStatus("OCCUPIED");
        selectedRoom.setTenantName(newTenant.getTenantName());
        selectedRoom.setTenantId(newTenant.getTenantId());

        // Add tenant to data manager
        dataManager.addTenant(newTenant);

        // Show success message
        showAlert("Success", "Tenant registered successfully!\n\n" +
                "Tenant: " + newTenant.getTenantName() + "\n" +
                "Room: " + newTenant.getRoomNumber() + "\n" +
                "Monthly Rent: ₱" + String.format("%.2f", newTenant.getMonthlyRent()) + "\n" +
                "Move-in Date: " + newTenant.getMoveInDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + "\n" +
                "First Due Date: " + newTenant.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Clear form
        clearForm();

        // Refresh available rooms
        loadAvailableRooms();

        // Refresh rooms view if it's currently displayed
        refreshRoomsView();
    }

    private void refreshRoomsView() {
        // This will update the rooms table if it's currently showing
        try {
            Node roomsView = contentPane.lookup(".rooms-view");
            if (roomsView != null) {
                // Force refresh of rooms controller
                FXMLLoader loader = new FXMLLoader(getClass().getResource("rooms-view.fxml"));
                Node newRoomsView = loader.load();
                RoomController roomController = loader.getController();
                roomController.refreshRoomList();
            }
        } catch (Exception e) {
            // Rooms view not currently active, ignore
        }
    }

    private void clearForm() {
        nameField.clear();
        contactField.clear();
        emailField.clear();
        addressField.clear();
        roomComboBox.setValue(null);
        roomRentLabel.setText("--");
        roomStatusLabel.setText("--");
        moveInDatePicker.setValue(LocalDate.now());
        updateDueDate();
    }

    @FXML
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to clear the form? All entered data will be lost.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            clearForm();
        }
    }

    @FXML
    private void handleRefreshRooms() {
        loadAvailableRooms();
        showAlert("Info", "Room list refreshed!");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}