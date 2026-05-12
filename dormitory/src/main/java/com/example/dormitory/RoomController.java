package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class RoomController {

    @FXML
    private TableView<Room> roomTable;

    @FXML
    private TableColumn<Room, String> roomIdColumn;

    @FXML
    private TableColumn<Room, String> roomNumberColumn;

    @FXML
    private TableColumn<Room, String> statusColumn;

    @FXML
    private TableColumn<Room, String> tenantNameColumn;

    @FXML
    private TableColumn<Room, Double> rentColumn;

    @FXML
    private TableColumn<Room, Double> rentDueColumn;

    @FXML
    private Label totalRoomsLabel;

    @FXML
    private Label occupiedLabel;

    @FXML
    private Label vacantLabel;

    @FXML
    private Label maintenanceLabel;

    @FXML
    private TextField searchField;

    private DataManager dataManager;
    private ObservableList<Room> roomList;
    private ObservableList<Room> filteredList;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();
        roomList = dataManager.getRoomList();
        filteredList = FXCollections.observableArrayList();

        // Initialize table columns
        roomIdColumn.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        tenantNameColumn.setCellValueFactory(new PropertyValueFactory<>("tenantName"));
        rentColumn.setCellValueFactory(new PropertyValueFactory<>("monthlyRent"));
        rentDueColumn.setCellValueFactory(new PropertyValueFactory<>("rentDue"));

        // Format rent columns
        rentColumn.setCellFactory(column -> new TableCell<Room, Double>() {
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

        rentDueColumn.setCellFactory(column -> new TableCell<Room, Double>() {
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
        statusColumn.setCellFactory(column -> new TableCell<Room, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch(item) {
                        case "OCCUPIED":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "VACANT":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        case "MAINTENANCE":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        loadRoomData();
        updateStatistics();

        // Add search listener
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchRooms(newText);
        });
    }

    private void loadRoomData() {
        filteredList.setAll(roomList);
        roomTable.setItems(filteredList);
    }

    private void searchRooms(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setAll(roomList);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            ObservableList<Room> searchResults = FXCollections.observableArrayList();

            for (Room room : roomList) {
                if (room.getRoomNumber().toLowerCase().contains(lowerCaseSearch) ||
                        room.getRoomId().toLowerCase().contains(lowerCaseSearch) ||
                        room.getStatus().toLowerCase().contains(lowerCaseSearch) ||
                        (room.getTenantName() != null && room.getTenantName().toLowerCase().contains(lowerCaseSearch))) {
                    searchResults.add(room);
                }
            }
            filteredList.setAll(searchResults);
        }
        roomTable.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleEditRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room to edit.", Alert.AlertType.WARNING);
            return;
        }

        showEditRoomDialog(selected);
    }

    private void showEditRoomDialog(Room room) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit Room: " + room.getRoomNumber());

        // Create dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomNumberField = new TextField(room.getRoomNumber());
        roomNumberField.setPromptText("Room Number");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("VACANT", "OCCUPIED", "MAINTENANCE");
        statusBox.setValue(room.getStatus());

        TextField rentField = new TextField(String.valueOf(room.getMonthlyRent()));
        rentField.setPromptText("Monthly Rent");

        // Only allow editing of room number and rent if room is vacant
        if (room.getStatus().equals("OCCUPIED")) {
            statusBox.setDisable(true);
            Label infoLabel = new Label("Note: Room is occupied. Status cannot be changed while occupied.");
            infoLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 11px;");
            grid.add(infoLabel, 0, 3, 2, 1);
        }

        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumberField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Monthly Rent (₱):"), 0, 2);
        grid.add(rentField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Check if room number already exists
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String newRoomNumber = roomNumberField.getText().trim();
            if (newRoomNumber.isEmpty()) {
                showAlert("Error", "Room number cannot be empty.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }

            // Check if room number already exists (and it's not the current room)
            Room existingRoom = dataManager.findRoomByNumber(newRoomNumber);
            if (existingRoom != null && !existingRoom.getRoomId().equals(room.getRoomId())) {
                showAlert("Error", "Room number " + newRoomNumber + " already exists!", Alert.AlertType.ERROR);
                event.consume();
                return;
            }

            try {
                double newRent = Double.parseDouble(rentField.getText());
                if (newRent <= 0) {
                    showAlert("Error", "Rent must be greater than 0.", Alert.AlertType.ERROR);
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid rent amount.", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String newRoomNumber = roomNumberField.getText().trim();
                String newStatus = statusBox.getValue();
                double newRent = Double.parseDouble(rentField.getText());

                // Update room properties
                String oldRoomNumber = room.getRoomNumber();
                room.setRoomNumber(newRoomNumber);
                room.setStatus(newStatus);
                room.setMonthlyRent(newRent);

                // If room status changed to VACANT, clear tenant assignment
                if (newStatus.equals("VACANT") && !oldRoomNumber.equals(newRoomNumber)) {
                    room.setTenantName("");
                    room.setTenantId("");
                }

                return room;
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();

        if (result.isPresent()) {
            // Update room in data manager
            dataManager.updateRoom(room);

            // If room number changed, update tenant's room reference
            if (!room.getRoomNumber().equals(result.get().getRoomNumber())) {
                updateTenantRoomReferences(room);
            }

            // Refresh the table
            refreshRoomList();
            showAlert("Success", "Room updated successfully!", Alert.AlertType.INFORMATION);
        }
    }

    private void updateTenantRoomReferences(Room room) {
        // Update any tenant that has this room
        for (Tenant tenant : dataManager.getTenantList()) {
            if (tenant.getRoomId().equals(room.getRoomId())) {
                tenant.setRoomNumber(room.getRoomNumber());
                int index = dataManager.getTenantList().indexOf(tenant);
                dataManager.updateTenant(tenant, index);
            }
        }
    }

    @FXML
    private void handleAddRoom() {
        showAddRoomDialog();
    }

    private void showAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Add a new room to the system");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomNumberField = new TextField();
        roomNumberField.setPromptText("Room Number");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("VACANT", "MAINTENANCE");
        statusBox.setValue("VACANT");

        TextField rentField = new TextField();
        rentField.setPromptText("Monthly Rent");

        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumberField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Monthly Rent (₱):"), 0, 2);
        grid.add(rentField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validate input
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String roomNumber = roomNumberField.getText().trim();
            if (roomNumber.isEmpty()) {
                showAlert("Error", "Room number cannot be empty.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }

            // Check if room number already exists
            if (dataManager.findRoomByNumber(roomNumber) != null) {
                showAlert("Error", "Room number " + roomNumber + " already exists!", Alert.AlertType.ERROR);
                event.consume();
                return;
            }

            try {
                double rent = Double.parseDouble(rentField.getText());
                if (rent <= 0) {
                    showAlert("Error", "Rent must be greater than 0.", Alert.AlertType.ERROR);
                    event.consume();
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid rent amount.", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String roomNumber = roomNumberField.getText().trim();
                String status = statusBox.getValue();
                double rent = Double.parseDouble(rentField.getText());
                String roomId = "R" + String.format("%03d", roomList.size() + 1);

                return new Room(roomId, roomNumber, status, rent);
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();

        if (result.isPresent()) {
            dataManager.addRoom(result.get());
            refreshRoomList();
            showAlert("Success", "Room added successfully!", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleDeleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room to delete.", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getStatus().equals("OCCUPIED")) {
            showAlert("Cannot Delete", "Cannot delete an occupied room. Please remove the tenant first.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Room: " + selected.getRoomNumber());
        confirm.setContentText("Are you sure you want to delete this room? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            dataManager.removeRoom(selected);
            refreshRoomList();
            showAlert("Success", "Room deleted successfully!", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleMarkRentDue() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room.", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getStatus().equals("OCCUPIED")) {
            selected.setRentDue(selected.getMonthlyRent());
            dataManager.updateRoom(selected);
            refreshRoomList();
            showAlert("Success", "Rent marked as due for Room " + selected.getRoomNumber(), Alert.AlertType.INFORMATION);
        } else {
            showAlert("Cannot Mark", "Cannot mark rent due for vacant or maintenance rooms.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleClearRentDue() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room.", Alert.AlertType.WARNING);
            return;
        }

        selected.setRentDue(0);
        dataManager.updateRoom(selected);
        refreshRoomList();
        showAlert("Success", "Rent due cleared for Room " + selected.getRoomNumber(), Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleRefresh() {
        refreshRoomList();
        searchField.clear();
        showAlert("Refreshed", "Room list has been refreshed.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleVacantRooms() {
        ObservableList<Room> vacantRooms = roomList.filtered(r -> r.getStatus().equals("VACANT"));
        roomTable.setItems(vacantRooms);
        updateStatistics();
    }

    @FXML
    private void handleOccupiedRooms() {
        ObservableList<Room> occupiedRooms = roomList.filtered(r -> r.getStatus().equals("OCCUPIED"));
        roomTable.setItems(occupiedRooms);
        updateStatistics();
    }

    @FXML
    private void handleMaintenanceRooms() {
        ObservableList<Room> maintenanceRooms = roomList.filtered(r -> r.getStatus().equals("MAINTENANCE"));
        roomTable.setItems(maintenanceRooms);
        updateStatistics();
    }

    @FXML
    private void handleRentDueRooms() {
        ObservableList<Room> rentDueRooms = roomList.filtered(r -> r.getRentDue() > 0);
        roomTable.setItems(rentDueRooms);
        updateStatistics();
    }

    @FXML
    private void handleShowAllRooms() {
        roomTable.setItems(filteredList);
        updateStatistics();
    }

    public void refreshRoomList() {
        roomList = dataManager.getRoomList();
        loadRoomData();
        updateStatistics();
        roomTable.refresh();
    }

    private void updateStatistics() {
        ObservableList<Room> currentList = roomTable.getItems();
        long total = currentList.size();
        long occupied = currentList.stream().filter(r -> r.getStatus().equals("OCCUPIED")).count();
        long vacant = currentList.stream().filter(r -> r.getStatus().equals("VACANT")).count();
        long maintenance = currentList.stream().filter(r -> r.getStatus().equals("MAINTENANCE")).count();

        totalRoomsLabel.setText(String.valueOf(total));
        occupiedLabel.setText(String.valueOf(occupied));
        vacantLabel.setText(String.valueOf(vacant));
        maintenanceLabel.setText(String.valueOf(maintenance));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}