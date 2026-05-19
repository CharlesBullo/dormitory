package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

        // Format tenant name column
        tenantNameColumn.setCellFactory(column -> new TableCell<Room, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item == null || item.isEmpty() || item.equals("null")) {
                        setText("— Vacant —");
                        setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #2c3e50;");
                    }
                }
            }
        });

        loadRoomData();
        updateStatistics();

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchRooms(newText);
        });
    }

    private void loadRoomData() {
        // Refresh from database
        dataManager.refreshData();
        roomList = dataManager.getRoomList();
        filteredList.setAll(roomList);
        roomTable.setItems(filteredList);
        roomTable.refresh();
    }

    private void searchRooms(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setAll(roomList);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            ObservableList<Room> searchResults = FXCollections.observableArrayList();

            for (Room room : roomList) {
                if (room.getRoomNumber().toLowerCase().contains(lowerCaseSearch) ||
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
    private void handleRefresh() {
        loadRoomData();
        updateStatistics();
        showAlert("Refreshed", "Room list has been refreshed.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleMarkRentDue() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room.", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getStatus().equals("OCCUPIED")) {
            double rentDue = selected.getMonthlyRent();
            selected.setRentDue(rentDue);

            // Update in database
            String sql = "UPDATE rooms SET rent_due = ? WHERE room_id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setDouble(1, rentDue);
                pstmt.setString(2, selected.getRoomId());
                pstmt.executeUpdate();

                // Update local list
                int index = roomList.indexOf(selected);
                if (index >= 0) {
                    roomList.set(index, selected);
                }

                roomTable.refresh();
                showAlert("Success", "Rent marked as due for Room " + selected.getRoomNumber(), Alert.AlertType.INFORMATION);

            } catch (SQLException e) {
                System.err.println("Error marking rent due: " + e.getMessage());
                showAlert("Error", "Failed to mark rent due.", Alert.AlertType.ERROR);
            }
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

        // Update in database
        String sql = "UPDATE rooms SET rent_due = 0 WHERE room_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, selected.getRoomId());
            pstmt.executeUpdate();

            // Update local list
            int index = roomList.indexOf(selected);
            if (index >= 0) {
                roomList.set(index, selected);
            }

            roomTable.refresh();
            showAlert("Success", "Rent due cleared for Room " + selected.getRoomNumber(), Alert.AlertType.INFORMATION);

        } catch (SQLException e) {
            System.err.println("Error clearing rent due: " + e.getMessage());
            showAlert("Error", "Failed to clear rent due.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleShowAllRooms() {
        loadRoomData();
        searchField.clear();
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

    private void updateStatistics() {
        ObservableList<Room> currentList = roomTable.getItems();
        long total = currentList.size();
        long occupied = currentList.stream().filter(r -> "OCCUPIED".equals(r.getStatus())).count();
        long vacant = currentList.stream().filter(r -> "VACANT".equals(r.getStatus())).count();
        long maintenance = currentList.stream().filter(r -> "MAINTENANCE".equals(r.getStatus())).count();

        totalRoomsLabel.setText(String.valueOf(total));
        occupiedLabel.setText(String.valueOf(occupied));
        vacantLabel.setText(String.valueOf(vacant));
        maintenanceLabel.setText(String.valueOf(maintenance));
    }

    @FXML
    private void handleAddRoom() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Add a new room");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

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
        grid.add(new Label("Monthly Rent:"), 0, 2);
        grid.add(rentField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    String roomNumber = roomNumberField.getText().trim();
                    String status = statusBox.getValue();
                    double rent = Double.parseDouble(rentField.getText());
                    String roomId = "R" + String.format("%03d", roomList.size() + 1);
                    return new Room(roomId, roomNumber, status, rent);
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid rent amount.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(room -> {
            dataManager.addRoom(room);
            handleRefresh();
        });
    }

    @FXML
    private void handleEditRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a room to edit.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit Room: " + selected.getRoomNumber());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField roomNumberField = new TextField(selected.getRoomNumber());
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("VACANT", "OCCUPIED", "MAINTENANCE");
        statusBox.setValue(selected.getStatus());
        TextField rentField = new TextField(String.valueOf(selected.getMonthlyRent()));

        if (selected.getStatus().equals("OCCUPIED")) {
            statusBox.setDisable(true);
        }

        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumberField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Monthly Rent:"), 0, 2);
        grid.add(rentField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                selected.setRoomNumber(roomNumberField.getText().trim());
                selected.setStatus(statusBox.getValue());
                selected.setMonthlyRent(Double.parseDouble(rentField.getText()));
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(room -> {
            dataManager.updateRoom(room);
            handleRefresh();
            showAlert("Success", "Room updated successfully.", Alert.AlertType.INFORMATION);
        });
    }

    @FXML
    private void handleDeleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a room to delete.", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getStatus().equals("OCCUPIED")) {
            showAlert("Cannot Delete", "Cannot delete occupied room. Remove tenant first.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setContentText("Delete room " + selected.getRoomNumber() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            dataManager.removeRoom(selected);
            handleRefresh();
            showAlert("Success", "Room deleted.", Alert.AlertType.INFORMATION);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}